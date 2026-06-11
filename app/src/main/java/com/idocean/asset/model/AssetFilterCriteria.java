package com.idocean.asset.model;

/**
 * Dieu kien loc danh sach tai san tren runtime cache.
 */
public class AssetFilterCriteria {
    private final String query;
    private final String inventoryStatus;
    private final String assetType;
    private final String department;
    private final String assignedUser;
    private final String location;

    public AssetFilterCriteria(String query,
                               String inventoryStatus,
                               String assetType,
                               String department,
                               String assignedUser,
                               String location) {
        this.query = safe(query);
        this.inventoryStatus = safe(inventoryStatus);
        this.assetType = safe(assetType);
        this.department = safe(department);
        this.assignedUser = safe(assignedUser);
        this.location = safe(location);
    }

    public String getQuery() {
        return query;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public String getAssetType() {
        return assetType;
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
