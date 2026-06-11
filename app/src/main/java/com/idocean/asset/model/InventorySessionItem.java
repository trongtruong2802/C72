package com.idocean.asset.model;

/**
 * Record hien thi trong man kiem ke cua mot phien lam viec.
 */
public class InventorySessionItem {
    private final String itemKey;
    private final Asset asset;
    private final boolean outsideList;

    private InventoryItemStatus status;
    private String scannedCode;
    private String scannedTid;
    private String scannedEpcHex;
    private InventoryScanSource scanSource;
    private long scannedAt;
    private String operatorName;
    private String inventoryNote;
    private int scanCount;

    private InventorySessionItem(String itemKey, Asset asset, boolean outsideList) {
        this.itemKey = itemKey;
        this.asset = asset;
        this.outsideList = outsideList;
        this.status = outsideList ? InventoryItemStatus.OUTSIDE : InventoryItemStatus.MISSING;
        this.scanSource = InventoryScanSource.NONE;
    }

    public static InventorySessionItem fromAsset(String itemKey, Asset asset) {
        return new InventorySessionItem(itemKey, asset, false);
    }

    public static InventorySessionItem createOutsideRfid(String itemKey, String tid, String epcHex, String epcAsciiCode,
                                                         long timestamp, String operatorName, String note) {
        Asset outsideAsset = new Asset(
                null,
                epcAsciiCode,
                tid,
                "",
                "",
                "[Ngoai danh sach]",
                "",
                "",
                "",
                "",
                "",
                InventoryItemStatus.OUTSIDE.getLabel(),
                "",
                "",
                "",
                note,
                "RFID"
        );
        InventorySessionItem item = new InventorySessionItem(itemKey, outsideAsset, true);
        item.markScanned(InventoryScanSource.RFID, operatorName, note, timestamp, epcAsciiCode, tid, epcHex);
        return item;
    }

    public static InventorySessionItem createOutsideQr(String itemKey, String code,
                                                       long timestamp, String operatorName, String note) {
        Asset outsideAsset = new Asset(
                null,
                code,
                "",
                "",
                "",
                "[Ngoai danh sach]",
                "",
                "",
                "",
                "",
                "",
                InventoryItemStatus.OUTSIDE.getLabel(),
                "",
                "",
                "",
                note,
                "QR"
        );
        InventorySessionItem item = new InventorySessionItem(itemKey, outsideAsset, true);
        item.markScanned(InventoryScanSource.QR, operatorName, note, timestamp, code, "", "");
        return item;
    }

    public void markScanned(InventoryScanSource scanSource, String operatorName, String note,
                            long timestamp, String scannedCode, String scannedTid, String scannedEpcHex) {
        this.scanSource = scanSource;
        this.operatorName = operatorName == null ? "" : operatorName;
        this.inventoryNote = note == null ? "" : note;
        this.scannedAt = timestamp;
        this.scannedCode = scannedCode == null ? "" : scannedCode;
        this.scannedTid = scannedTid == null ? "" : scannedTid;
        this.scannedEpcHex = scannedEpcHex == null ? "" : scannedEpcHex;
        this.scanCount += 1;
        if (!outsideList) {
            this.status = InventoryItemStatus.CHECKED;
        }
    }

    public void resetScan() {
        if (outsideList) {
            return;
        }
        status = InventoryItemStatus.MISSING;
        scannedCode = "";
        scannedTid = "";
        scannedEpcHex = "";
        scanSource = InventoryScanSource.NONE;
        scannedAt = 0L;
        operatorName = "";
        inventoryNote = "";
        scanCount = 0;
    }

    public String getItemKey() {
        return itemKey;
    }

    public Asset getAsset() {
        return asset;
    }

    public boolean isOutsideList() {
        return outsideList;
    }

    public InventoryItemStatus getStatus() {
        return status;
    }

    public String getDisplayCode() {
        if (scannedCode != null && !scannedCode.trim().isEmpty()) {
            return scannedCode;
        }
        return asset.getAssetCode() == null ? "" : asset.getAssetCode();
    }

    public String getDisplayTid() {
        if (scannedTid != null && !scannedTid.trim().isEmpty()) {
            return scannedTid;
        }
        return asset.getTid() == null ? "" : asset.getTid();
    }

    public String getDisplayEpcHex() {
        return scannedEpcHex == null ? "" : scannedEpcHex;
    }

    public String getAssetName() {
        return asset.getAssetName();
    }

    public String getAssignedUser() {
        return asset.getAssignedUser();
    }

    public String getDepartment() {
        return asset.getDepartment();
    }

    public String getLocation() {
        return asset.getLocation();
    }

    public String getAssetType() {
        return asset.getAssetType();
    }

    public String getSerialNumber() {
        return asset.getSerialNumber();
    }

    public InventoryScanSource getScanSource() {
        return scanSource;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getInventoryNote() {
        return inventoryNote;
    }

    public int getScanCount() {
        return scanCount;
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String normalized = query.trim().toLowerCase();
        return contains(getStatus().getLabel(), normalized)
                || contains(getDisplayCode(), normalized)
                || contains(getDisplayTid(), normalized)
                || contains(getDisplayEpcHex(), normalized)
                || contains(getAssetName(), normalized)
                || contains(getAssignedUser(), normalized)
                || contains(getDepartment(), normalized)
                || contains(getLocation(), normalized)
                || contains(getAssetType(), normalized)
                || contains(getSerialNumber(), normalized)
                || contains(getOperatorName(), normalized)
                || contains(getInventoryNote(), normalized);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
