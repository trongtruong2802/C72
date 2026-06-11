package com.idocean.asset.model;

import java.io.Serializable;

/**
 * Item tai san duoc them vao phieu check out.
 */
public class CheckoutAssetItem implements Serializable {
    private final String identityKey;
    private final String tid;
    private final String code;
    private final String assetName;
    private final String assetType;
    private final String serialNumber;
    private final String department;
    private final String assignedUser;
    private final String location;
    private final String scanSource;
    private final long scannedAt;
    private final boolean matchedFromCache;

    public CheckoutAssetItem(String identityKey,
                             String tid,
                             String code,
                             String assetName,
                             String assetType,
                             String serialNumber,
                             String department,
                             String assignedUser,
                             String location,
                             String scanSource,
                             long scannedAt,
                             boolean matchedFromCache) {
        this.identityKey = safe(identityKey);
        this.tid = safe(tid);
        this.code = safe(code);
        this.assetName = safe(assetName);
        this.assetType = safe(assetType);
        this.serialNumber = safe(serialNumber);
        this.department = safe(department);
        this.assignedUser = safe(assignedUser);
        this.location = safe(location);
        this.scanSource = safe(scanSource);
        this.scannedAt = scannedAt;
        this.matchedFromCache = matchedFromCache;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public String getTid() {
        return tid;
    }

    public String getCode() {
        return code;
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

    public String getScanSource() {
        return scanSource;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public boolean isMatchedFromCache() {
        return matchedFromCache;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
