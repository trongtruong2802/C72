package com.idocean.asset.data.repository;

final class AssetSyncPagePolicy {
    static final int UNKNOWN_TOTAL_COUNT = -1;

    private AssetSyncPagePolicy() {
    }

    static int resolveInitialTotalCount(boolean hasExplicitTotalCount, int totalCount, int previewAssetCount) {
        if (previewAssetCount <= 0) {
            return 0;
        }
        if (hasExplicitTotalCount) {
            return Math.max(totalCount, previewAssetCount);
        }
        return UNKNOWN_TOTAL_COUNT;
    }

    static int estimateTotalBatches(int totalCount, int batchSize) {
        if (totalCount < 0 || batchSize <= 0) {
            return UNKNOWN_TOTAL_COUNT;
        }
        return (int) Math.ceil((double) totalCount / batchSize);
    }

    static boolean pageIgnoredRequestedLimit(int requestedLimit, int pageSize) {
        return requestedLimit > 0 && pageSize > requestedLimit;
    }

    static boolean shouldRetryWithoutPagination(
            int requestedLimit,
            int pageSize,
            int addedCount,
            int uniqueCount,
            int totalCount,
            int offset
    ) {
        if (pageSize <= 0) {
            return false;
        }
        if (pageIgnoredRequestedLimit(requestedLimit, pageSize)) {
            return totalCount >= 0 && uniqueCount < totalCount;
        }
        return addedCount <= 0 && offset > 0;
    }

    static boolean shouldStopPaging(
            int requestedLimit,
            int pageSize,
            int addedCount,
            int uniqueCount,
            int totalCount
    ) {
        if (pageSize <= 0) {
            return true;
        }
        if (pageIgnoredRequestedLimit(requestedLimit, pageSize)) {
            return true;
        }
        if (addedCount <= 0) {
            return true;
        }
        if (totalCount >= 0 && uniqueCount >= totalCount) {
            return true;
        }
        return requestedLimit > 0 && pageSize < requestedLimit;
    }
}
