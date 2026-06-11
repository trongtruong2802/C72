package com.idocean.asset.data.repository;

import com.idocean.asset.model.AssetSyncQuery;

/**
 * Mode chay sync o entrypoint hien tai de bridge query legacy sang Sync V2.
 */
public enum AssetSyncEntrypointMode {
    FULL,
    FILTERED,
    SESSION;

    public static AssetSyncEntrypointMode inferFromQuery(AssetSyncQuery query) {
        AssetSyncQuery safeQuery = query == null ? new AssetSyncQuery("", "", "", 0) : query;
        return safeQuery.hasAnyFilter() ? FILTERED : FULL;
    }
}
