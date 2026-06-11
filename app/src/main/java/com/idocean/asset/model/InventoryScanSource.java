package com.idocean.asset.model;

public enum InventoryScanSource {
    RFID("RFID"),
    QR("QR"),
    NONE("-");

    private final String label;

    InventoryScanSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
