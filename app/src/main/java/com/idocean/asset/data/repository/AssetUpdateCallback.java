package com.idocean.asset.data.repository;

import com.idocean.asset.model.Asset;

public interface AssetUpdateCallback {
    void onSuccess(Asset asset, String message);

    void onError(String message);
}
