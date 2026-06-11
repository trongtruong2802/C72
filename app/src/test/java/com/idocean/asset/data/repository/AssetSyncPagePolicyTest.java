package com.idocean.asset.data.repository;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssetSyncPagePolicyTest {

    @Test
    public void resolveInitialTotalCount_returnsUnknownWhenBackendOmitsTotal() {
        int totalCount = AssetSyncPagePolicy.resolveInitialTotalCount(false, 1, 1);

        assertEquals(AssetSyncPagePolicy.UNKNOWN_TOTAL_COUNT, totalCount);
    }

    @Test
    public void shouldRetryWithoutPagination_whenLaterPageAddsNoNewAssets() {
        boolean shouldRetry = AssetSyncPagePolicy.shouldRetryWithoutPagination(
                300,
                300,
                0,
                300,
                900,
                300
        );

        assertTrue(shouldRetry);
    }

    @Test
    public void shouldRetryWithoutPagination_whenBackendIgnoresLimitAndStillMissingRows() {
        boolean shouldRetry = AssetSyncPagePolicy.shouldRetryWithoutPagination(
                300,
                500,
                500,
                500,
                1000,
                0
        );

        assertTrue(shouldRetry);
    }

    @Test
    public void shouldStopPaging_whenBatchIsShorterThanRequestedLimit() {
        boolean shouldStop = AssetSyncPagePolicy.shouldStopPaging(
                300,
                120,
                120,
                420,
                AssetSyncPagePolicy.UNKNOWN_TOTAL_COUNT
        );

        assertTrue(shouldStop);
    }

    @Test
    public void shouldNotRetryWithoutPagination_onFirstExactFullBatch() {
        boolean shouldRetry = AssetSyncPagePolicy.shouldRetryWithoutPagination(
                300,
                300,
                300,
                300,
                AssetSyncPagePolicy.UNKNOWN_TOTAL_COUNT,
                0
        );

        assertFalse(shouldRetry);
    }
}
