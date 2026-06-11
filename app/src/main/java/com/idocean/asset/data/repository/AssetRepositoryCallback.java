package com.idocean.asset.data.repository;

import com.idocean.asset.model.Asset;

import java.util.List;

public interface AssetRepositoryCallback {
    void onSuccess(List<Asset> assets, String message);

    void onError(String message);
}
