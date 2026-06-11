package com.idocean.asset.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiAssetListResponseDto {
    @SerializedName("success")
    public Boolean success;
    @SerializedName("total")
    public Integer total;
    @SerializedName("data")
    public List<ApiAssetDto> data;
}
