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
    private boolean editing;
    private boolean saving;
    private boolean activityStarted;
    private Asset currentAsset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        setContentView(R.layout.activity_ido_lookup);

        MaterialToolbar toolbar = findViewById(R.id.toolbarIdoLookup);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.lookup_title_screen);
        toolbar.setNavigationOnClickListener(v -> finish());

        initScannerControllers();
        bindViews();
        bindEditableDropdowns();
        setupControls();
        applyDefaultScannerMode();
        setEditMode(false);
        updateButtons();
        updateScannerStatus();
        if (savedInstanceState != null) {
            restoreScreenState(savedInstanceState);
        } else {
            openAssetFromIntentIfNeeded();
        }
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
        outState.putString(STATE_CURRENT_ASSET_CODE, currentAsset == null ? "" : valueOrEmpty(currentAsset.getAssetCode()));
        outState.putString(STATE_CURRENT_ASSET_TID, currentAsset == null ? "" : valueOrEmpty(currentAsset.getTid()));
        outState.putBoolean(STATE_EDITING, editing);
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
        bindDropdown(etLookupType, buildAssetTypeOptions());
        bindDropdown(etLookupDepartment, buildDepartmentOptions());
        bindDropdown(etLookupLocation, buildLocationOptions());
        bindDropdown(etLookupInventoryStatus, buildInventoryStatusOptions());
    }

    private void bindDropdown(MaterialAutoCompleteTextView view, List<String> options) {
        if (view == null) {
            return;
        }
        view.setSimpleItems(options.toArray(new String[0]));
        view.setOnClickListener(v -> view.showDropDown());
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                view.showDropDown();
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
        List<String> runtimeValues = assetRepository.collectDistinctValues("assetType");
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
        List<String> runtimeValues = assetRepository.collectDistinctValues("department");
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
        List<String> runtimeValues = assetRepository.collectDistinctValues("location");
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
        List<String> runtimeValues = assetRepository.collectDistinctValues("inventoryStatus");
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
        if (scannerPreparing || saving) {
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

    private void bindAsset(Asset asset, String statusMessage) {
        currentAsset = asset;
        if (statusMessage != null && !statusMessage.trim().isEmpty()) {
            tvLookupStatus.setText(statusMessage);
        }
        if (asset == null) {
            clearForm();
            setEditMode(false);
            updateButtons();
            return;
        }

        etLookupCode.setText(valueOrEmpty(asset.getAssetCode()));
        etLookupTid.setText(valueOrEmpty(asset.getTid()));
        etLookupOldCode.setText(valueOrEmpty(asset.getOldCode()));
        etLookupOldSerial.setText(valueOrEmpty(asset.getOldSerial()));
        etLookupName.setText(valueOrEmpty(asset.getAssetName()));
        etLookupType.setText(AssetFieldNormalizer.normalizeAssetTypeForDisplay(asset.getAssetType()));
        etLookupSerial.setText(valueOrEmpty(asset.getSerialNumber()));
        etLookupDepartment.setText(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset.getDepartment()), false);
        etLookupUser.setText(valueOrEmpty(asset.getAssignedUser()));
        etLookupLocation.setText(AssetLocationUtils.normalizeLocationForDisplay(asset.getLocation()), false);
        etLookupInventoryStatus.setText(AssetFieldNormalizer.normalizeInventoryStatusForDisplay(asset.getInventoryStatus()), false);
        etLookupNote.setText(valueOrEmpty(asset.getNote()));

        setEditMode(false);
        updateButtons();
    }

    private void clearForm() {
        etLookupCode.setText("");
        etLookupTid.setText("");
        etLookupOldCode.setText("");
        etLookupOldSerial.setText("");
        etLookupName.setText("");
        etLookupType.setText("");
        etLookupSerial.setText("");
        etLookupDepartment.setText("");
        etLookupUser.setText("");
        etLookupLocation.setText("");
        etLookupInventoryStatus.setText("", false);
        etLookupNote.setText("");
    }

    private void startEditMode() {
        lookupController.startEdit(this);
    }

    private void cancelEditMode() {
        lookupController.cancelEdit(this);
    }

    private void saveAssetChanges() {
        if (!editing || saving) {
            return;
        }

        clearEditableFieldErrors();
        EditableAssetDraft draft = new EditableAssetDraft(
                textOf(etLookupCode),
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
            if (validation.getField() == LookupController.ValidationResult.Field.ASSET_NAME) {
                setFieldError(etLookupName, validation.getMessage());
            }
            showToast(validation.getMessage());
            tvLookupStatus.setText(getString(R.string.lookup_status_update_failed, validation.getMessage()));
            return;
        }
        lookupController.saveEditableAsset(draft, this);
    }

    private void openHandoverDialog() {
        final Asset sourceAsset = currentAsset;
        if (sourceAsset == null) {
            showToast(getString(R.string.lookup_need_asset_first));
            return;
        }
        if (saving) {
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
        bindDropdown(etHandoverDepartment, buildDepartmentOptions());
        bindDropdown(etHandoverLocation, buildLocationOptions());
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

    private String validateHandoverForm(EditText userField, EditText dateField) {
        if (textOf(userField).isEmpty()) {
            return setFieldError(userField, R.string.lookup_handover_need_user);
        }

        String handoverDate = textOf(dateField);
        if (handoverDate.isEmpty()) {
            return setFieldError(dateField, R.string.lookup_handover_need_date);
        }
        if (!isValidTagDate(handoverDate)) {
            return setFieldError(dateField, R.string.lookup_handover_invalid_date);
        }
        return "";
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

    private boolean hasHandoverChanges(
            Asset sourceAsset,
            String newUser,
            String newDepartment,
            String newLocation,
            String handoverDate
    ) {
        String currentUser = normalizeSearch(sourceAsset == null ? "" : sourceAsset.getAssignedUser());
        String currentDepartment = normalizeSearch(AssetFieldNormalizer.normalizeDepartmentForDisplay(
                sourceAsset == null ? "" : sourceAsset.getDepartment()
        ));
        String currentLocation = normalizeSearch(AssetLocationUtils.normalizeLocationForDisplay(
                sourceAsset == null ? "" : sourceAsset.getLocation()
        ));
        String nextUser = normalizeSearch(newUser);
        String nextDepartment = normalizeSearch(newDepartment);
        String nextLocation = normalizeSearch(newLocation);
        return !nextUser.equals(currentUser)
                || !nextDepartment.equals(currentDepartment)
                || !nextLocation.equals(currentLocation)
                || !normalizeSearch(handoverDate).equals(normalizeSearch(todayDateString()));
    }

    private String normalizeDepartmentForHandover(Asset asset, String value) {
        String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(value);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return AssetFieldNormalizer.normalizeDepartmentForDisplay(asset == null ? "" : asset.getDepartment());
    }

    private String normalizeLocationForHandover(Asset asset, String value) {
        String normalized = AssetLocationUtils.normalizeLocationForDisplay(value);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return AssetLocationUtils.normalizeLocationForDisplay(asset == null ? "" : asset.getLocation());
    }

    private String sanitizeNoteForMasterAsset(String note) {
        String safeNote = valueOrEmpty(note);
        if (safeNote.isEmpty()) {
            return "";
        }

        String[] lines = safeNote.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (looksLikeLegacyHandoverTrail(trimmed)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }
        return builder.toString().trim();
    }

    private boolean looksLikeLegacyHandoverTrail(String line) {
        String normalized = valueOrEmpty(line);
        if (normalized.isEmpty()) {
            return false;
        }
        return (normalized.startsWith("[Bàn giao ") || normalized.startsWith("[BÃ n giao "))
                && (normalized.contains("Người nhận") || normalized.contains("NgÆ°á»i nháº­n"));
    }

    private String mergeHandoverNote(Asset asset, String newUser, String newDepartment, String newLocation, String handoverDate, String handoverNote) {
        StringBuilder builder = new StringBuilder();
        String currentNote = asset == null ? "" : valueOrEmpty(asset.getNote());
        String summary = buildHandoverTrail(asset, newUser, newDepartment, newLocation, handoverDate, handoverNote);
        if (!currentNote.isEmpty()) {
            builder.append(currentNote).append('\n');
        }
        builder.append(summary);
        return builder.toString().trim();
    }

    private String buildHandoverTrail(Asset asset, String newUser, String newDepartment, String newLocation, String handoverDate, String handoverNote) {
        String oldUser = valueOrDash(asset == null ? "" : asset.getAssignedUser());
        String oldDepartment = valueOrDash(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset == null ? "" : asset.getDepartment()));
        String oldLocation = valueOrDash(AssetLocationUtils.normalizeLocationForDisplay(asset == null ? "" : asset.getLocation()));
        StringBuilder builder = new StringBuilder();
        builder.append("[Bàn giao ").append(valueOrDash(handoverDate)).append("] ");
        builder.append("Người nhận: ").append(valueOrDash(newUser));
        builder.append(" | Từ người dùng: ").append(oldUser);
        builder.append(" | Phòng ban: ").append(oldDepartment).append(" -> ").append(valueOrDash(newDepartment));
        builder.append(" | Vị trí: ").append(oldLocation).append(" -> ").append(valueOrDash(newLocation));
        if (!handoverNote.trim().isEmpty()) {
            builder.append(" | Ghi chú: ").append(handoverNote.trim());
        }
        return builder.toString();
    }

    private String buildHandoverCurrentSummary(Asset asset) {
        if (asset == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Người dùng hiện tại: ").append(valueOrDash(asset.getAssignedUser())).append('\n');
        builder.append("Phòng ban hiện tại: ")
                .append(valueOrDash(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset.getDepartment())))
                .append('\n');
        builder.append("Vị trí hiện tại: ")
                .append(valueOrDash(AssetLocationUtils.normalizeLocationForDisplay(asset.getLocation())))
                .append('\n');
        builder.append("TID: ").append(valueOrDash(asset.getTid()));
        return builder.toString();
    }

    private String buildHandoverSummary(Asset asset) {
        if (asset == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Mã: ").append(valueOrDash(asset.getAssetCode())).append('\n');
        builder.append("Tên: ").append(valueOrDash(asset.getAssetName())).append('\n');
        builder.append("Người dùng hiện tại: ").append(valueOrDash(asset.getAssignedUser())).append('\n');
        builder.append("Phòng ban hiện tại: ").append(valueOrDash(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset.getDepartment()))).append('\n');
        builder.append("Vị trí hiện tại: ").append(valueOrDash(AssetLocationUtils.normalizeLocationForDisplay(asset.getLocation())));
        return builder.toString();
    }

    private String assetSummaryForLog(Asset asset) {
        if (asset == null) {
            return "-";
        }
        return valueOrDash(asset.getAssetCode())
                + " | " + valueOrDash(asset.getAssignedUser())
                + " | " + valueOrDash(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset.getDepartment()))
                + " | " + valueOrDash(AssetLocationUtils.normalizeLocationForDisplay(asset.getLocation()));
    }

    private long parseDateMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1L;
        }
        try {
            synchronized (tagDateFormat) {
                tagDateFormat.setLenient(false);
                Date parsed = tagDateFormat.parse(value.trim());
                return parsed == null ? -1L : parsed.getTime();
            }
        } catch (ParseException ignored) {
            return -1L;
        }
    }

    private String formatDate(long millis) {
        synchronized (tagDateFormat) {
            return tagDateFormat.format(new Date(millis));
        }
    }

    private String todayDateString() {
        return formatDate(System.currentTimeMillis());
    }

    private void setEditMode(boolean enabled) {
        editing = enabled;
        etLookupCode.setEnabled(enabled);
        etLookupTid.setEnabled(false);
        setEditableFields(enabled);
        updateButtons();
    }

    private void setEditableFields(boolean enabled) {
        for (EditText field : editableFields) {
            field.setEnabled(enabled);
        }
    }

    private void updateButtons() {
        boolean hasAsset = currentAsset != null;
        btnLookupScan.setEnabled(!scannerPreparing && !saving);
        btnLookupStop.setEnabled(!scannerPreparing && qrScannerController != null && qrScannerController.isScanning());
        btnLookupEdit.setEnabled(hasAsset && !editing && !saving);
        btnLookupEdit.setVisibility(editing ? Button.GONE : Button.VISIBLE);
        btnLookupCancel.setVisibility(editing ? Button.VISIBLE : Button.GONE);
        btnLookupCancel.setEnabled(editing && !saving);
        btnLookupSave.setEnabled(hasAsset && editing && !saving);
        btnLookupSave.setVisibility(editing ? Button.VISIBLE : Button.GONE);
        btnLookupHandover.setEnabled(hasAsset && !editing && !saving);
        btnLookupHandover.setVisibility(editing ? Button.GONE : Button.VISIBLE);
    }

    private void updateScannerStatus() {
        if (scannerPreparing) {
            tvLookupScannerStatus.setText(R.string.lookup_scanner_preparing);
            return;
        }
        if (rbLookupQr.isChecked()) {
            if (qrScannerController != null && qrScannerController.isScanning()) {
                tvLookupScannerStatus.setText(R.string.lookup_scanner_qr_running);
                return;
            }
            tvLookupScannerStatus.setText(
                    qrScannerController != null && qrScannerController.isReady()
                            ? R.string.lookup_scanner_qr_ready
                            : R.string.lookup_scanner_qr_lazy
            );
            return;
        }
        tvLookupScannerStatus.setText(rfidScannerController != null && rfidScannerController.isReady()
                ? R.string.lookup_scanner_rfid_ready
                : R.string.lookup_scanner_rfid_lazy);
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
        bindAsset(asset, null);
    }

    @Override
    public void showStatus(String message) {
        if (tvLookupStatus != null && message != null && !message.trim().isEmpty()) {
            tvLookupStatus.setText(message);
        }
    }

    @Override
    public void renderEditMode(boolean editing) {
        setEditMode(editing);
    }

    @Override
    public void renderSaving(boolean saving) {
        this.saving = saving;
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

    private void restoreScreenState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String savedCode = savedInstanceState.getString(STATE_CURRENT_ASSET_CODE, "");
        String savedTid = savedInstanceState.getString(STATE_CURRENT_ASSET_TID, "");
        editing = savedInstanceState.getBoolean(STATE_EDITING, false);
        lookupController.restoreState(savedCode, savedTid, editing, this);
    }

    private String validateEditableForm() {
        String assetName = textOf(etLookupName);
        if (assetName.isEmpty()) {
            return setFieldError(etLookupName, R.string.lookup_need_asset_name);
        }
        return "";
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

    private boolean isValidTagDate(String value) {
        try {
            synchronized (tagDateFormat) {
                tagDateFormat.setLenient(false);
                return tagDateFormat.parse(value) != null;
            }
        } catch (ParseException ignored) {
            return false;
        }
    }

}
