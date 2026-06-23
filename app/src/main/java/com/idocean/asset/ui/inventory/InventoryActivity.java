package com.idocean.asset.ui.inventory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.AssetRepositoryCallback;
import com.idocean.asset.data.repository.DashboardMetricsRepository;
import com.idocean.asset.data.dto.InventoryCheckinBatchRequestDto;
import com.idocean.asset.data.repository.InventoryCheckinService;
import com.idocean.asset.data.repository.InventoryCheckinUploadResult;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.AppFailureReporter;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.data.repository.SessionRepository;
import com.idocean.asset.export.InventoryCsvExportManager;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.InventorySessionItem;
import com.idocean.asset.model.SessionConfig;
import com.idocean.asset.scanner.rfid.ScannerTriggerHandler;
import com.idocean.asset.scanner.rfid.UhfScanData;
import com.idocean.asset.storage.AppPermissionManager;
import com.idocean.asset.utils.HardwareKeyUtils;
import com.idocean.asset.utils.NetworkUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Man kiem ke bam truc tiep logic UHF goc cua demo Chainway de dam bao lay duoc TID that.
 */
public class InventoryActivity extends AppCompatActivity implements ScannerTriggerHandler, InventoryScannerController.Callback {
    private static final String SCREEN = "Inventory";
    private static final String FLOW_SCREEN_OPEN = "screen_open";
    private static final String FLOW_CACHE_LOAD = "cache_load";
    private static final String FLOW_SOURCE_PREPARE = "source_prepare";
    private static final String FLOW_FILTER = "filter_search";
    private static final long FILTER_DEBOUNCE_MS = 180L;
    private static final long SCAN_REFRESH_DEBOUNCE_MS = 75L;

    private final AssetRepository assetRepository = AssetRepository.getInstance();
    private final LogRepository logRepository = LogRepository.getInstance();
    private final InventoryCsvExportManager exportManager = new InventoryCsvExportManager();
    private final InventoryCheckinService inventoryCheckinService = new InventoryCheckinService();
    private final InventoryResultAdapter adapter = new InventoryResultAdapter();
    private final InventoryController inventoryController = new InventoryController(
            DashboardMetricsRepository.getInstance(),
            LogRepository.getInstance()
    );
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private InventoryScannerController inventoryScannerController;
    private InventoryUiRenderer uiRenderer;
    private final Runnable inventorySearchRefreshRunnable = this::runInventorySearchRefresh;
    private final Runnable inventoryScanRefreshRunnable = this::runInventoryScanRefresh;

    private final ActivityResultLauncher<String[]> importCsvLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onCsvSelected);
    private final ActivityResultLauncher<String> writeStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    exportInventoryResults();
                    return;
                }
                showToast(getString(R.string.checkout_need_storage));
            });

    private SessionRepository sessionRepository;
    private TextView tvDataSourceStatus;
    private TextView tvSessionInfo;
    private TextView tvScannerStatus;
    private TextView tvSummaryTotal;
    private TextView tvSummaryDatasetTotal;
    private TextView tvSummaryChecked;
    private EditText etInventoryNote;
    private EditText etInventorySearch;
    private RadioGroup rgScannerType;
    private RadioButton rbScannerRfid;
    private RadioButton rbScannerQr;
    private SwitchCompat swContinuousScan;
    private SwitchCompat swSingleScan;
    private Button btnInventoryLoadApi;
    private Button btnInventoryImportCsv;
    private Button btnInventoryStart;
    private Button btnInventoryStop;
    private Button btnInventoryClear;
    private Button btnInventoryExport;
    private Button btnInventorySubmitCheckin;
    private Button btnInventoryToggleOptions;
    private View layoutInventoryOptions;
    private RecyclerView rvInventoryResults;
    private TextView tvInventoryEmpty;

    private PerfLogger.Trace screenOpenTrace;
    private boolean initialSourceResolved;
    private boolean sourceLoadInProgress;
    private long activeSourceLoadToken;
    private boolean exporting;
    private boolean uploadingCheckin;
    private boolean pendingInventoryScrollToTop;
    private boolean inventoryScanRefreshScheduled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        screenOpenTrace = PerfLogger.start(SCREEN, FLOW_SCREEN_OPEN, "onCreate", "activity=InventoryActivity");
        screenOpenTrace.markStart(logRepository);
        setContentView(R.layout.activity_ido_inventory);

        sessionRepository = new SessionRepository(getApplicationContext());
        inventoryController.setCurrentSession(sessionRepository.getSession());
        inventoryScannerController = new InventoryScannerController(this, logRepository);
        bindViews();
        uiRenderer = new InventoryUiRenderer(
                adapter,
                rvInventoryResults,
                tvInventoryEmpty,
                tvSummaryTotal,
                tvSummaryDatasetTotal,
                tvSummaryChecked,
                tvScannerStatus,
                btnInventoryStart
        );
        setupRecyclerView();
        setupControls();
        updateSessionInfo();
        updateScannerStatus();
        findViewById(android.R.id.content).post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (screenOpenTrace != null) {
                screenOpenTrace.finish(logRepository, "first_render", "contentReady=true");
                screenOpenTrace = null;
            }
            hydrateCachedAssetsAsync();
            scheduleScannerWarmup();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        inventoryController.setCurrentSession(sessionRepository.getSession());
        inventoryScannerController.onStart(this);
        updateSessionInfo();
        updateScannerStatus();
    }

    @Override
    public void onPause() {
        inventoryScannerController.onStop();
        updateScannerStatus();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (inventoryScannerController != null) {
            inventoryScannerController.shutdown();
        }
        mainHandler.removeCallbacks(inventorySearchRefreshRunnable);
        mainHandler.removeCallbacks(inventoryScanRefreshRunnable);
        backgroundExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (HardwareKeyUtils.isScannerTrigger(keyCode) && event.getRepeatCount() == 0) {
            onScannerTriggerDown();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (HardwareKeyUtils.isScannerTrigger(keyCode)) {
            onScannerTriggerUp();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onScannerTriggerDown() {
        startSelectedScanner();
    }

    @Override
    public void onScannerTriggerUp() {
        inventoryScannerController.handleTriggerUp(swContinuousScan.isChecked());
        updateScannerStatus();
    }

    private void bindViews() {
        tvDataSourceStatus = findViewById(R.id.tvDataSourceStatus);
        tvSessionInfo = findViewById(R.id.tvSessionInfo);
        tvScannerStatus = findViewById(R.id.tvScannerStatus);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        tvSummaryDatasetTotal = findViewById(R.id.tvSummaryDatasetTotal);
        tvSummaryChecked = findViewById(R.id.tvSummaryChecked);
        etInventoryNote = findViewById(R.id.etInventoryNote);
        etInventorySearch = findViewById(R.id.etInventorySearch);
        rgScannerType = findViewById(R.id.rgScannerType);
        rbScannerRfid = findViewById(R.id.rbScannerRfid);
        rbScannerQr = findViewById(R.id.rbScannerQr);
        swContinuousScan = findViewById(R.id.swContinuousScan);
        swSingleScan = findViewById(R.id.swSingleScan);
        btnInventoryLoadApi = findViewById(R.id.btnInventoryLoadApi);
        btnInventoryImportCsv = findViewById(R.id.btnInventoryImportCsv);
        btnInventoryStart = findViewById(R.id.btnInventoryStart);
        btnInventoryStop = findViewById(R.id.btnInventoryStop);
        btnInventoryClear = findViewById(R.id.btnInventoryClear);
        btnInventoryExport = findViewById(R.id.btnInventoryExport);
        btnInventorySubmitCheckin = findViewById(R.id.btnInventorySubmitCheckin);
        btnInventoryToggleOptions = findViewById(R.id.btnInventoryToggleOptions);
        layoutInventoryOptions = findViewById(R.id.layoutInventoryOptions);
        rvInventoryResults = findViewById(R.id.rvInventoryResults);
        tvInventoryEmpty = findViewById(R.id.tvInventoryEmpty);
    }

    private void setupRecyclerView() {
        rvInventoryResults.setLayoutManager(new LinearLayoutManager(this));
        rvInventoryResults.setAdapter(adapter);
    }

    private void setupControls() {
        swSingleScan.setChecked(true);
        swContinuousScan.setChecked(false);

        swContinuousScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                swSingleScan.setChecked(false);
            } else if (!swSingleScan.isChecked()) {
                swSingleScan.setChecked(true);
            }
        });
        swSingleScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                swContinuousScan.setChecked(false);
            } else if (!swContinuousScan.isChecked()) {
                swContinuousScan.setChecked(true);
            }
        });

        rgScannerType.setOnCheckedChangeListener((group, checkedId) -> updateScannerStatus());

        btnInventoryLoadApi.setOnClickListener(v -> loadAssetsFromApi());
        btnInventoryImportCsv.setOnClickListener(v -> importCsvLauncher.launch(new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/*",
                "*/*"
        }));
        btnInventoryStart.setOnClickListener(v -> {
            if (isAnyScannerRunning()) {
                stopAllScanning();
            } else {
                startSelectedScanner();
            }
        });
        btnInventoryStop.setOnClickListener(v -> stopAllScanning());
        btnInventoryToggleOptions.setOnClickListener(v -> toggleInventoryOptions());
        btnInventoryClear.setOnClickListener(v -> clearSessionResults());
        btnInventoryExport.setOnClickListener(v -> exportInventoryResults());
        btnInventorySubmitCheckin.setOnClickListener(v -> submitInventoryCheckin());
        etInventorySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable == null ? "" : editable.toString();
                inventoryController.setCurrentSearchQuery(query);
                scheduleInventorySearchRefresh();
            }
        });
        updatePrimaryActionButton();
        updateTransferButtons();
    }

    private void hydrateCachedAssetsAsync() {
        long requestToken = beginSourceLoad();
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_CACHE_LOAD, "load_requested", "source=runtime_cache");
        trace.markStart(logRepository);
        assetRepository.loadCacheSnapshotAsync(snapshot -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (!isCurrentSourceLoad(requestToken)) {
                logStaleSourceCallbackIgnored("runtime_cache", requestToken);
                return;
            }
            try {
                int assetCount = snapshot == null ? 0 : snapshot.getAssetCount();
                String source = snapshot == null ? "CACHE" : snapshot.getSource();
                trace.finish(logRepository, "load_completed", "assetCount=" + assetCount + " | source=" + source);
                if (assetCount <= 0) {
                    completeSourceLoad(requestToken);
                    cancelPendingInventoryRefreshes();
                    tvDataSourceStatus.setText(getString(R.string.inventory_source_empty));
                    refreshInventoryResults(false);
                    return;
                }
                applySourceAssetsAsync(snapshot.getAssets(), source, false, requestToken);
            } catch (Exception exception) {
                failSourceLoad(requestToken);
                AppFailureReporter.report(
                        logRepository,
                        trace,
                        SCREEN,
                        FLOW_CACHE_LOAD,
                        "load_failed",
                        AppErrorCodes.UI_RENDER_FAILED,
                        exception
                );
            }
        });
    }

    private void updateSessionInfo() {
        SessionConfig currentSession = inventoryController.getCurrentSession();
        if (currentSession == null) {
            inventoryController.setCurrentSession(sessionRepository.getSession());
            currentSession = inventoryController.getCurrentSession();
        }
        tvSessionInfo.setText(getString(
                R.string.inventory_session_brief,
                valueOrDash(currentSession == null ? "" : currentSession.getOperatorName()),
                valueOrDash(currentSession == null ? "" : currentSession.getDepartment())
        ));
    }

    private void toggleInventoryOptions() {
        if (layoutInventoryOptions == null || btnInventoryToggleOptions == null) {
            return;
        }
        boolean showing = layoutInventoryOptions.getVisibility() == View.VISIBLE;
        layoutInventoryOptions.setVisibility(showing ? View.GONE : View.VISIBLE);
        btnInventoryToggleOptions.setText(showing
                ? R.string.inventory_toggle_options_show
                : R.string.inventory_toggle_options_hide);
    }

    private void loadAssetsFromApi() {
        if (!NetworkUtils.isConnected(this)) {
            tvDataSourceStatus.setText(getString(R.string.inventory_source_api_offline));
            showToast(getString(R.string.inventory_source_api_offline));
            return;
        }
        long requestToken = beginSourceLoad();
        tvDataSourceStatus.setText(getString(R.string.inventory_source_loading_api));
        assetRepository.loadAssetsFromApi(new AssetRepositoryCallback() {
            @Override
            public void onSuccess(List<Asset> assets, String message) {
                if (!isCurrentSourceLoad(requestToken)) {
                    logStaleSourceCallbackIgnored("api", requestToken);
                    return;
                }
                applySourceAssetsAsync(assets, getString(R.string.inventory_source_api_label), true, requestToken);
                showToast(message);
            }

            @Override
            public void onError(String message) {
                if (!isCurrentSourceLoad(requestToken)) {
                    logStaleSourceCallbackIgnored("api", requestToken);
                    return;
                }
                failSourceLoad(requestToken);
                tvDataSourceStatus.setText(message);
                showToast(message);
                if (!initialSourceResolved) {
                    hydrateCachedAssetsAsync();
                }
            }
        });
    }

    private void onCsvSelected(Uri uri) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (uri == null) {
            showToast(getString(R.string.inventory_source_csv_cancelled));
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (Exception ignored) {
        }
        long requestToken = beginSourceLoad();
        tvDataSourceStatus.setText(getString(R.string.inventory_source_loading_csv));
        assetRepository.importAssetsFromCsv(this, uri, new AssetRepositoryCallback() {
            @Override
            public void onSuccess(List<Asset> assets, String message) {
                if (!isCurrentSourceLoad(requestToken)) {
                    logStaleSourceCallbackIgnored("csv", requestToken);
                    return;
                }
                applySourceAssetsAsync(assets, getString(R.string.inventory_source_csv_label), true, requestToken);
                showToast(message);
            }

            @Override
            public void onError(String message) {
                if (!isCurrentSourceLoad(requestToken)) {
                    logStaleSourceCallbackIgnored("csv", requestToken);
                    return;
                }
                failSourceLoad(requestToken);
                tvDataSourceStatus.setText(message);
                showToast(message);
                if (!initialSourceResolved) {
                    hydrateCachedAssetsAsync();
                }
            }
        });
    }

    private void applySourceAssetsAsync(List<Asset> assets, String sourceLabel, boolean scrollToTop, long requestToken) {
        List<Asset> safeAssets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        PerfLogger.Trace trace = PerfLogger.start(
                SCREEN,
                FLOW_SOURCE_PREPARE,
                "prepare_requested",
                "assetCount=" + safeAssets.size() + " | source=" + valueOrDash(sourceLabel)
        );
        trace.markStart(logRepository);
        backgroundExecutor.execute(() -> {
            if (!isCurrentSourceLoad(requestToken)) {
                return;
            }
            InventoryController.SourceSnapshot snapshot = inventoryController.prepareSourceAssets(safeAssets, sourceLabel);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (!isCurrentSourceLoad(requestToken)) {
                    logStaleSourceCallbackIgnored(valueOrDash(sourceLabel), requestToken);
                    return;
                }
                try {
                    InventoryController.SourceLoadResult loadResult = inventoryController.applySourceAssets(snapshot);
                    completeSourceLoad(requestToken);
                    cancelPendingInventoryRefreshes();
                    tvDataSourceStatus.setText(getString(
                            R.string.inventory_source_status,
                            valueOrDash(loadResult.getSourceLabel()),
                            getString(R.string.inventory_source_count_assets, loadResult.getSourceCount())
                    ));
                    refreshInventoryResults(scrollToTop);
                    trace.finish(
                            logRepository,
                            "prepare_completed",
                            "sourceCount=" + loadResult.getSourceCount() + " | source=" + valueOrDash(loadResult.getSourceLabel())
                    );
                } catch (Exception exception) {
                    failSourceLoad(requestToken);
                    AppFailureReporter.report(
                            logRepository,
                            trace,
                            SCREEN,
                            FLOW_SOURCE_PREPARE,
                            "prepare_failed",
                            AppErrorCodes.UI_RENDER_FAILED,
                            exception
                    );
                }
            });
        });
    }

    private void startSelectedScanner() {
        if (!ensureInventorySourceReadyForScan()) {
            return;
        }
        if (inventoryScannerController != null) {
            inventoryScannerController.handleTriggerDown(
                    this,
                    rbScannerQr.isChecked(),
                    swSingleScan.isChecked()
            );
        }
    }

    private void scheduleScannerWarmup() {
        if (inventoryScannerController != null) {
            inventoryScannerController.warmup(this);
        }
    }

    @Override
    public void onQrScanResult(String code, long timestamp) {
        processQrResult(code, timestamp);
    }

    @Override
    public void onRfidScanResult(UhfScanData scanData) {
        processRfidResult(scanData);
    }

    @Override
    public void onRfidSingleScanCompleted() {
        if (uiRenderer != null) {
            uiRenderer.renderScannerStatus(getString(R.string.inventory_scanner_rfid_single_done));
        }
        updatePrimaryActionButton();
    }

    @Override
    public void onScannerError(String message) {
        showToast(message);
        updateScannerStatus();
    }

    @Override
    public void onScannerStateChanged() {
        updateScannerStatus();
    }

    private void processRfidResult(UhfScanData scanData) {
        InventoryController.ScanResult result = inventoryController.handleRfidScan(
                scanData,
                getCurrentInventoryNote()
        );
        if (!result.isHandled()) {
            return;
        }
        tvScannerStatus.setText(getString(
                R.string.inventory_scanner_rfid_matched,
                valueOrDash(result.getDisplayTid()),
                valueOrDash(result.getDisplayEpcHex())
        ));
        scheduleInventoryScanRefresh(true);
    }

    private void processQrResult(String code, long timestamp) {
        InventoryController.ScanResult result = inventoryController.handleQrScan(
                code,
                timestamp,
                getCurrentInventoryNote()
        );
        if (!result.isHandled()) {
            return;
        }
        tvScannerStatus.setText(getString(R.string.inventory_scanner_qr_matched, valueOrDash(code)));
        scheduleInventoryScanRefresh(true);
    }

    private void clearSessionResults() {
        stopAllScanning();
        cancelPendingInventoryRefreshes();
        inventoryController.clearSessionResults();
        refreshInventoryResults(false);
        showToast(getString(R.string.inventory_session_cleared));
    }

    private void exportInventoryResults() {
        DebugEventLogger.info(logRepository, SCREEN, "export_inventory", "export_requested", "source=button");
        if (uploadingCheckin) {
            showToast(getString(R.string.inventory_checkin_running));
            return;
        }
        List<InventorySessionItem> exportItems = inventoryController.buildExportItems();
        if (exportItems.isEmpty()) {
            showToast(getString(R.string.inventory_export_empty_results));
            return;
        }
        if (exporting) {
            return;
        }
        if (!ensureExportStoragePermission()) {
            return;
        }

        exporting = true;
        updateTransferButtons();
        tvDataSourceStatus.setText(getString(R.string.inventory_export_running));
        List<InventorySessionItem> snapshot = new ArrayList<>(exportItems);
        backgroundExecutor.execute(() -> {
            try {
                File file = exportManager.export(this, snapshot);
                logRepository.logInfo("EXPORT_FILE", "Da export ket qua kiem ke", file.getAbsolutePath());
                mainHandler.post(() -> {
                    exporting = false;
                    updateTransferButtons();
                    restoreInventoryDataSourceStatus();
                    showToast(getString(R.string.inventory_export_done_csv, file.getAbsolutePath()));
                });
            } catch (Exception exception) {
                logRepository.logError("ERROR", "Export ket qua kiem ke that bai", exception.getMessage());
                mainHandler.post(() -> {
                    exporting = false;
                    updateTransferButtons();
                    restoreInventoryDataSourceStatus();
                    showToast(getString(R.string.inventory_export_failed));
                });
            }
        });
    }

    private void submitInventoryCheckin() {
        if (!NetworkUtils.isConnected(this)) {
            tvDataSourceStatus.setText(getString(R.string.inventory_checkin_offline));
            showToast(getString(R.string.inventory_checkin_offline));
            return;
        }
        if (exporting) {
            showToast(getString(R.string.inventory_export_running));
            return;
        }
        if (uploadingCheckin) {
            return;
        }

        List<InventorySessionItem> snapshotItems = new ArrayList<>(inventoryController.buildExportItems());
        if (snapshotItems.isEmpty()) {
            showToast(getString(R.string.inventory_checkin_empty_results));
            return;
        }

        InventoryCheckinBatchRequestDto requestDto = inventoryCheckinService.buildRequest(snapshotItems);
        if (requestDto.isEmpty()) {
            logRepository.logInfo(
                    "CHECKIN_UPLOAD_UI",
                    "Khong co item hop le de gui kiem ke",
                    "sessionItems=" + snapshotItems.size() + " | validItems=0"
            );
            showToast(getString(R.string.inventory_checkin_empty_results));
            return;
        }

        logRepository.logInfo(
                "CHECKIN_UPLOAD_UI",
                "Nguoi dung bat dau gui kiem ke",
                "sessionItems=" + snapshotItems.size()
                        + " | validItems=" + requestDto.size()
                        + " | sample=" + inventoryCheckinService.describeFirstItem(requestDto)
        );
        uploadingCheckin = true;
        updateTransferButtons();
        tvDataSourceStatus.setText(getString(R.string.inventory_checkin_running));
        backgroundExecutor.execute(() -> {
            try {
                InventoryCheckinUploadResult result = inventoryCheckinService.uploadRequest(requestDto);
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    uploadingCheckin = false;
                    updateTransferButtons();
                    restoreInventoryDataSourceStatus();
                    String message = resolveCheckinCompletionMessage(result);
                    logRepository.logInfo(
                            "CHECKIN_UPLOAD_UI",
                            result.isWarning()
                                    ? "Gui kiem ke xong voi canh bao"
                                    : "Gui kiem ke thanh cong",
                            safe(message)
                    );
                    showToast(message);
                });
            } catch (InventoryCheckinService.UploadFailureException exception) {
                mainHandler.post(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    uploadingCheckin = false;
                    updateTransferButtons();
                    restoreInventoryDataSourceStatus();
                    String message = safe(exception.getMessage());
                    if (message.isEmpty()) {
                        message = getString(R.string.inventory_checkin_offline);
                    }
                    logRepository.logError(
                            "CHECKIN_UPLOAD_UI",
                            "Gui kiem ke that bai",
                            message
                    );
                    showToast(message);
                });
            }
        });
    }

    private boolean ensureExportStoragePermission() {
        if (AppPermissionManager.hasExportStoragePermission(this)) {
            return true;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Intent intent = AppPermissionManager.buildManageAllFilesAccessIntent(this);
            startActivity(intent);
            showToast(getString(R.string.checkout_need_storage));
            return false;
        }
        writeStoragePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return false;
    }

    private void restoreInventoryDataSourceStatus() {
        tvDataSourceStatus.setText(getString(
                R.string.inventory_source_status,
                valueOrDash(inventoryController.getCurrentDataSourceLabel()),
                getString(R.string.inventory_source_count_assets, inventoryController.getState().getSourceItems().size())
        ));
    }

    private void updateTransferButtons() {
        boolean transferRunning = exporting || uploadingCheckin;
        if (btnInventoryExport != null) {
            btnInventoryExport.setEnabled(!transferRunning);
        }
        if (btnInventorySubmitCheckin != null) {
            btnInventorySubmitCheckin.setEnabled(!transferRunning);
        }
        if (btnInventoryImportCsv != null) {
            btnInventoryImportCsv.setEnabled(!transferRunning);
        }
        if (btnInventoryClear != null) {
            btnInventoryClear.setEnabled(!transferRunning);
        }
    }

    private String resolveCheckinCompletionMessage(InventoryCheckinUploadResult result) {
        if (result == null || result.getResponse() == null) {
            return getString(R.string.inventory_checkin_success);
        }
        int totalInserted = result.getResponse().getTotalInserted();
        int totalSkipped = result.getResponse().getTotalSkipped();
        int totalReceived = result.getResponse().getTotalReceived();

        if (result.isWarning()) {
            if (totalReceived > 0) {
                return getString(R.string.inventory_checkin_warning_summary, totalReceived);
            }
            String warningMessage = safe(result.getUserMessage());
            return warningMessage.isEmpty()
                    ? getString(R.string.inventory_checkin_warning_empty)
                    : warningMessage;
        }

        if (totalInserted > 0 || totalSkipped > 0) {
            return getString(R.string.inventory_checkin_success_summary, totalInserted, totalSkipped);
        }

        String successMessage = safe(result.getUserMessage());
        return successMessage.isEmpty()
                ? getString(R.string.inventory_checkin_success)
                : successMessage;
    }

    private void stopAllScanning() {
        if (inventoryScannerController != null) {
            inventoryScannerController.stopAllScanning();
        }
        updateScannerStatus();
    }

    private void refreshInventoryResults(boolean scrollToTop) {
        List<InventorySessionItem> items = inventoryController.buildOrderedItems();
        InventoryController.InventorySummary summary = inventoryController.buildSummary();
        uiRenderer.renderResults(items, summary, scrollToTop, resolveEmptyStateMessage());
    }

    private void scheduleInventorySearchRefresh() {
        mainHandler.removeCallbacks(inventorySearchRefreshRunnable);
        mainHandler.postDelayed(inventorySearchRefreshRunnable, FILTER_DEBOUNCE_MS);
    }

    private void runInventorySearchRefresh() {
        String query = inventoryController.getCurrentSearchQuery();
        PerfLogger.Trace trace = PerfLogger.start(
                SCREEN,
                FLOW_FILTER,
                "filter_requested",
                "queryLength=" + safe(query).trim().length()
        );
        refreshInventoryResults(false);
        trace.finish(logRepository, "filter_completed", "queryLength=" + safe(query).trim().length());
    }

    private void scheduleInventoryScanRefresh(boolean scrollToTop) {
        pendingInventoryScrollToTop = pendingInventoryScrollToTop || scrollToTop;
        if (inventoryScanRefreshScheduled) {
            return;
        }
        inventoryScanRefreshScheduled = true;
        mainHandler.postDelayed(inventoryScanRefreshRunnable, SCAN_REFRESH_DEBOUNCE_MS);
    }

    private void runInventoryScanRefresh() {
        inventoryScanRefreshScheduled = false;
        boolean scrollToTop = pendingInventoryScrollToTop;
        pendingInventoryScrollToTop = false;
        refreshInventoryResults(scrollToTop);
    }

    private void cancelPendingInventoryRefreshes() {
        inventoryScanRefreshScheduled = false;
        pendingInventoryScrollToTop = false;
        mainHandler.removeCallbacks(inventorySearchRefreshRunnable);
        mainHandler.removeCallbacks(inventoryScanRefreshRunnable);
    }

    private void updateScannerStatus() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (inventoryScannerController == null) {
            return;
        }
        String scannerStatus;
        if (rbScannerQr != null && rbScannerQr.isChecked()) {
            if (inventoryScannerController.isQrScanning()) {
                scannerStatus = getString(R.string.inventory_scanner_qr_running);
            } else {
                scannerStatus = inventoryScannerController.getQrStatusMessage();
            }
            uiRenderer.renderScannerStatus(scannerStatus);
            updatePrimaryActionButton();
            return;
        }

        if (inventoryScannerController.isDemoInventoryRunning()) {
            scannerStatus = getString(R.string.inventory_scanner_rfid_running);
        } else if (inventoryScannerController.isDemoReaderReady()) {
            scannerStatus = getString(R.string.inventory_scanner_uhf_ready);
        } else {
            scannerStatus = getString(R.string.inventory_scanner_uhf_not_ready);
        }
        uiRenderer.renderScannerStatus(scannerStatus);
        updatePrimaryActionButton();
    }

    private boolean isAnyScannerRunning() {
        return inventoryScannerController != null && inventoryScannerController.isAnyScannerRunning();
    }

    private void updatePrimaryActionButton() {
        uiRenderer.renderPrimaryAction(isAnyScannerRunning());
    }

    private String resolveEmptyStateMessage() {
        if (inventoryController.getCurrentSearchQuery() != null && !inventoryController.getCurrentSearchQuery().trim().isEmpty()) {
            return getString(R.string.inventory_empty_state_search);
        }
        return getString(R.string.inventory_empty_placeholder);
    }

    private String getCurrentOperatorName() {
        return inventoryController.getCurrentOperatorName();
    }

    private String getCurrentInventoryNote() {
        String screenNote = etInventoryNote == null ? "" : safe(etInventoryNote.getText() == null ? "" : etInventoryNote.getText().toString());
        return inventoryController.resolveInventoryNote(screenNote);
    }

    private String normalize(String value) {
        return safe(value).trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private long beginSourceLoad() {
        sourceLoadInProgress = true;
        activeSourceLoadToken += 1L;
        return activeSourceLoadToken;
    }

    private boolean isCurrentSourceLoad(long requestToken) {
        return requestToken == activeSourceLoadToken;
    }

    private void completeSourceLoad(long requestToken) {
        if (!isCurrentSourceLoad(requestToken)) {
            return;
        }
        initialSourceResolved = true;
        sourceLoadInProgress = false;
    }

    private void failSourceLoad(long requestToken) {
        if (!isCurrentSourceLoad(requestToken)) {
            return;
        }
        sourceLoadInProgress = false;
    }

    private boolean ensureInventorySourceReadyForScan() {
        if (initialSourceResolved && !sourceLoadInProgress) {
            return true;
        }
        DebugEventLogger.info(
                logRepository,
                SCREEN,
                FLOW_CACHE_LOAD,
                "source_not_ready",
                "request=scan | resolved=" + initialSourceResolved + " | loading=" + sourceLoadInProgress
        );
        showToast("Du lieu kiem ke dang khoi tao. Vui long thu lai sau.");
        return false;
    }

    private void logStaleSourceCallbackIgnored(String source, long requestToken) {
        DebugEventLogger.info(
                logRepository,
                SCREEN,
                FLOW_CACHE_LOAD,
                "stale_source_callback_ignored",
                "source=" + valueOrDash(source) + " | requestToken=" + requestToken + " | activeToken=" + activeSourceLoadToken
        );
    }

}
