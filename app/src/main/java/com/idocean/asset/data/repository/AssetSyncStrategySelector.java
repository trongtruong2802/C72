package com.idocean.asset.data.repository;

import com.idocean.asset.model.AssetSyncQuery;

final class AssetSyncStrategySelector {

    AssetSyncStrategy select(AssetSyncQuery query) {
        if (query == null || !query.hasAnyFilter()) {
            return AssetSyncStrategy.FULL_BATCHED;
        }
        if (query.isLocalFilterOnly()) {
            return AssetSyncStrategy.LOCAL_FILTER_FALLBACK;
        }
        // Backend get-db da ho tro multi-value request, nen multi-select van di remote batched.
        // Local fallback chi con la duong explicit compatibility hoac duong xu ly khi remote that bai.
        return AssetSyncStrategy.FILTERED_REMOTE_BATCHED;
    }
}
