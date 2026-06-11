package com.idocean.asset.model;

public enum InventoryItemStatus {
    CHECKED("Đã kiểm kê"),
    MISSING("Chưa thấy"),
    OUTSIDE("Ngoài danh sách");

    private final String label;

    InventoryItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
