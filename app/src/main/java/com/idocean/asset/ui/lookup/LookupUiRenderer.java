package com.idocean.asset.ui.lookup;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.List;

final class LookupUiRenderer {
    private final TextView tvLookupStatus;
    private final TextView tvLookupScannerStatus;
    private final Button btnLookupScan;
    private final Button btnLookupStop;
    private final Button btnLookupEdit;
    private final Button btnLookupCancel;
    private final Button btnLookupSave;
    private final Button btnLookupHandover;
    private final Button btnLookupManualAdd;
    private final EditText etLookupCode;
    private final EditText etLookupTid;
    private final EditText etLookupOldCode;
    private final EditText etLookupOldSerial;
    private final EditText etLookupName;
    private final MaterialAutoCompleteTextView etLookupType;
    private final EditText etLookupSerial;
    private final MaterialAutoCompleteTextView etLookupDepartment;
    private final EditText etLookupUser;
    private final MaterialAutoCompleteTextView etLookupLocation;
    private final MaterialAutoCompleteTextView etLookupInventoryStatus;
    private final EditText etLookupNote;
    private final EditText[] editableFields;

    LookupUiRenderer(TextView tvLookupStatus,
                     TextView tvLookupScannerStatus,
                     Button btnLookupScan,
                     Button btnLookupStop,
                     Button btnLookupManualAdd,
                     Button btnLookupEdit,
                     Button btnLookupCancel,
                     Button btnLookupSave,
                     Button btnLookupHandover,
                     EditText etLookupCode,
                     EditText etLookupTid,
                     EditText etLookupOldCode,
                     EditText etLookupOldSerial,
                     EditText etLookupName,
                     MaterialAutoCompleteTextView etLookupType,
                     EditText etLookupSerial,
                     MaterialAutoCompleteTextView etLookupDepartment,
                     EditText etLookupUser,
                     MaterialAutoCompleteTextView etLookupLocation,
                     MaterialAutoCompleteTextView etLookupInventoryStatus,
                     EditText etLookupNote,
                     EditText[] editableFields) {
        this.tvLookupStatus = tvLookupStatus;
        this.tvLookupScannerStatus = tvLookupScannerStatus;
        this.btnLookupScan = btnLookupScan;
        this.btnLookupStop = btnLookupStop;
        this.btnLookupManualAdd = btnLookupManualAdd;
        this.btnLookupEdit = btnLookupEdit;
        this.btnLookupCancel = btnLookupCancel;
        this.btnLookupSave = btnLookupSave;
        this.btnLookupHandover = btnLookupHandover;
        this.etLookupCode = etLookupCode;
        this.etLookupTid = etLookupTid;
        this.etLookupOldCode = etLookupOldCode;
        this.etLookupOldSerial = etLookupOldSerial;
        this.etLookupName = etLookupName;
        this.etLookupType = etLookupType;
        this.etLookupSerial = etLookupSerial;
        this.etLookupDepartment = etLookupDepartment;
        this.etLookupUser = etLookupUser;
        this.etLookupLocation = etLookupLocation;
        this.etLookupInventoryStatus = etLookupInventoryStatus;
        this.etLookupNote = etLookupNote;
        this.editableFields = editableFields;
    }

    void bindDropdown(MaterialAutoCompleteTextView view, List<String> options) {
        if (view == null) {
            return;
        }
        view.setSimpleItems(options == null ? new String[0] : options.toArray(new String[0]));
        view.setOnClickListener(v -> view.showDropDown());
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                view.showDropDown();
            }
        });
    }

    void renderAsset(Asset asset) {
        if (asset == null) {
            clearForm();
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
        String status = asset.getInventoryStatus();
        if (status == null || status.trim().isEmpty()) {
            status = asset.getAssetCondition();
        }
        etLookupInventoryStatus.setText(AssetFieldNormalizer.normalizeInventoryStatusForDisplay(status), false);
        etLookupNote.setText(valueOrEmpty(asset.getNote()));
    }

    void renderStatus(String message) {
        if (tvLookupStatus != null && message != null && !message.trim().isEmpty()) {
            tvLookupStatus.setText(message);
        }
    }

    void renderEditMode(boolean enabled) {
        renderEditMode(enabled, false);
    }

    void renderEditMode(boolean enabled, boolean isTidEditable) {
        etLookupCode.setEnabled(enabled);
        etLookupTid.setEnabled(enabled && isTidEditable);
        if (editableFields != null) {
            for (EditText field : editableFields) {
                if (field != null) {
                    field.setEnabled(enabled);
                }
            }
        }
    }

    void renderScannerStatus(String message) {
        if (tvLookupScannerStatus != null) {
            tvLookupScannerStatus.setText(message == null ? "" : message);
        }
    }

    void renderButtons(boolean hasAsset,
                       boolean editing,
                       boolean saving,
                       boolean scannerPreparing,
                       boolean qrScanning) {
        btnLookupScan.setEnabled(!scannerPreparing && !saving);
        btnLookupStop.setEnabled(!scannerPreparing && qrScanning);
        if (btnLookupManualAdd != null) {
            btnLookupManualAdd.setEnabled(!scannerPreparing && !editing && !saving);
        }
        btnLookupEdit.setEnabled(hasAsset && !editing && !saving);
        btnLookupEdit.setVisibility(editing ? Button.GONE : Button.VISIBLE);
        btnLookupCancel.setVisibility(editing ? Button.VISIBLE : Button.GONE);
        btnLookupCancel.setEnabled(editing && !saving);
        btnLookupSave.setEnabled(hasAsset && editing && !saving);
        btnLookupSave.setVisibility(editing ? Button.VISIBLE : Button.GONE);
        btnLookupHandover.setEnabled(hasAsset && !editing && !saving);
        btnLookupHandover.setVisibility(editing ? Button.GONE : Button.VISIBLE);
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
