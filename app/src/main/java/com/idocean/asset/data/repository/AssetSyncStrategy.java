package com.idocean.asset.data.repository;

enum AssetSyncStrategy {
    FULL_BATCHED,
    FILTERED_REMOTE_BATCHED,
    LOCAL_FILTER_FALLBACK
}
