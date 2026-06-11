package com.idocean.asset.model;

import com.idocean.asset.utils.AssetFieldNormalizer;

/**
 * Model nội bộ chuẩn hóa cho tài sản, dùng chung cho API và file import.
 */
public class Asset {
    private final Integer rowNumber;
    private final String assetCode;
    private final String tid;
    private final String oldCode;
    private final String oldSerial;
    private final String assetName;
    private final String assetType;
    private final String serialNumber;
    private final String department;
    private final String assignedUser;
    private final String location;
    private final String inventoryStatus;
    private final String assetCondition;
    private final String tagDate;
    private final String tagBy;
    private final String note;
    private final String source;

    public Asset(Integer rowNumber, String assetCode, String tid, String oldCode, String oldSerial,
                 String assetName, String assetType, String serialNumber, String department,
                 String assignedUser, String location, String inventoryStatus, String assetCondition,
                 String tagDate, String tagBy, String note, String source) {
        this.rowNumber = rowNumber;
        this.assetCode = assetCode;
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
        this.assetCondition = assetCondition;
        this.tagDate = tagDate;
        this.tagBy = tagBy;
        this.note = note;
        this.source = source;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public String getTid() {
        return tid;
    }

    public String getOldCode() {
        return oldCode;
    }

    public String getOldSerial() {
        return oldSerial;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getDepartment() {
        return department;
    }

    public String getAssignedUser() {
        return assignedUser;
    }

    public String getLocation() {
        return location;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public String getAssetCondition() {
        return assetCondition;
    }

    public String getTagDate() {
        return tagDate;
    }

    public String getTagBy() {
        return tagBy;
    }

    public String getNote() {
        return note;
    }

    public String getSource() {
        return source;
    }

    public String toDisplayLine() {
        return safe(assetCode) + " | " + safe(assetName) + " | "
                + safe(AssetFieldNormalizer.normalizeDepartmentForDisplay(department)) + " | "
                + safe(AssetFieldNormalizer.normalizeLocationForDisplay(location)) + " | "
                + safe(AssetFieldNormalizer.normalizeInventoryStatusForDisplay(inventoryStatus));
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
