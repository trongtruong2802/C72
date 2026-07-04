package com.idocean.asset.ui.checkout;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.tabs.TabLayout;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.CheckoutCsvRepository;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.data.repository.SessionRepository;
import com.idocean.asset.diagnostics.AppFailureReporter;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckOutFormData;
import com.idocean.asset.model.CheckoutAssetItem;
import com.idocean.asset.model.ImportedCheckoutData;
import com.idocean.asset.model.SessionConfig;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.scanner.rfid.ScannerTriggerHandler;
import com.idocean.asset.storage.AppPermissionManager;
import com.idocean.asset.utils.HardwareKeyUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckoutActivity extends AppCompatActivity implements ScannerTriggerHandler, CheckoutScannerController.Callback {
    private static final String SCREEN = "Checkout";
    private static final String FLOW_SCREEN_OPEN = "screen_open";
    private static final String FLOW_CACHE_LOAD = "cache_load";
    private static final long SCAN_REFRESH_DEBOUNCE_MS = 75L;

    enum ScreenTab {
        CHECKOUT,
        CHECKIN
    }

    private static final String STATE_ACTIVE_TAB = "checkout_active_tab";

    private final AssetRepository assetRepository = AssetRepository.getInstance();
    private final CheckoutCsvRepository csvRepository = new CheckoutCsvRepository();
    private final LogRepository logRepository = LogRepository.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final CheckoutController checkoutController = new CheckoutController(csvRepository, logRepository);
    private final CheckoutScannerController checkoutScannerController = new CheckoutScannerController(this, logRepository);
    private final Runnable checkoutRefreshRunnable = this::runScheduledCheckoutRefresh;
    private final Runnable checkinRefreshRunnable = this::runScheduledCheckinRefresh;

    private final CheckoutAssetAdapter checkoutAdapter = new CheckoutAssetAdapter(this::removeCheckoutItem);
    private final CheckInResultAdapter checkinAdapter = new CheckInResultAdapter();
    private CheckoutUiRenderer uiRenderer;

    private final ActivityResultLauncher<String[]> importCheckoutLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onCheckoutCsvSelected);
    private final ActivityResultLauncher<String> writeStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    logRepository.logError("ERROR", "Khong duoc cap quyen ghi file check out/check in");
                    showToast(getString(R.string.checkout_need_storage));
                }
            });

    private SessionRepository sessionRepository;
    private ScreenTab activeTab = ScreenTab.CHECKOUT;
    private AssetRepository.CacheSnapshot cacheSnapshot;
    private PerfLogger.Trace screenOpenTrace;
    private boolean initialCacheResolved;
    private boolean checkoutRefreshScheduled;
    private boolean checkinRefreshScheduled;

    private TabLayout tabsCheckoutMode;
    private NestedScrollView scrollCheckoutTab;
    private NestedScrollView scrollCheckinTab;
    private TextView tvCheckoutDataStatus;
    private TextView tvCheckoutScannerStatus;
    private TextView tvCheckoutSummarySelected;
    private TextView tvCheckoutSummaryCached;
    private TextView tvCheckoutSummaryOutsideCache;
    private EditText etCheckoutCarrierName;
    private MaterialAutoCompleteTextView etCheckoutDepartment;
    private EditText etCheckoutPurpose;
    private EditText etCheckoutEvent;
    private EditText etCheckoutTime;
    private EditText etCheckoutExpectedReturn;
    private EditText etCheckoutApprover;
    private EditText etCheckoutNote;
    private TextView tvCheckoutCount;
    private RadioGroup rgCheckoutScannerType;
    private RadioButton rbCheckoutRfid;
    private RadioButton rbCheckoutQr;
    private SwitchCompat swCheckoutContinuousScan;
    private SwitchCompat swCheckoutSingleScan;
    private Button btnCheckoutScan;
    private Button btnCheckoutStop;
    private Button btnCheckoutClear;
    private Button btnCheckoutExport;

    private TextView tvCheckinImportStatus;
    private TextView tvCheckinImportMeta;
    private TextView tvCheckinScannerStatus;
    private TextView tvCheckinSummaryHeadline;
    private TextView tvCheckinSummaryDetail;
    private TextView tvCheckinSummaryTotal;
    private TextView tvCheckinSummaryReturned;
    private TextView tvCheckinSummaryMissing;
    private LinearProgressIndicator progressCheckinSummary;
    private RadioGroup rgCheckinScannerType;
    private RadioButton rbCheckinRfid;
    private RadioButton rbCheckinQr;
    private SwitchCompat swCheckinContinuousScan;
    private SwitchCompat swCheckinSingleScan;
    private Button btnCheckinImport;
    private Button btnCheckinScan;
    private Button btnCheckinStop;
    private Button btnCheckinClear;
    private Button btnCheckinExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        screenOpenTrace = PerfLogger.start(SCREEN, FLOW_SCREEN_OPEN, "onCreate", "activity=CheckoutActivity");
        screenOpenTrace.markStart(logRepository);
        setContentView(R.layout.activity_ido_checkout);

        sessionRepository = new SessionRepository(getApplicationContext());

        MaterialToolbar toolbar = findViewById(R.id.toolbarIdoCheckout);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.checkout_title_screen);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        uiRenderer = new CheckoutUiRenderer(
                checkoutAdapter,
                checkinAdapter,
                tvCheckoutCount,
                tvCheckoutSummarySelected,
                tvCheckoutSummaryCached,
                tvCheckoutSummaryOutsideCache,
                tvCheckinSummaryHeadline,
                tvCheckinSummaryDetail,
                tvCheckinSummaryTotal,
                tvCheckinSummaryReturned,
                tvCheckinSummaryMissing,
                progressCheckinSummary,
                tvCheckoutScannerStatus,
                tvCheckinScannerStatus,
                btnCheckoutScan,
                btnCheckoutStop,
                btnCheckoutClear,
                btnCheckoutExport,
                btnCheckinScan,
                btnCheckinStop,
                btnCheckinClear,
                btnCheckinExport
        );
        setupLists();
        setupTabs();
        setupControls();

        RetainedState retainedState = (RetainedState) getLastCustomNonConfigurationInstance();
        if (retainedState != null) {
            restoreRetainedState(retainedState);
        }

        if (savedInstanceState != null) {
            activeTab = ScreenTab.valueOf(savedInstanceState.getString(STATE_ACTIVE_TAB, ScreenTab.CHECKOUT.name()));
        }

        applySessionDefaultsIfNeeded();
        refreshAllViews();
        setActiveTab(activeTab);
        loadCacheSnapshotAsync();
        findViewById(android.R.id.content).post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (screenOpenTrace != null) {
                screenOpenTrace.finish(logRepository, "first_render", "contentReady=true");
                screenOpenTrace = null;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkoutScannerController.onStart(this);
        updateScannerStatuses();
        updateButtons();
    }

    @Override
    protected void onStop() {
        checkoutScannerController.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        checkoutScannerController.shutdown();
        mainHandler.removeCallbacks(checkoutRefreshRunnable);
        mainHandler.removeCallbacks(checkinRefreshRunnable);
        ioExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return new RetainedState(activeTab, checkoutController.snapshot());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ACTIVE_TAB, activeTab.name());
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
        if (activeTab == ScreenTab.CHECKOUT) {
            startCheckoutScan();
            return;
        }
        startCheckinScan();
    }

    @Override
    public void onScannerTriggerUp() {
        checkoutScannerController.handleTriggerUp(isCurrentRfidContinuousMode());
        updateScannerStatuses();
    }

    @Override
    public void onQrScanResult(String code, long timestamp) {
        if (activeTab == ScreenTab.CHECKOUT) {
            processCheckoutQr(code, timestamp);
        } else {
            processCheckinQr(code, timestamp);
        }
    }

    @Override
    public void onRfidScanResult(ScreenTab targetTab, CheckoutScannerController.RfidReadResult result, boolean suppressDuplicateToast) {
        if (targetTab == ScreenTab.CHECKOUT) {
            processCheckoutRfid(result, suppressDuplicateToast);
        } else {
            processCheckinRfid(result, suppressDuplicateToast);
        }
    }

    @Override
    public void onScannerError(String message) {
        logRepository.logError("ERROR", "Scanner loi o man check out/check in", message);
        showToast(message);
        updateScannerStatuses();
    }

    @Override
    public void onScannerStateChanged() {
        updateScannerStatuses();
        updateButtons();
    }

    private void bindViews() {
        tabsCheckoutMode = findViewById(R.id.tabsCheckoutMode);
        scrollCheckoutTab = findViewById(R.id.scrollCheckoutTab);
        scrollCheckinTab = findViewById(R.id.scrollCheckinTab);
        tvCheckoutDataStatus = findViewById(R.id.tvCheckoutDataStatus);
        tvCheckoutScannerStatus = findViewById(R.id.tvCheckoutScannerStatus);
        tvCheckoutSummarySelected = findViewById(R.id.tvCheckoutSummarySelected);
        tvCheckoutSummaryCached = findViewById(R.id.tvCheckoutSummaryCached);
        tvCheckoutSummaryOutsideCache = findViewById(R.id.tvCheckoutSummaryOutsideCache);
        etCheckoutCarrierName = findViewById(R.id.etCheckoutCarrierName);
        etCheckoutDepartment = findViewById(R.id.etCheckoutDepartment);
        etCheckoutPurpose = findViewById(R.id.etCheckoutPurpose);
        etCheckoutEvent = findViewById(R.id.etCheckoutEvent);
        etCheckoutTime = findViewById(R.id.etCheckoutTime);
        etCheckoutExpectedReturn = findViewById(R.id.etCheckoutExpectedReturn);
        etCheckoutApprover = findViewById(R.id.etCheckoutApprover);
        etCheckoutNote = findViewById(R.id.etCheckoutNote);
        tvCheckoutCount = findViewById(R.id.tvCheckoutCount);
        rgCheckoutScannerType = findViewById(R.id.rgCheckoutScannerType);
        rbCheckoutRfid = findViewById(R.id.rbCheckoutRfid);
        rbCheckoutQr = findViewById(R.id.rbCheckoutQr);
        swCheckoutContinuousScan = findViewById(R.id.swCheckoutContinuousScan);
        swCheckoutSingleScan = findViewById(R.id.swCheckoutSingleScan);
        btnCheckoutScan = findViewById(R.id.btnCheckoutScan);
        btnCheckoutStop = findViewById(R.id.btnCheckoutStop);
        btnCheckoutClear = findViewById(R.id.btnCheckoutClear);
        btnCheckoutExport = findViewById(R.id.btnCheckoutExport);
        tvCheckinImportStatus = findViewById(R.id.tvCheckinImportStatus);
        tvCheckinImportMeta = findViewById(R.id.tvCheckinImportMeta);
        tvCheckinScannerStatus = findViewById(R.id.tvCheckinScannerStatus);
        tvCheckinSummaryHeadline = findViewById(R.id.tvCheckinSummaryHeadline);
        tvCheckinSummaryDetail = findViewById(R.id.tvCheckinSummaryDetail);
        tvCheckinSummaryTotal = findViewById(R.id.tvCheckinSummaryTotal);
        tvCheckinSummaryReturned = findViewById(R.id.tvCheckinSummaryReturned);
        tvCheckinSummaryMissing = findViewById(R.id.tvCheckinSummaryMissing);
        progressCheckinSummary = findViewById(R.id.progressCheckinSummary);
        rgCheckinScannerType = findViewById(R.id.rgCheckinScannerType);
        rbCheckinRfid = findViewById(R.id.rbCheckinRfid);
        rbCheckinQr = findViewById(R.id.rbCheckinQr);
        swCheckinContinuousScan = findViewById(R.id.swCheckinContinuousScan);
        swCheckinSingleScan = findViewById(R.id.swCheckinSingleScan);
        btnCheckinImport = findViewById(R.id.btnCheckinImport);
        btnCheckinScan = findViewById(R.id.btnCheckinScan);
        btnCheckinStop = findViewById(R.id.btnCheckinStop);
        btnCheckinClear = findViewById(R.id.btnCheckinClear);
        btnCheckinExport = findViewById(R.id.btnCheckinExport);
    }

    private void setupLists() {
        RecyclerView rvCheckoutItems = findViewById(R.id.rvCheckoutItems);
        rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        rvCheckoutItems.setAdapter(checkoutAdapter);

        RecyclerView rvCheckinItems = findViewById(R.id.rvCheckinItems);
        rvCheckinItems.setLayoutManager(new LinearLayoutManager(this));
        rvCheckinItems.setAdapter(checkinAdapter);
    }

    private void setupTabs() {
        tabsCheckoutMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setActiveTab(tab != null && tab.getPosition() == 1 ? ScreenTab.CHECKIN : ScreenTab.CHECKOUT);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupControls() {
        bindDateInputs();
        swCheckoutSingleScan.setChecked(true);
        swCheckoutContinuousScan.setChecked(false);
        swCheckinSingleScan.setChecked(true);
        swCheckinContinuousScan.setChecked(false);

        swCheckoutContinuousScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                swCheckoutSingleScan.setChecked(false);
            } else if (!swCheckoutSingleScan.isChecked()) {
                swCheckoutSingleScan.setChecked(true);
            }
            updateScannerStatuses();
        });
        swCheckoutSingleScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                swCheckoutContinuousScan.setChecked(false);
            } else if (!swCheckoutContinuousScan.isChecked()) {
                swCheckoutContinuousScan.setChecked(true);
            }
            updateScannerStatuses();
        });
        swCheckinContinuousScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                swCheckinSingleScan.setChecked(false);
            } else if (!swCheckinSingleScan.isChecked()) {
                swCheckinSingleScan.setChecked(true);
            }
            updateScannerStatuses();
        });
        swCheckinSingleScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                swCheckinContinuousScan.setChecked(false);
            } else if (!swCheckinContinuousScan.isChecked()) {
                swCheckinContinuousScan.setChecked(true);
            }
            updateScannerStatuses();
        });

        rgCheckoutScannerType.setOnCheckedChangeListener((group, checkedId) -> {
            stopAllScanning();
            updateScannerStatuses();
        });
        rgCheckinScannerType.setOnCheckedChangeListener((group, checkedId) -> {
            stopAllScanning();
            updateScannerStatuses();
        });
        btnCheckoutScan.setOnClickListener(v -> startCheckoutScan());
        btnCheckoutStop.setOnClickListener(v -> stopAllScanning());
        btnCheckoutClear.setOnClickListener(v -> clearCheckoutItems());
        btnCheckoutExport.setOnClickListener(v -> exportCheckoutCsv());
        btnCheckinImport.setOnClickListener(v -> openCheckoutCsvPicker());
        btnCheckinScan.setOnClickListener(v -> startCheckinScan());
        btnCheckinStop.setOnClickListener(v -> stopAllScanning());
        btnCheckinClear.setOnClickListener(v -> clearCheckinSession());
        btnCheckinExport.setOnClickListener(v -> exportCheckinCsv());
        bindDepartmentDropdown(new ArrayList<>());
    }

    private void restoreRetainedState(RetainedState retainedState) {
        if (retainedState == null) {
            return;
        }
        activeTab = retainedState.activeTab == null ? ScreenTab.CHECKOUT : retainedState.activeTab;
        checkoutController.restore(retainedState.snapshot);
    }

    private void applySessionDefaultsIfNeeded() {
        SessionConfig session = sessionRepository.getSession();
        boolean manualEntry = session != null && session.isManualEntryEachSession();
        if (!manualEntry) {
            if (textOf(etCheckoutCarrierName).isEmpty()) {
                etCheckoutCarrierName.setText(session.getOperatorName());
            }
            if (textOf(etCheckoutDepartment).isEmpty()) {
                etCheckoutDepartment.setText(
                        AssetFieldNormalizer.normalizeDepartmentForDisplay(session.getDepartment()),
                        false
                );
            }
            if (textOf(etCheckoutNote).isEmpty()) {
                etCheckoutNote.setText(session.getSessionNote());
            }
        }
        if (textOf(etCheckoutTime).isEmpty()) {
            etCheckoutTime.setText(csvRepository.today());
        }
    }

    private void bindDateInputs() {
        bindDateInput(etCheckoutTime, true);
        bindDateInput(etCheckoutExpectedReturn, false);
    }

    private void bindDepartmentDropdown(List<String> runtimeDepartments) {
        List<String> options = new ArrayList<>();
        String[] defaults = getResources().getStringArray(R.array.known_department_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(option);
            if (!normalized.isEmpty() && !options.contains(normalized)) {
                options.add(normalized);
            }
        }
        if (runtimeDepartments != null) {
            for (String department : runtimeDepartments) {
                String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(department);
                if (!normalized.isEmpty() && !options.contains(normalized)) {
                    options.add(normalized);
                }
            }
        }
        etCheckoutDepartment.setSimpleItems(options.toArray(new String[0]));
        String normalizedCurrent = AssetFieldNormalizer.normalizeDepartmentForDisplay(textOf(etCheckoutDepartment));
        if (!normalizedCurrent.isEmpty() && !normalizedCurrent.equals(textOf(etCheckoutDepartment))) {
            etCheckoutDepartment.setText(normalizedCurrent, false);
        }
        etCheckoutDepartment.setOnClickListener(v -> etCheckoutDepartment.showDropDown());
        etCheckoutDepartment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etCheckoutDepartment.showDropDown();
            }
        });
    }

    private void bindDateInput(EditText targetField, boolean mirrorExpectedReturn) {
        if (targetField == null) {
            return;
        }
        targetField.setFocusable(false);
        targetField.setClickable(true);
        targetField.setLongClickable(false);
        targetField.setCursorVisible(false);
        targetField.setKeyListener(null);
        targetField.setOnClickListener(v -> openDatePicker(targetField, mirrorExpectedReturn));
    }

    private void openDatePicker(EditText targetField, boolean mirrorExpectedReturn) {
        Calendar calendar = Calendar.getInstance();
        long existingDate = csvRepository.parseDateToMillis(textOf(targetField));
        if (existingDate > 0L) {
            calendar.setTimeInMillis(existingDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                    selectedDate.set(Calendar.MINUTE, 0);
                    selectedDate.set(Calendar.SECOND, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    String formattedDate = csvRepository.formatDate(selectedDate.getTimeInMillis());
                    targetField.setText(formattedDate);
                    targetField.setError(null);
                    if (mirrorExpectedReturn && textOf(etCheckoutExpectedReturn).isEmpty()) {
                        etCheckoutExpectedReturn.setText(formattedDate);
                        etCheckoutExpectedReturn.setError(null);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void rebuildCachedAssetIndex() {
        List<Asset> assets = cacheSnapshot == null ? new ArrayList<>() : cacheSnapshot.getAssets();
        checkoutController.setCachedAssets(assets);
    }

    private void loadCacheSnapshotAsync() {
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_CACHE_LOAD, "load_requested", "source=runtime_cache");
        trace.markStart(logRepository);
        assetRepository.loadCacheSnapshotAsync(snapshot -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            try {
                cacheSnapshot = snapshot;
                rebuildCachedAssetIndex();
                initialCacheResolved = true;
                bindDepartmentDropdown(snapshot == null ? new ArrayList<>() : snapshot.getDistinctValues("department"));
                updateCheckoutDataStatus();
                updateButtons();
                trace.finish(
                        logRepository,
                        "load_completed",
                        "assetCount=" + (snapshot == null ? 0 : snapshot.getAssetCount())
                                + " | source=" + (snapshot == null ? "CACHE" : snapshot.getSource())
                );
            } catch (Exception exception) {
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

    private void refreshAllViews() {
        cancelScheduledListRefreshes();
        refreshCheckoutList();
        refreshCheckinList();
        updateCheckoutDataStatus();
        updateCheckinImportInfo();
        updateScannerStatuses();
        updateButtons();
    }

    private void setActiveTab(ScreenTab tab) {
        ScreenTab nextTab = tab == null ? ScreenTab.CHECKOUT : tab;
        if (activeTab != nextTab) {
            stopAllScanning();
        }
        activeTab = nextTab;
        scrollCheckoutTab.setVisibility(activeTab == ScreenTab.CHECKOUT ? View.VISIBLE : View.GONE);
        scrollCheckinTab.setVisibility(activeTab == ScreenTab.CHECKIN ? View.VISIBLE : View.GONE);
        TabLayout.Tab selectedTab = tabsCheckoutMode.getTabAt(activeTab == ScreenTab.CHECKIN ? 1 : 0);
        if (selectedTab != null && !selectedTab.isSelected()) {
            selectedTab.select();
        }
        updateScannerStatuses();
        updateButtons();
    }

    private void updateCheckoutDataStatus() {
        int cachedCount = cacheSnapshot == null ? 0 : cacheSnapshot.getAssetCount();
        if (cachedCount <= 0) {
            tvCheckoutDataStatus.setText(R.string.checkout_data_status_empty);
            return;
        }
        tvCheckoutDataStatus.setText(getString(
                R.string.checkout_data_status_ready,
                cachedCount,
                valueOrDash(cacheSnapshot == null ? "" : cacheSnapshot.getSource())
        ));
    }

    private void updateCheckinImportInfo() {
        ImportedCheckoutData importedCheckoutData = checkoutController.getImportedCheckoutData();
        if (importedCheckoutData == null || importedCheckoutData.getFormData() == null) {
            tvCheckinImportStatus.setText(R.string.checkin_import_status_empty);
            tvCheckinImportMeta.setText("");
            return;
        }
        CheckOutFormData formData = importedCheckoutData.getFormData();
        tvCheckinImportStatus.setText(getString(
                R.string.checkin_import_status_done,
                importedCheckoutData.getExpectedItems().size(),
                importedCheckoutData.getSourceFileName()
        ));
        tvCheckinImportMeta.setText(getString(
                R.string.checkin_import_meta,
                valueOrDash(formData.getTicketId()),
                valueOrDash(formData.getCarrierName()),
                valueOrDash(formData.getCheckoutAt()),
                valueOrDash(formData.getExpectedReturnAt())
        ));
    }

    private void refreshCheckoutList() {
        List<CheckoutAssetItem> orderedItems = checkoutController.buildOrderedCheckoutItems();
        CheckoutController.CheckoutSummary summary = checkoutController.buildCheckoutSummary();
        uiRenderer.renderCheckoutList(
                orderedItems,
                summary,
                getString(R.string.checkout_count_value, orderedItems.size())
        );
    }

    private void refreshCheckinList() {
        List<CheckInResultItem> orderedItems = checkoutController.buildOrderedCheckinItems();
        CheckoutController.CheckinSummary summary = checkoutController.buildCheckinSummary();
        if (summary == null || summary.getTotalCount() <= 0) {
            uiRenderer.renderCheckinList(
                    orderedItems,
                    summary,
                    getString(R.string.checkin_summary_headline, 0, 0),
                    getString(R.string.checkin_summary_detail_empty),
                    0
            );
            return;
        }

        int completionPercent = Math.min(100, Math.max(0, Math.round((summary.getReturnedCount() * 100f) / summary.getTotalCount())));
        uiRenderer.renderCheckinList(
                orderedItems,
                summary,
                getString(R.string.checkin_summary_headline, summary.getReturnedCount(), summary.getTotalCount()),
                getString(R.string.checkin_summary_detail, completionPercent, summary.getMissingCount()),
                completionPercent
        );
    }

    private void updateScannerStatuses() {
        uiRenderer.renderScannerStatuses(
                buildScannerStatusText(ScreenTab.CHECKOUT, rbCheckoutQr.isChecked(), swCheckoutContinuousScan.isChecked()),
                buildScannerStatusText(ScreenTab.CHECKIN, rbCheckinQr.isChecked(), swCheckinContinuousScan.isChecked())
        );
    }

    private String buildScannerStatusText(ScreenTab tab, boolean qrSelected, boolean continuousEnabled) {
        if (checkoutScannerController.isScannerPreparing() && checkoutScannerController.getScannerPreparingTab() == tab) {
            return getString(R.string.checkout_scanner_preparing);
        }
        if (qrSelected) {
            return checkoutScannerController.isQrScanning()
                    ? getString(R.string.checkout_scanner_qr_running)
                    : (checkoutScannerController.isBarcodeReady()
                    ? getString(R.string.checkout_scanner_qr_ready)
                    : getString(R.string.checkout_scanner_qr_lazy));
        }
        if (checkoutScannerController.isReaderInventoryRunning()) {
            return getString(R.string.checkout_scanner_rfid_running);
        }
        if (!checkoutScannerController.isReaderReady()) {
            return continuousEnabled
                    ? getString(R.string.checkout_scanner_rfid_lazy_continuous)
                    : getString(R.string.checkout_scanner_rfid_lazy_single);
        }
        return continuousEnabled
                ? getString(R.string.checkout_scanner_rfid_ready_continuous)
                : getString(R.string.checkout_scanner_rfid_ready_single);
    }

    private void updateButtons() {
        boolean checkoutItemsAvailable = !checkoutController.getState().getCheckoutItems().isEmpty();
        boolean hasImportedTicket = checkoutController.hasImportedCheckoutData();
        boolean hasCheckinData = hasImportedTicket && !checkoutController.getState().getExpectedCheckinItems().isEmpty();
        boolean scannerRunning = checkoutScannerController.isQrScanning() || checkoutScannerController.isReaderInventoryRunning();
        boolean scannerPreparing = checkoutScannerController.isScannerPreparing();
        uiRenderer.renderButtons(new CheckoutUiRenderer.ButtonsState(
                !scannerPreparing,
                !scannerPreparing && scannerRunning,
                checkoutItemsAvailable,
                checkoutItemsAvailable,
                hasImportedTicket && !scannerPreparing,
                hasImportedTicket && !scannerPreparing && scannerRunning,
                hasImportedTicket,
                hasCheckinData
        ));
    }

    private boolean isCurrentModeQr() {
        return activeTab == ScreenTab.CHECKOUT ? rbCheckoutQr.isChecked() : rbCheckinQr.isChecked();
    }

    private boolean isCurrentRfidContinuousMode() {
        if (activeTab == ScreenTab.CHECKOUT) {
            return rbCheckoutRfid.isChecked() && swCheckoutContinuousScan.isChecked();
        }
        return rbCheckinRfid.isChecked() && swCheckinContinuousScan.isChecked();
    }

    private void stopAllScanning() {
        checkoutScannerController.stopAllScanning();
        updateScannerStatuses();
        updateButtons();
    }

    private void openCheckoutCsvPicker() {
        logRepository.logInfo("IMPORT_FILE", "Mo bo chon file check out de import");
        importCheckoutLauncher.launch(new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/*",
                "*/*"
        });
    }

    private void onCheckoutCsvSelected(Uri uri) {
        if (uri == null) {
            logRepository.logInfo("IMPORT_FILE", "Da huy import file check out");
            showToast(getString(R.string.checkin_import_cancelled));
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        ioExecutor.execute(() -> {
            try {
                ImportedCheckoutData importedData = csvRepository.importCheckout(this, uri);
                mainHandler.post(() -> {
                    applyImportedCheckoutData(importedData);
                    logRepository.logInfo(
                            "IMPORT_FILE",
                            "Da import file check out",
                            importedData.getSourceFileName()
                    );
                    showToast(getString(
                            R.string.checkin_import_status_done,
                            importedData.getExpectedItems().size(),
                            importedData.getSourceFileName()
                    ));
                });
            } catch (IOException exception) {
                mainHandler.post(() -> {
                    logRepository.logError("ERROR", "Import file check out that bai", exception.getMessage());
                    showToast(getString(R.string.checkin_import_failed));
                });
            }
        });
    }

    private void applyImportedCheckoutData(ImportedCheckoutData importedData) {
        checkoutController.applyImportedCheckoutData(importedData);
        refreshAllViews();
        setActiveTab(ScreenTab.CHECKIN);
    }

    private void clearCheckoutItems() {
        cancelScheduledListRefreshes();
        checkoutController.clearCheckoutItems();
        refreshCheckoutList();
        updateButtons();
    }

    private void removeCheckoutItem(CheckoutAssetItem item) {
        if (!checkoutController.removeCheckoutItem(item)) {
            return;
        }
        cancelScheduledListRefreshes();
        refreshCheckoutList();
        updateButtons();
        showToast(getString(R.string.checkout_removed_item));
    }

    private void clearCheckinSession() {
        if (!checkoutController.hasImportedCheckoutData()) {
            showToast(getString(R.string.checkin_need_import_first));
            return;
        }
        cancelScheduledListRefreshes();
        checkoutController.clearCheckinSession();
        refreshAllViews();
        showToast(getString(R.string.checkin_reset_done));
    }

    private void exportCheckoutCsv() {
        if (checkoutController.getState().getCheckoutItems().isEmpty()) {
            showToast(getString(R.string.checkout_export_empty));
            return;
        }
        CheckoutController.ValidationResult validationResult = validateCheckoutForm();
        if (!validationResult.isValid()) {
            showToast(getString(checkoutValidationMessageRes(validationResult.getField())));
            return;
        }
        if (!ensureExportStoragePermission()) {
            return;
        }

        CheckoutDraft draft = buildCheckoutDraft();
        CheckOutFormData formData = checkoutController.buildCheckoutFormData(draft);
        List<CheckoutAssetItem> exportItems = checkoutController.buildOrderedCheckoutItems();
        ioExecutor.execute(() -> {
            try {
                File exportFile = csvRepository.exportCheckout(this, formData, exportItems);
                mainHandler.post(() -> {
                    logRepository.logInfo("EXPORT_FILE", "Da export file check out", exportFile.getAbsolutePath());
                    logRepository.logInfo(
                            "CHECKOUT",
                            "Da tao phieu check out",
                            formData.getTicketId() + " | " + exportItems.size() + " tai san"
                    );
                    showToast(getString(R.string.checkout_export_done, exportFile.getAbsolutePath()));
                });
            } catch (IOException exception) {
                mainHandler.post(() -> {
                    logRepository.logError("ERROR", "Export file check out that bai", exception.getMessage());
                    showToast(getString(R.string.checkout_export_failed));
                });
            }
        });
    }

    private void exportCheckinCsv() {
        ImportedCheckoutData importedCheckoutData = checkoutController.getImportedCheckoutData();
        if (importedCheckoutData == null) {
            showToast(getString(R.string.checkin_need_import_first));
            return;
        }
        List<CheckInResultItem> exportItems = checkoutController.buildOrderedCheckinItems();
        if (exportItems.isEmpty()) {
            showToast(getString(R.string.checkin_export_empty));
            return;
        }
        if (!ensureExportStoragePermission()) {
            return;
        }

        ioExecutor.execute(() -> {
            try {
                File exportFile = csvRepository.exportCheckIn(this, importedCheckoutData, exportItems);
                mainHandler.post(() -> {
                    logRepository.logInfo("EXPORT_FILE", "Da export file check in", exportFile.getAbsolutePath());
                    logRepository.logInfo(
                            "CHECKIN",
                            "Da export ket qua check in",
                            valueOrDash(importedCheckoutData.getSourceFileName())
                    );
                    showToast(getString(R.string.checkin_export_done, exportFile.getAbsolutePath()));
                });
            } catch (IOException exception) {
                mainHandler.post(() -> {
                    logRepository.logError("ERROR", "Export file check in that bai", exception.getMessage());
                    showToast(getString(R.string.checkin_export_failed));
                });
            }
        });
    }

    private CheckoutController.ValidationResult validateCheckoutForm() {
        clearCheckoutFormErrors();
        CheckoutController.ValidationResult result = checkoutController.validateCheckoutDraft(buildCheckoutDraft());
        if (!result.isValid()) {
            setCheckoutFieldError(fieldForValidation(result.getField()), checkoutValidationMessageRes(result.getField()));
        }
        return result;
    }

    private void clearCheckoutFormErrors() {
        EditText[] checkoutFields = new EditText[]{
                etCheckoutCarrierName,
                etCheckoutDepartment,
                etCheckoutPurpose,
                etCheckoutEvent,
                etCheckoutTime,
                etCheckoutExpectedReturn,
                etCheckoutApprover,
                etCheckoutNote
        };
        for (EditText field : checkoutFields) {
            if (field != null) {
                field.setError(null);
            }
        }
    }

    private String setCheckoutFieldError(EditText field, int messageRes) {
        String message = getString(messageRes);
        if (field != null) {
            field.setError(message);
            field.requestFocus();
        }
        return message;
    }

    private int checkoutValidationMessageRes(CheckoutController.ValidationResult.Field field) {
        if (field == null) {
            return R.string.common_unknown_value;
        }
        switch (field) {
            case CARRIER_NAME:
                return R.string.checkout_need_carrier;
            case DEPARTMENT:
                return R.string.checkout_need_department;
            case PURPOSE:
                return R.string.checkout_need_purpose;
            case EVENT:
                return R.string.checkout_need_event;
            case CHECKOUT_TIME:
                return R.string.checkout_need_checkout_time;
            case EXPECTED_RETURN:
                return R.string.checkout_need_expected_return;
            case APPROVER:
                return R.string.checkout_need_approver;
            case NONE:
            default:
                return R.string.common_unknown_value;
        }
    }

    private EditText fieldForValidation(CheckoutController.ValidationResult.Field field) {
        if (field == null) {
            return null;
        }
        switch (field) {
            case CARRIER_NAME:
                return etCheckoutCarrierName;
            case DEPARTMENT:
                return etCheckoutDepartment;
            case PURPOSE:
                return etCheckoutPurpose;
            case EVENT:
                return etCheckoutEvent;
            case CHECKOUT_TIME:
                return etCheckoutTime;
            case EXPECTED_RETURN:
                return etCheckoutExpectedReturn;
            case APPROVER:
                return etCheckoutApprover;
            case NONE:
            default:
                return null;
        }
    }

    private CheckoutDraft buildCheckoutDraft() {
        return new CheckoutDraft(
                textOf(etCheckoutCarrierName),
                textOf(etCheckoutDepartment),
                textOf(etCheckoutPurpose),
                textOf(etCheckoutEvent),
                textOf(etCheckoutTime),
                textOf(etCheckoutExpectedReturn),
                textOf(etCheckoutApprover),
                textOf(etCheckoutNote)
        );
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

    private void startCheckoutScan() {
        if (checkoutScannerController.isScannerPreparing()) {
            return;
        }
        if (!ensureCheckoutCacheReadyForScan()) {
            return;
        }
        logRepository.logInfo("START_SCAN", "Bat dau scan Check Out", describeScannerMode(ScreenTab.CHECKOUT));
        if (rbCheckoutQr.isChecked()) {
            checkoutScannerController.handleTriggerDown(this, ScreenTab.CHECKOUT, true, false);
        } else {
            if (swCheckoutContinuousScan.isChecked()) {
                checkoutScannerController.handleTriggerDown(this, ScreenTab.CHECKOUT, false, false);
            } else {
                checkoutScannerController.handleTriggerDown(this, ScreenTab.CHECKOUT, false, true);
            }
        }
    }

    private void startCheckinScan() {
        if (checkoutScannerController.isScannerPreparing()) {
            return;
        }
        if (!checkoutController.hasImportedCheckoutData()) {
            showToast(getString(R.string.checkin_need_import_first));
            return;
        }
        logRepository.logInfo("START_SCAN", "Bat dau scan Check In", describeScannerMode(ScreenTab.CHECKIN));
        if (rbCheckinQr.isChecked()) {
            checkoutScannerController.handleTriggerDown(this, ScreenTab.CHECKIN, true, false);
        } else {
            if (swCheckinContinuousScan.isChecked()) {
                checkoutScannerController.handleTriggerDown(this, ScreenTab.CHECKIN, false, false);
            } else {
                checkoutScannerController.handleTriggerDown(this, ScreenTab.CHECKIN, false, true);
            }
        }
    }

    private void processCheckoutQr(String code, long timestamp) {
        CheckoutController.ScanOutcome outcome = checkoutController.processCheckoutQr(code, timestamp);
        handleCheckoutScanOutcome(outcome, false, false);
    }

    private void processCheckinQr(String code, long timestamp) {
        CheckoutController.ScanOutcome outcome = checkoutController.processCheckinQr(code, timestamp);
        handleCheckinScanOutcome(outcome, false, false);
    }

    private void processCheckoutRfid(CheckoutScannerController.RfidReadResult result, boolean suppressDuplicateToast) {
        CheckoutController.ScanOutcome outcome = checkoutController.processCheckoutRfid(
                result.getTid(),
                result.getCode(),
                result.getScannedAt(),
                suppressDuplicateToast
        );
        handleCheckoutScanOutcome(outcome, true, suppressDuplicateToast);
    }

    private void processCheckinRfid(CheckoutScannerController.RfidReadResult result, boolean suppressDuplicateToast) {
        CheckoutController.ScanOutcome outcome = checkoutController.processCheckinRfid(
                result.getTid(),
                result.getCode(),
                result.getScannedAt(),
                suppressDuplicateToast
        );
        handleCheckinScanOutcome(outcome, true, suppressDuplicateToast);
    }

    private void handleCheckoutScanOutcome(CheckoutController.ScanOutcome outcome,
                                           boolean rfid,
                                           boolean suppressDuplicateToast) {
        if (outcome == null) {
            return;
        }
        String displayValue = valueOrDash(outcome.getDisplayValue());
        switch (outcome.getType()) {
            case NEED_IDENTIFIER:
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkout_need_identifier));
                }
                return;
            case CHECKOUT_DUPLICATE:
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkout_duplicate_item));
                }
                return;
            case CHECKOUT_ADDED:
                tvCheckoutScannerStatus.setText(getString(
                        rfid ? R.string.checkout_scanner_rfid_done : R.string.checkout_scanner_qr_done,
                        displayValue
                ));
                scheduleCheckoutListRefresh();
                updateButtons();
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkout_added_item));
                }
                return;
            case NONE:
            case CHECKIN_RETURNED:
            case CHECKIN_DUPLICATE_RETURNED:
            case CHECKIN_NOT_IN_TICKET:
            default:
                return;
        }
    }

    private void handleCheckinScanOutcome(CheckoutController.ScanOutcome outcome,
                                          boolean rfid,
                                          boolean suppressDuplicateToast) {
        if (outcome == null) {
            return;
        }
        String displayValue = valueOrDash(outcome.getDisplayValue());
        switch (outcome.getType()) {
            case NEED_IDENTIFIER:
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkout_need_identifier));
                }
                return;
            case CHECKIN_DUPLICATE_RETURNED:
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkin_duplicate_returned));
                }
                return;
            case CHECKIN_RETURNED:
                tvCheckinScannerStatus.setText(getString(
                        rfid ? R.string.checkout_scanner_rfid_done : R.string.checkout_scanner_qr_done,
                        displayValue
                ));
                scheduleCheckinListRefresh();
                updateButtons();
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkin_marked_returned));
                }
                return;
            case CHECKIN_NOT_IN_TICKET:
                tvCheckinScannerStatus.setText(getString(
                        rfid ? R.string.checkout_scanner_rfid_done : R.string.checkout_scanner_qr_done,
                        displayValue
                ));
                if (!suppressDuplicateToast) {
                    showToast(getString(R.string.checkin_not_in_ticket));
                }
                return;
            case NONE:
            case CHECKOUT_ADDED:
            case CHECKOUT_DUPLICATE:
            default:
                return;
        }
    }

    private String textOf(TextView view) {
        return view == null || view.getText() == null ? "" : view.getText().toString().trim();
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? getString(R.string.common_unknown_value) : value;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void scheduleCheckoutListRefresh() {
        if (checkoutRefreshScheduled) {
            return;
        }
        checkoutRefreshScheduled = true;
        mainHandler.postDelayed(checkoutRefreshRunnable, SCAN_REFRESH_DEBOUNCE_MS);
    }

    private void scheduleCheckinListRefresh() {
        if (checkinRefreshScheduled) {
            return;
        }
        checkinRefreshScheduled = true;
        mainHandler.postDelayed(checkinRefreshRunnable, SCAN_REFRESH_DEBOUNCE_MS);
    }

    private void cancelScheduledListRefreshes() {
        checkoutRefreshScheduled = false;
        checkinRefreshScheduled = false;
        mainHandler.removeCallbacks(checkoutRefreshRunnable);
        mainHandler.removeCallbacks(checkinRefreshRunnable);
    }

    private void runScheduledCheckoutRefresh() {
        checkoutRefreshScheduled = false;
        refreshCheckoutList();
    }

    private void runScheduledCheckinRefresh() {
        checkinRefreshScheduled = false;
        refreshCheckinList();
    }

    private boolean ensureCheckoutCacheReadyForScan() {
        if (initialCacheResolved) {
            return true;
        }
        DebugEventLogger.info(
                logRepository,
                SCREEN,
                FLOW_CACHE_LOAD,
                "cache_not_ready",
                "flow=checkout_scan"
        );
        showToast("Cache tai san dang khoi tao. Vui long thu lai sau.");
        return false;
    }

    private String describeScannerMode(ScreenTab tab) {
        boolean qrSelected = tab == ScreenTab.CHECKIN ? rbCheckinQr.isChecked() : rbCheckoutQr.isChecked();
        boolean continuous = tab == ScreenTab.CHECKIN ? swCheckinContinuousScan.isChecked() : swCheckoutContinuousScan.isChecked();
        if (qrSelected) {
            return "QR";
        }
        return continuous ? "RFID lien tuc" : "RFID 1 lan";
    }

    private static final class RetainedState {
        final ScreenTab activeTab;
        final CheckoutState.Snapshot snapshot;

        RetainedState(ScreenTab activeTab, CheckoutState.Snapshot snapshot) {
            this.activeTab = activeTab;
            this.snapshot = snapshot;
        }
    }
}
