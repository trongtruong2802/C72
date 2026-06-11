package com.idocean.asset.data.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Item payload gui len webhook check-in tai san.
 */
public final class InventoryCheckinRequestItemDto {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @SerializedName("code")
    private final String code;

    @SerializedName("tid")
    private final String tid;

    @SerializedName("epc_hex")
    private final String epcHex;

    @SerializedName("scan_source")
    private final String scanSource;

    @SerializedName("scanned_at")
    private final String scannedAt;

    @SerializedName("inventory_status")
    private final String inventoryStatus;

    @SerializedName("asset_name")
    private final String assetName;

    @SerializedName("user")
    private final String user;

    @SerializedName("department")
    private final String department;

    @SerializedName("location")
    private final String location;

    @SerializedName("asset_type")
    private final String assetType;

    @SerializedName("serial")
    private final String serial;

    @SerializedName("operator")
    private final String operator;

    @SerializedName("note")
    private final String note;

    public InventoryCheckinRequestItemDto(
            String code,
            String tid,
            String epcHex,
            String scanSource,
            String scannedAt,
            String inventoryStatus,
            String assetName,
            String user,
            String department,
            String location,
            String assetType,
            String serial,
            String operator,
            String note
    ) {
        this.code = code;
        this.tid = tid;
        this.epcHex = epcHex;
        this.scanSource = scanSource;
        this.scannedAt = scannedAt;
        this.inventoryStatus = inventoryStatus;
        this.assetName = assetName;
        this.user = user;
        this.department = department;
        this.location = location;
        this.assetType = assetType;
        this.serial = serial;
        this.operator = operator;
        this.note = note;
    }

    public String getCode() {
        return code;
    }

    public String getTid() {
        return tid;
    }

    public String getEpcHex() {
        return epcHex;
    }

    public String getScanSource() {
        return scanSource;
    }

    public String getScannedAt() {
        return scannedAt;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getUser() {
        return user;
    }

    public String getDepartment() {
        return department;
    }

    public String getLocation() {
        return location;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getSerial() {
        return serial;
    }

    public String getOperator() {
        return operator;
    }

    public String getNote() {
        return note;
    }

    public JsonObject toJson() {
        return GSON.toJsonTree(this).getAsJsonObject();
    }
}
