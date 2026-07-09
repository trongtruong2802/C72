package com.idocean.asset.data.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tach query multi-select thanh danh sach query con don gia tri theo logic AND giua cac field.
 */
public final class AssetSyncCombinationBuilderV2 {
    public static final int DEFAULT_MAX_COMBINATIONS = 30;

    private final int maxCombinations;

    public AssetSyncCombinationBuilderV2() {
        this(DEFAULT_MAX_COMBINATIONS);
    }

    public AssetSyncCombinationBuilderV2(int maxCombinations) {
        this.maxCombinations = maxCombinations > 0 ? maxCombinations : DEFAULT_MAX_COMBINATIONS;
    }

    public BuildResult build(AssetSyncQueryV2 query) throws CombinationLimitException {
        AssetSyncQueryV2 safeQuery = query == null ? AssetSyncQueryV2.fullSync() : query;

        List<String> departments = expandDimension(safeQuery.getDepartments());
        List<String> locations = expandDimension(safeQuery.getLocations());
        List<String> assetTypes = expandDimension(safeQuery.getAssetTypes());

        int combinationCount = departments.size() * locations.size() * assetTypes.size();
        if (combinationCount > maxCombinations) {
            throw new CombinationLimitException(
                    combinationCount,
                    maxCombinations,
                    AssetSyncLogFormatterV2.buildCombinationLimitMessage(
                            safeQuery,
                            combinationCount,
                            maxCombinations
                    )
            );
        }

        List<AssetSyncQueryV2> subQueries = new ArrayList<>(combinationCount);
        for (String department : departments) {
            for (String location : locations) {
                for (String assetType : assetTypes) {
                    subQueries.add(
                            AssetSyncQueryV2.singleCombination(
                                    safeQuery.getMode(),
                                    department,
                                    location,
                                    assetType
                            )
                    );
                }
            }
        }

        return new BuildResult(safeQuery, subQueries, combinationCount, maxCombinations);
    }

    private List<String> expandDimension(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.singletonList("");
        }
        return new ArrayList<>(values);
    }

    public static final class BuildResult {
        private final AssetSyncQueryV2 query;
        private final List<AssetSyncQueryV2> subQueries;
        private final int combinationCount;
        private final int maxCombinations;

        BuildResult(
                AssetSyncQueryV2 query,
                List<AssetSyncQueryV2> subQueries,
                int combinationCount,
                int maxCombinations
        ) {
            this.query = query;
            this.subQueries = Collections.unmodifiableList(new ArrayList<>(subQueries));
            this.combinationCount = combinationCount;
            this.maxCombinations = maxCombinations;
        }

        public AssetSyncQueryV2 getQuery() {
            return query;
        }

        public List<AssetSyncQueryV2> getSubQueries() {
            return subQueries;
        }

        public int getCombinationCount() {
            return combinationCount;
        }

        public int getMaxCombinations() {
            return maxCombinations;
        }

        public String getDescription() {
            return query.describe() + " | requests=" + combinationCount;
        }
    }

    public static final class CombinationLimitException extends Exception {
        private final int combinationCount;
        private final int maxCombinations;

        CombinationLimitException(int combinationCount, int maxCombinations, String message) {
            super(message);
            this.combinationCount = combinationCount;
            this.maxCombinations = maxCombinations;
        }

        public int getCombinationCount() {
            return combinationCount;
        }

        public int getMaxCombinations() {
            return maxCombinations;
        }
    }
}
