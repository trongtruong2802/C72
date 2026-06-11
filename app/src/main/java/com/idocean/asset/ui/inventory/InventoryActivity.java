package com.idocean.asset.ui.inventory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Man kiem ke bam truc tiep logic UHF goc cua demo Chainway de dam bao lay duoc TID that.
 */
public class InventoryActivity extends AppCompatActivity implements ScannerTriggerHandler, InventoryScannerController.Callback {
    private static final String TAG_INVENTORY = "INVENTORY";
    private static final String TAG_INV_PERF = "INV_PERF";

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

    private long openPerfStartMs;
    private boolean exporting;
    private boolean uploadingCheckin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        openPerfStartMs = SystemClock.elapsedRealtime();
        Log.d(TAG_INV_PERF, "[INV_PERF] onCreate start");
        setContentView(R.layout.activity_ido_inventory);

        sessionRepository = new SessionRepository(getApplicationContext());
        inventoryController.setCurrentSession(sessionRepository.getSession());
        inventoryScannerController = new InventoryScannerController(this, logRepository);
        bindViews();
        setupRecyclerView();
        Log.d(TAG_INV_PERF, "[INV_PERF] init recycler end +" + elapsedFromOpen() + "ms");
        setupControls();
        updateSessionInfo();
        updateScannerStatus();
        findViewById(android.R.id.content).post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            Log.d(TAG_INV_PERF, "[INV_PERF] first UI render end +" + elapsedFromOpen() + "ms");
            hydrateCachedAssetsAsync();
            scheduleScannerWarmup();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG_INV_PERF, "[INV_PERF] onResume +" + elapsedFromOpen() + "ms");
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
        inventoryScannerController.handleTriggerDown(this, rbScannerQr.isChecked(), swSingleScan.isChecked());
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
                inventoryController.setCurrentSearchQuery(editable == null ? "" : editable.toString());
                refreshInventoryResults(false);
            }
        });
        updatePrimaryActionButton();
        updateTransferButtons();
    }

    private void hydrateCachedAssetsAsync() {
        List<Asset> cachedAssets = assetRepository.getCachedAssets();
        if (cachedAssets.isEmpty()) {
            tvDataSourceStatus.setText(getString(R.string.inventory_source_empty));
            refreshInventoryResults(false);
            return;
        }
        applySourceAssetsAsync(cachedAssets, assetRepository.getLastSource(), false);
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
        tvDataSourceStatus.setText(getString(R.string.inventory_source_loading_api));
        assetRepository.loadAssetsFromApi(new AssetRepositoryCallback() {
            @Override
            public void onSuccess(List<Asset> assets, String message) {
                applySourceAssetsAsync(assets, getString(R.string.inventory_source_api_label), true);
                showToast(message);
            }

            @Override
            public void onError(String message) {
                tvDataSourceStatus.setText(message);
                showToast(message);
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
        tvDataSourceStatus.setText(getString(R.string.inventory_source_loading_csv));
        assetRepository.importAssetsFromCsv(this, uri, new AssetRepositoryCallback() {
            @Override
            public void onSuccess(List<Asset> assets, String message) {
                applySourceAssetsAsync(assets, getString(R.string.inventory_source_csv_label), true);
                showToast(message);
            }

            @Override
            public void onError(String message) {
                tvDataSourceStatus.setText(message);
                showToast(message);
            }
        });
    }

    private void applySourceAssetsAsync(List<Asset> assets, String sourceLabel, boolean scrollToTop) {
        List<Asset> safeAssets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        Log.d(TAG_INV_PERF, "[INV_PERF] build asset maps start +" + elapsedFromOpen() + "ms");
        backgroundExecutor.execute(() -> {
            InventoryController.SourceSnapshot snapshot = inventoryController.prepareSourceAssets(safeAssets, sourceLabel);
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                InventoryController.SourceLoadResult loadResult = inventoryController.applySourceAssets(snapshot);
                tvDataSourceStatus.setText(getString(
                        R.string.inventory_source_status,
                        valueOrDash(loadResult.getSourceLabel()),
                        getString(R.string.inventory_source_count_assets, loadResult.getSourceCount())
                ));
                refreshInventoryResults(scrollToTop);
                Log.d(TAG_INV_PERF, "[INV_PERF] build asset maps end +" + elapsedFromOpen() + "ms");
            });
        });
    }

    private void startSelectedScanner() {
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
        if (tvScannerStatus != null) {
            tvScannerStatus.setText(getString(R.string.inventory_scanner_rfid_single_done));
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
        refreshInventoryResults(true);
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
        refreshInventoryResults(true);
    }

    private void clearSessionResults() {
        stopAllScanning();
        inventoryController.clearSessionResults();
        refreshInventoryResults(false);
        showToast(getString(R.string.inventory_session_cleared));
    }

    private void exportInventoryResults() {
        Log.d("EXPORT", "[EXPORT] export button clicked");
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
                Log.e("EXPORT", "[EXPORT] export failed", exception);
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
        adapter.submitList(items);
        updateSummary();
        boolean hasItems = !items.isEmpty();
        rvInventoryResults.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (tvInventoryEmpty != null) {
            tvInventoryEmpty.setVisibility(hasItems ? View.GONE : View.VISIBLE);
            tvInventoryEmpty.setText(resolveEmptyStateMessage());
        }
        Log.d(TAG_INVENTORY, "[INVENTORY] item count after update=" + items.size());
        if (scrollToTop && !items.isEmpty()) {
            rvInventoryResults.scrollToPosition(0);
        }
    }

    private void updateSummary() {
        InventoryController.InventorySummary summary = inventoryController.buildSummary();
        tvSummaryTotal.setText(String.valueOf(summary.getScannedCount()));
        tvSummaryDatasetTotal.setText(String.valueOf(summary.getDatasetCount()));
        tvSummaryChecked.setText(String.valueOf(summary.getMatchedCount()));
    }

    private void updateScannerStatus() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (inventoryScannerController == null) {
            return;
        }
        if (rbScannerQr != null && rbScannerQr.isChecked()) {
            if (inventoryScannerController.isQrScanning()) {
                tvScannerStatus.setText(getString(R.string.inventory_scanner_qr_running));
            } else {
                tvScannerStatus.setText(inventoryScannerController.getQrStatusMessage());
            }
            updatePrimaryActionButton();
            return;
        }

        if (inventoryScannerController.isDemoInventoryRunning()) {
            tvScannerStatus.setText(getString(R.string.inventory_scanner_rfid_running));
        } else if (inventoryScannerController.isDemoReaderReady()) {
            tvScannerStatus.setText(getString(R.string.inventory_scanner_uhf_ready));
        } else {
            tvScannerStatus.setText(getString(R.string.inventory_scanner_uhf_not_ready));
        }
        updatePrimaryActionButton();
    }

    private boolean isAnyScannerRunning() {
        return inventoryScannerController != null && inventoryScannerController.isAnyScannerRunning();
    }

    private void updatePrimaryActionButton() {
        if (btnInventoryStart == null) {
            return;
        }
        btnInventoryStart.setText(isAnyScannerRunning()
                ? R.string.inventory_stop_action
                : R.string.inventory_start_action);
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

    private long elapsedFromOpen() {
        if (openPerfStartMs <= 0L) {
            return 0L;
        }
        return SystemClock.elapsedRealtime() - openPerfStartMs;
    }

}
