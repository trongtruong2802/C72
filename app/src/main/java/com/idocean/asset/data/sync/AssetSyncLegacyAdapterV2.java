package com.idocean.asset.data.sync;

import com.idocean.asset.model.AssetSyncQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter mong de bridge AssetSyncQuery hien tai sang coordinator Sync V2 moi.
 */
public final class AssetSyncLegacyAdapterV2 {
    private final AssetSyncCoordinatorV2 coordinator;

    public AssetSyncLegacyAdapterV2() {
        this(new AssetSyncCoordinatorV2());
    }

    public AssetSyncLegacyAdapterV2(AssetSyncCoordinatorV2 coordinator) {
        this.coordinator = coordinator == null ? new AssetSyncCoordinatorV2() : coordinator;
    }

    public AssetSyncCoordinatorV2.SyncResult execute(
            AssetSyncQuery legacyQuery,
            AssetSyncEntrypointMode entrypointMode,
            AssetSyncProgressCallback callback
    ) throws AssetSyncCoordinatorV2.SyncFailureException {
        return coordinator.sync(toQueryV2(legacyQuery, entrypointMode), new ProgressBridgeCallback(legacyQuery, callback));
    }

    public AssetSyncQueryV2 toQueryV2(AssetSyncQuery legacyQuery, AssetSyncEntrypointMode entrypointMode) {
        AssetSyncQuery safeLegacyQuery = legacyQuery == null
                ? new AssetSyncQuery("", "", "", 0)
                : legacyQuery;
        AssetSyncEntrypointMode safeMode = entrypointMode == null
                ? AssetSyncEntrypointMode.inferFromQuery(safeLegacyQuery)
                : entrypointMode;

        if (safeMode == AssetSyncEntrypointMode.FULL) {
            return AssetSyncQueryV2.fullSync();
        }

        List<String> departments = safeCopy(safeLegacyQuery.getDepartments());
        List<String> locations = resolveLocationValues(safeLegacyQuery);
        List<String> assetTypes = safeCopy(safeLegacyQuery.getAssetTypes());

        if (safeMode == AssetSyncEntrypointMode.SESSION) {
            return AssetSyncQueryV2.session(
                    safeLegacyQuery.getDepartment(),
                    locations,
                    assetTypes
            );
        }

        return AssetSyncQueryV2.filtered(departments, locations, assetTypes);
    }

    private List<String> resolveLocationValues(AssetSyncQuery legacyQuery) {
        if (legacyQuery == null) {
            return Collections.emptyList();
        }
        List<String> selectedLocations = safeCopy(legacyQuery.getLocations());
        if (!selectedLocations.isEmpty()) {
            return selectedLocations;
        }
        return safeCopy(legacyQuery.getRequestLocationValues());
    }

    private List<String> safeCopy(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(values);
    }

    private static final class ProgressBridgeCallback implements AssetSyncProgressCallback {
        private final String description;
        private final AssetSyncProgressCallback callback;

        ProgressBridgeCallback(AssetSyncQuery legacyQuery, AssetSyncProgressCallback callback) {
            this.description = legacyQuery == null ? "" : legacyQuery.describe();
            this.callback = callback;
        }

        @Override
        public void onCountReady(int totalCount, int batchSize, String ignoredDescription) {
            if (callback == null) {
                return;
            }
            callback.onCountReady(totalCount, batchSize, description);
        }

        @Override
        public void onProgress(int loadedCount, int totalCount, int batchIndex, int totalBatches) {
            if (callback == null) {
                return;
            }
            callback.onProgress(loadedCount, totalCount, batchIndex, totalBatches);
        }

        @Override
        public void onSuccess(java.util.List<com.idocean.asset.model.Asset> assets, String message) {
        }

        @Override
        public void onError(AssetSyncErrorType errorType, String message) {
        }
    }
}
