package com.idocean.asset.ui.lookup;

/**
 * Draft chi chua du lieu edit cua man tra cuu.
 */
public final class EditableAssetDraft {
    public final String code;
    public final String tid;
    public final String oldCode;
    public final String oldSerial;
    public final String assetName;
    public final String assetType;
    public final String serialNumber;
    public final String department;
    public final String assignedUser;
    public final String location;
    public final String inventoryStatus;
    public final String note;

    public EditableAssetDraft(
            String code,
            String tid,
            String oldCode,
            String oldSerial,
            String assetName,
            String assetType,
            String serialNumber,
            String department,
            String assignedUser,
            String location,
            String inventoryStatus,
            String note
    ) {
        this.code = code;
        this.tid = tid;
        this.oldCode = oldCode;
        this.oldSerial = oldSerial;
        this.assetName = assetName;
        this.assetType = assetType;
        this.serialNumber = serialNumber;
        this.department = department;
        this.assignedUser = assignedUser;
        this.location = location;
        this.inventoryStatus = inventoryStatus;
        this.note = note;
    }
}
