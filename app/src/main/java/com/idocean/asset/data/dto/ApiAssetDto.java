package com.idocean.asset.data.dto;

import com.google.gson.annotations.SerializedName;

public class ApiAssetDto {
    @SerializedName("stt")
    public Integer stt;
    @SerializedName("code")
    public String code;
    @SerializedName("tid")
    public String tid;
    @SerializedName("inventory_status")
    public String inventoryStatus;
    @SerializedName("old_serial")
    public String oldSerial;
    @SerializedName("old_code")
    public String oldCode;
    @SerializedName("asset_name")
    public String assetName;
    @SerializedName("asset_type")
    public String assetType;
    @SerializedName("serial_number")
    public String serialNumber;
    @SerializedName("department")
    public String department;
    @SerializedName("user")
    public String user;
    @SerializedName("location")
    public String location;
    @SerializedName("condition")
    public String condition;
    @SerializedName("tag_date")
    public String tagDate;
    @SerializedName("tag_by")
    public String tagBy;
    @SerializedName("note")
    public String note;
}
