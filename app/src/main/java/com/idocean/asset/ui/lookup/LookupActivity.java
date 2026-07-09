package com.idocean.asset.ui.lookup;

import android.os.Bundle;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.AppFailureReporter;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.model.Asset;
import com.idocean.asset.scanner.rfid.ScannerTriggerHandler;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;
import com.idocean.asset.utils.HardwareKeyUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LookupActivity extends AppCompatActivity implements ScannerTriggerHandler, LookupScannerCallback, LookupController.LookupUi {
    private static final String SCREEN = "Lookup";
    private static final String FLOW_SCREEN_OPEN = "screen_open";
    private static final String FLOW_CACHE_LOAD = "cache_load";

    public static final String EXTRA_ASSET_CODE = "lookup_asset_code";
    public static final String EXTRA_ASSET_TID = "lookup_asset_tid";
    private static final String STATE_CURRENT_ASSET_CODE = "lookup_state_asset_code";
    private static final String STATE_CURRENT_ASSET_TID = "lookup_state_asset_tid";
    private static final String STATE_EDITING = "lookup_state_editing";
    private final SimpleDateFormat tagDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final AssetRepository assetRepository = AssetRepository.getInstance();
    private final LogRepository logRepository = LogRepository.getInstance();
    private final LookupController lookupController = new LookupController(assetRepository, logRepository);
    private QrScannerController qrScannerController;
    private RfidScannerController rfidScannerController;
    private LookupUiRenderer uiRenderer;

    private TextView tvLookupStatus;
    private TextView tvLookupScannerStatus;
    private RadioGroup rgLookupScannerType;
    private RadioButton rbLookupRfid;
    private RadioButton rbLookupQr;
    private Button btnLookupScan;
    private Button btnLookupStop;
    private Button btnLookupEdit;
    private Button btnLookupCancel;
    private Button btnLookupSave;
    private Button btnLookupHandover;
    private Button btnLookupManualAdd;

    private EditText etLookupCode;
    private EditText etLookupTid;
    private EditText etLookupOldCode;
    private EditText etLookupOldSerial;
    private EditText etLookupName;
    private MaterialAutoCompleteTextView etLookupType;
    private EditText etLookupSerial;
    private MaterialAutoCompleteTextView etLookupDepartment;
    private EditText etLookupUser;
    private MaterialAutoCompleteTextView etLookupLocation;
    private MaterialAutoCompleteTextView etLookupInventoryStatus;
    private EditText etLookupNote;
    private EditText[] editableFields;

    private boolean scannerPreparing;
    private boolean activityStarted;
    private AssetRepository.CacheSnapshot cacheSnapshot;
    private Bundle deferredSavedState;
    private PerfLogger.Trace screenOpenTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        screenOpenTrace = PerfLogger.start(SCREEN, FLOW_SCREEN_OPEN, "onCreate", "activity=LookupActivity");
        screenOpenTrace.markStart(logRepository);
        setContentView(R.layout.activity_ido_lookup);

        MaterialToolbar toolbar = findViewById(R.id.toolbarIdoLookup);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.lookup_title_screen);
        toolbar.setNavigationOnClickListener(v -> finish());

        initScannerControllers();
        bindViews();
        uiRenderer = new LookupUiRenderer(
                tvLookupStatus,
                tvLookupScannerStatus,
                btnLookupScan,
                btnLookupStop,
                btnLookupManualAdd,
                btnLookupEdit,
                btnLookupCancel,
                btnLookupSave,
                btnLookupHandover,
                etLookupCode,
                etLookupTid,
                etLookupOldCode,
                etLookupOldSerial,
                etLookupName,
                etLookupType,
                etLookupSerial,
                etLookupDepartment,
                etLookupUser,
                etLookupLocation,
                etLookupInventoryStatus,
                etLookupNote,
                editableFields
        );
        bindEditableDropdowns();
        setupControls();
        applyDefaultScannerMode();
        uiRenderer.renderEditMode(false);
        updateButtons();
        updateScannerStatus();
        deferredSavedState = savedInstanceState;
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
        activityStarted = true;
        if (qrScannerController != null) {
            qrScannerController.onStart();
        }
        if (rfidScannerController != null) {
            rfidScannerController.onStart();
        }
        bindEditableDropdowns();
        updateScannerStatus();
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        if (qrScannerController != null) {
            qrScannerController.onStop();
        }
        if (rfidScannerController != null) {
            rfidScannerController.onStop();
        }
        setScannerPreparing(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (qrScannerController != null) {
            qrScannerController.shutdown();
        }
        if (rfidScannerController != null) {
            rfidScannerController.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Asset currentAsset = lookupController.getState().getCurrentAsset();
        outState.putString(STATE_CURRENT_ASSET_CODE, currentAsset == null ? "" : valueOrEmpty(currentAsset.getAssetCode()));
        outState.putString(STATE_CURRENT_ASSET_TID, currentAsset == null ? "" : valueOrEmpty(currentAsset.getTid()));
        outState.putBoolean(STATE_EDITING, lookupController.getState().isEditing());
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
        startLookupScan();
    }

    @Override
    public void onScannerTriggerUp() {
    }

    private void bindViews() {
        tvLookupStatus = findViewById(R.id.tvLookupStatus);
        tvLookupScannerStatus = findViewById(R.id.tvLookupScannerStatus);
        rgLookupScannerType = findViewById(R.id.rgLookupScannerType);
        rbLookupRfid = findViewById(R.id.rbLookupRfid);
        rbLookupQr = findViewById(R.id.rbLookupQr);
        btnLookupScan = findViewById(R.id.btnLookupScan);
        btnLookupStop = findViewById(R.id.btnLookupStop);
        btnLookupEdit = findViewById(R.id.btnLookupEdit);
        btnLookupCancel = findViewById(R.id.btnLookupCancel);
        btnLookupSave = findViewById(R.id.btnLookupSave);
        btnLookupHandover = findViewById(R.id.btnLookupHandover);
        btnLookupManualAdd = findViewById(R.id.btnLookupManualAdd);

        etLookupCode = findViewById(R.id.etLookupCode);
        etLookupTid = findViewById(R.id.etLookupTid);
        etLookupOldCode = findViewById(R.id.etLookupOldCode);
        etLookupOldSerial = findViewById(R.id.etLookupOldSerial);
        etLookupName = findViewById(R.id.etLookupName);
        etLookupType = findViewById(R.id.etLookupType);
        etLookupSerial = findViewById(R.id.etLookupSerial);
        etLookupDepartment = findViewById(R.id.etLookupDepartment);
        etLookupUser = findViewById(R.id.etLookupUser);
        etLookupLocation = findViewById(R.id.etLookupLocation);
        etLookupInventoryStatus = findViewById(R.id.etLookupInventoryStatus);
        etLookupNote = findViewById(R.id.etLookupNote);

        editableFields = new EditText[]{
                etLookupOldCode,
                etLookupOldSerial,
                etLookupName,
                etLookupType,
                etLookupSerial,
                etLookupDepartment,
                etLookupUser,
                etLookupLocation,
                etLookupInventoryStatus,
                etLookupNote
        };
    }

    private void setupControls() {
        rgLookupScannerType.setOnCheckedChangeListener((group, checkedId) -> {
            stopQrScan(false);
            updateScannerStatus();
        });
        btnLookupScan.setOnClickListener(v -> startLookupScan());
        btnLookupStop.setOnClickListener(v -> stopQrScan(true));
        btnLookupManualAdd.setOnClickListener(v -> startManualAdd());
        btnLookupEdit.setOnClickListener(v -> startEditMode());
        btnLookupCancel.setOnClickListener(v -> cancelEditMode());
        btnLookupSave.setOnClickListener(v -> saveAssetChanges());
        btnLookupHandover.setOnClickListener(v -> openHandoverDialog());
    }

    private void initScannerControllers() {
        qrScannerController = new QrScannerController(this);
        rfidScannerController = new RfidScannerController(this);
    }

    private void applyDefaultScannerMode() {
        if (rgLookupScannerType == null || rbLookupQr == null) {
            return;
        }
        if (rgLookupScannerType.getCheckedRadioButtonId() != R.id.rbLookupQr) {
            rgLookupScannerType.check(R.id.rbLookupQr);
        }
        rbLookupQr.post(() -> rbLookupQr.requestFocus());
    }

    private void bindEditableDropdowns() {
        uiRenderer.bindDropdown(etLookupType, buildAssetTypeOptions());
        uiRenderer.bindDropdown(etLookupDepartment, buildDepartmentOptions());
        uiRenderer.bindDropdown(etLookupLocation, buildLocationOptions());
        uiRenderer.bindDropdown(etLookupInventoryStatus, buildInventoryStatusOptions());
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
                bindEditableDropdowns();
                if (deferredSavedState != null) {
                    restoreScreenState(deferredSavedState);
                    deferredSavedState = null;
                } else {
                    openAssetFromIntentIfNeeded();
                }
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

    private List<String> buildAssetTypeOptions() {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_asset_type_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeAssetTypeForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }
        List<String> runtimeValues = cacheSnapshot == null ? new ArrayList<>() : cacheSnapshot.getDistinctValues("assetType");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetFieldNormalizer.normalizeAssetTypeForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }
        return new ArrayList<>(options);
    }

    private List<String> buildDepartmentOptions() {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_department_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }
        List<String> runtimeValues = cacheSnapshot == null ? new ArrayList<>() : cacheSnapshot.getDistinctValues("department");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }
        return new ArrayList<>(options);
    }

    private List<String> buildLocationOptions() {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_location_options);
        for (String option : defaults) {
            String normalized = AssetLocationUtils.normalizeLocationForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }
        List<String> runtimeValues = cacheSnapshot == null ? new ArrayList<>() : cacheSnapshot.getDistinctValues("location");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetLocationUtils.normalizeLocationForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }
        return new ArrayList<>(options);
    }

    private List<String> buildInventoryStatusOptions() {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_inventory_status_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeInventoryStatusForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }
        List<String> runtimeValues = cacheSnapshot == null ? new ArrayList<>() : cacheSnapshot.getDistinctValues("inventoryStatus");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetFieldNormalizer.normalizeInventoryStatusForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }
        return new ArrayList<>(options);
    }

    private void openAssetFromIntentIfNeeded() {
        String assetCode = getIntent() == null ? "" : getIntent().getStringExtra(EXTRA_ASSET_CODE);
        String assetTid = getIntent() == null ? "" : getIntent().getStringExtra(EXTRA_ASSET_TID);
        lookupController.openAssetFromIntent(assetCode, assetTid, this);
    }

    private void startLookupScan() {
        if (scannerPreparing || lookupController.getState().isSaving()) {
            return;
        }

        String scannerType = rbLookupQr.isChecked() ? "QR" : "RFID";
        logRepository.logInfo("START_SCAN", "Bat dau scan tra cuu tai san", scannerType);
        if (rbLookupQr.isChecked()) {
            startQrScan();
        } else {
            runSingleRfidScan();
        }
        updateScannerStatus();
    }

    private void startQrScan() {
        if (qrScannerController == null) {
            return;
        }
        if (!qrScannerController.isReady() && !qrScannerController.isScanning()) {
            setScannerPreparing(true);
        }
        qrScannerController.startScan(getApplicationContext());
    }

    private void runSingleRfidScan() {
        if (rfidScannerController == null) {
            return;
        }
        setScannerPreparing(true);
        rfidScannerController.scanSingle(getApplicationContext());
    }

    private void startEditMode() {
        lookupController.startEdit(this);
    }

    private void startManualAdd() {
        if (lookupController != null) {
            lookupController.startManualAdd(this);
            updateButtons();
        }
    }

    private void cancelEditMode() {
        lookupController.cancelEdit(this);
    }

    private void saveAssetChanges() {
        if (!lookupController.getState().isEditing() || lookupController.getState().isSaving()) {
            return;
        }

        clearEditableFieldErrors();
        EditableAssetDraft draft = new EditableAssetDraft(
                textOf(etLookupCode),
                textOf(etLookupTid),
                textOf(etLookupOldCode),
                textOf(etLookupOldSerial),
                textOf(etLookupName),
                textOf(etLookupType),
                textOf(etLookupSerial),
                textOf(etLookupDepartment),
                textOf(etLookupUser),
                textOf(etLookupLocation),
                textOf(etLookupInventoryStatus),
                textOf(etLookupNote)
        );
        LookupController.ValidationResult validation = lookupController.validateEditableDraft(draft, this);
        if (!validation.isValid()) {
            if (validation.getField() == LookupController.ValidationResult.Field.ASSET_CODE) {
                setFieldError(etLookupCode, validation.getMessage());
            } else if (validation.getField() == LookupController.ValidationResult.Field.ASSET_NAME) {
                setFieldError(etLookupName, validation.getMessage());
            }
            showToast(validation.getMessage());
            tvLookupStatus.setText(getString(R.string.lookup_status_update_failed, validation.getMessage()));
            return;
        }
        lookupController.saveEditableAsset(draft, this);
    }

    private void openHandoverDialog() {
        final Asset sourceAsset = lookupController.getState().getCurrentAsset();
        if (sourceAsset == null) {
            showToast(getString(R.string.lookup_need_asset_first));
            return;
        }
        if (lookupController.getState().isSaving()) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lookup_handover, null, false);
        TextView tvAssetName = dialogView.findViewById(R.id.tvHandoverAssetName);
        TextView tvAssetCode = dialogView.findViewById(R.id.tvHandoverAssetCode);
        TextView tvAssetSummary = dialogView.findViewById(R.id.tvHandoverAssetSummary);
        TextInputEditText etHandoverUser = dialogView.findViewById(R.id.etHandoverUser);
        MaterialAutoCompleteTextView etHandoverDepartment = dialogView.findViewById(R.id.etHandoverDepartment);
        MaterialAutoCompleteTextView etHandoverLocation = dialogView.findViewById(R.id.etHandoverLocation);
        TextInputEditText etHandoverDate = dialogView.findViewById(R.id.etHandoverDate);

        tvAssetName.setText(valueOrDash(sourceAsset.getAssetName()));
        tvAssetCode.setText("Code: " + valueOrDash(sourceAsset.getAssetCode()));
        tvAssetSummary.setText(LookupController.buildHandoverCurrentSummary(sourceAsset));
        uiRenderer.bindDropdown(etHandoverDepartment, buildDepartmentOptions());
        uiRenderer.bindDropdown(etHandoverLocation, buildLocationOptions());
        etHandoverUser.setText("");
        etHandoverDepartment.setText(LookupController.normalizeDepartmentForHandover(sourceAsset, sourceAsset.getDepartment()), false);
        etHandoverLocation.setText(LookupController.normalizeLocationForHandover(sourceAsset, sourceAsset.getLocation()), false);
        etHandoverDate.setText(LookupController.todayDateString());
        bindDateInput(etHandoverDate);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lookup_handover_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.lookup_handover_cancel_action, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.lookup_handover_confirm_action, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            clearHandoverFieldErrors(etHandoverUser, etHandoverDate);
            HandoverDraft draft = new HandoverDraft(
                    textOf(etHandoverUser),
                    textOf(etHandoverDepartment),
                    textOf(etHandoverLocation),
                    textOf(etHandoverDate)
            );
            LookupController.ValidationResult validation = lookupController.validateHandoverDraft(draft, this);
            if (!validation.isValid()) {
                if (validation.getField() == LookupController.ValidationResult.Field.HANDOVER_USER) {
                    setFieldError(etHandoverUser, validation.getMessage());
                } else if (validation.getField() == LookupController.ValidationResult.Field.HANDOVER_DATE) {
                    setFieldError(etHandoverDate, validation.getMessage());
                }
                showToast(validation.getMessage());
                return;
            }

            String newDepartment = LookupController.normalizeDepartmentForHandover(sourceAsset, draft.newDepartment);
            String newLocation = LookupController.normalizeLocationForHandover(sourceAsset, draft.newLocation);
            if (!LookupController.hasHandoverChanges(sourceAsset, draft.newUser, newDepartment, newLocation, draft.handoverDate)) {
                showToast(getString(R.string.lookup_handover_no_change));
                return;
            }

            dialog.dismiss();
            lookupController.performHandover(new HandoverDraft(
                    draft.newUser,
                    newDepartment,
                    newLocation,
                    draft.handoverDate
            ), this);
        }));

        dialog.show();
        etHandoverUser.requestFocus();
    }

    private void bindDateInput(EditText targetField) {
        if (targetField == null) {
            return;
        }
        targetField.setFocusable(false);
        targetField.setClickable(true);
        targetField.setLongClickable(false);
        targetField.setCursorVisible(false);
        targetField.setKeyListener(null);
        targetField.setOnClickListener(v -> openDatePicker(targetField));
    }

    private void openDatePicker(EditText targetField) {
        Calendar calendar = Calendar.getInstance();
        long existingDate = LookupController.parseDateMillis(textOf(targetField));
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

                    String formattedDate = LookupController.formatDate(selectedDate.getTimeInMillis());
                    targetField.setText(formattedDate);
                    targetField.setError(null);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void clearHandoverFieldErrors(EditText... fields) {
        if (fields == null) {
            return;
        }
        for (EditText field : fields) {
            if (field != null) {
                field.setError(null);
            }
        }
    }

    private void updateButtons() {
        LookupState state = lookupController.getState();
        uiRenderer.renderButtons(
                state.hasCurrentAsset(),
                state.isEditing(),
                state.isSaving(),
                scannerPreparing,
                qrScannerController != null && qrScannerController.isScanning()
        );
    }

    private void updateScannerStatus() {
        uiRenderer.renderScannerStatus(resolveScannerStatusMessage());
    }

    private void setScannerPreparing(boolean preparing) {
        scannerPreparing = preparing;
        updateButtons();
        updateScannerStatus();
    }

    private void stopQrScan(boolean logStop) {
        boolean wasScanning = qrScannerController != null && qrScannerController.stopScan();
        if (wasScanning && logStop) {
            logRepository.logInfo("STOP_SCAN", "Dung scanner tra cuu", "QR");
        }
        updateButtons();
        updateScannerStatus();
    }

    private String resolveScannerStatusMessage() {
        if (scannerPreparing) {
            return getString(R.string.lookup_scanner_preparing);
        }
        if (rbLookupQr.isChecked()) {
            if (qrScannerController != null && qrScannerController.isScanning()) {
                return getString(R.string.lookup_scanner_qr_running);
            }
            return getString(
                    qrScannerController != null && qrScannerController.isReady()
                            ? R.string.lookup_scanner_qr_ready
                            : R.string.lookup_scanner_qr_lazy
            );
        }
        return getString(
                rfidScannerController != null && rfidScannerController.isReady()
                        ? R.string.lookup_scanner_rfid_ready
                        : R.string.lookup_scanner_rfid_lazy
        );
    }

    private String textOf(TextView view) {
        return view == null || view.getText() == null ? "" : view.getText().toString().trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void onScannerPreparingChanged(boolean preparing) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || !activityStarted) {
                return;
            }
            setScannerPreparing(preparing);
        });
    }

    @Override
    public void onScanResult(ScanResult result) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || !activityStarted || result == null) {
                return;
            }
            setScannerPreparing(false);
            String matchedBy = result.getScannerType().name();
            String rawValue = result.getRawValue();
            lookupController.handleLookupResult(
                    result.isRfid() ? result.getTid() : "",
                    result.getCode(),
                    matchedBy,
                    rawValue,
                    this
            );
            tvLookupScannerStatus.setText(getString(R.string.lookup_scan_found_by, matchedBy, LookupController.valueOrDash(rawValue)));
        });
    }

    @Override
    public void onScannerError(String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || !activityStarted) {
                return;
            }
            setScannerPreparing(false);
            logRepository.logError("ERROR", "Doc RFID de tra cuu that bai", message);
            showToast(message);
            updateScannerStatus();
        });
    }

    @Override
    public void onQrScannerUnavailable() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || !activityStarted) {
                return;
            }
            setScannerPreparing(false);
            logRepository.logError("ERROR", "Khong mo duoc scanner QR tren man tra cuu");
            showToast(getString(R.string.lookup_scanner_unknown));
            updateScannerStatus();
        });
    }

    @Override
    public void onQrScannerBusy() {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || !activityStarted) {
                return;
            }
            setScannerPreparing(false);
            showToast("Scanner QR dang ban hoac dang cho ket qua.");
            updateScannerStatus();
        });
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void renderAsset(Asset asset) {
        uiRenderer.renderAsset(asset);
        updateButtons();
    }

    @Override
    public void showStatus(String message) {
        uiRenderer.renderStatus(message);
    }

    @Override
    public void renderEditMode(boolean editing) {
        boolean isTidEditable = false;
        if (lookupController != null && lookupController.getState() != null) {
            Asset current = lookupController.getState().getCurrentAsset();
            isTidEditable = current != null
                    && (current.getTid() == null || current.getTid().isEmpty());
        }
        uiRenderer.renderEditMode(editing, isTidEditable);
        updateButtons();
    }

    @Override
    public void renderSaving(boolean saving) {
        updateButtons();
    }

    @Override
    public String lookupNeedAssetFirst() {
        return getString(R.string.lookup_need_asset_first);
    }

    @Override
    public String lookupNeedAssetName() {
        return getString(R.string.lookup_need_asset_name);
    }

    @Override
    public String lookupOpenedFromList() {
        return getString(R.string.lookup_opened_from_list);
    }

    @Override
    public String lookupStatusNotFound() {
        return getString(R.string.lookup_status_not_found);
    }

    @Override
    public String lookupStatusFound(String assetName) {
        return getString(R.string.lookup_status_found, LookupController.valueOrDash(assetName));
    }

    @Override
    public String lookupEditCancelled() {
        return getString(R.string.lookup_edit_cancelled);
    }

    @Override
    public String lookupStatusEditing() {
        return getString(R.string.lookup_status_editing);
    }

    @Override
    public String lookupStatusUpdateFailed(String message) {
        return getString(R.string.lookup_status_update_failed, message);
    }

    @Override
    public String lookupHandoverNeedUser() {
        return getString(R.string.lookup_handover_need_user);
    }

    @Override
    public String lookupHandoverNeedDate() {
        return getString(R.string.lookup_handover_need_date);
    }

    @Override
    public String lookupHandoverInvalidDate() {
        return getString(R.string.lookup_handover_invalid_date);
    }

    @Override
    public String lookupHandoverNoChange() {
        return getString(R.string.lookup_handover_no_change);
    }

    @Override
    public String lookupHandoverSuccess() {
        return getString(R.string.lookup_handover_success);
    }

    @Override
    public String lookupHandoverFailed(String message) {
        return getString(R.string.lookup_handover_failed, message);
    }

    @Override
    public String lookupStatusNotFoundInSystem() {
        return getString(R.string.lookup_status_not_found_in_system);
    }

    @Override
    public String lookupStatusNewScannedTag() {
        return getString(R.string.lookup_status_new_scanned_tag);
    }

    @Override
    public String lookupStatusManualAddTitle() {
        return getString(R.string.lookup_status_manual_add_title);
    }

    @Override
    public String lookupStatusCancelledRegistration() {
        return getString(R.string.lookup_status_cancelled_registration);
    }

    @Override
    public String lookupRequiredAssetCode() {
        return getString(R.string.lookup_required_asset_code);
    }

    private void restoreScreenState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String savedCode = savedInstanceState.getString(STATE_CURRENT_ASSET_CODE, "");
        String savedTid = savedInstanceState.getString(STATE_CURRENT_ASSET_TID, "");
        boolean editing = savedInstanceState.getBoolean(STATE_EDITING, false);
        lookupController.restoreState(savedCode, savedTid, editing, this);
    }

    private void clearEditableFieldErrors() {
        for (EditText field : editableFields) {
            field.setError(null);
        }
    }

    private String setFieldError(EditText field, int messageRes) {
        String message = getString(messageRes);
        if (field != null) {
            field.setError(message);
            field.requestFocus();
        }
        return message;
    }

    private String setFieldError(EditText field, String message) {
        String safeMessage = message == null ? "" : message;
        if (field != null) {
            field.setError(safeMessage);
            field.requestFocus();
        }
        return safeMessage;
    }

}
