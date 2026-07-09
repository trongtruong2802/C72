package com.idocean.asset.data.sync;

import com.idocean.asset.model.Asset;

import java.util.List;

public interface AssetSyncProgressCallback {
    void onCountReady(int totalCount, int batchSize, String description);

    void onProgress(int loadedCount, int totalCount, int batchIndex, int totalBatches);

    void onSuccess(List<Asset> assets, String message);

    void onError(AssetSyncErrorType errorType, String message);
}
