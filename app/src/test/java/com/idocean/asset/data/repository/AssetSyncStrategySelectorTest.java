package com.idocean.asset.data.repository;

import com.idocean.asset.model.AssetSyncQuery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class AssetSyncStrategySelectorTest {
    private final AssetSyncStrategySelector selector = new AssetSyncStrategySelector();

    @Test
    public void select_returnsFullBatchedForEmptyQuery() {
        assertEquals(
                AssetSyncStrategy.FULL_BATCHED,
                selector.select(AssetSyncQuery.withFilters(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        300
                ))
        );
    }

    @Test
    public void select_returnsFilteredRemoteBatchedForMultiLocationQuery() {
        assertEquals(
                AssetSyncStrategy.FILTERED_REMOTE_BATCHED,
                selector.select(AssetSyncQuery.withFilters(
                        Arrays.asList("IT"),
                        Arrays.asList("Lầu 5 - TT16", "Lầu 6 - TT16"),
                        Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5", "Lầu 6 - TT16", "Idoplex - 6", "Idoplex-6"),
                        Arrays.asList("LAPTOP"),
                        300
                ))
        );
    }

    @Test
    public void select_returnsFilteredRemoteBatchedForMultiDepartmentQuery() {
        assertEquals(
                AssetSyncStrategy.FILTERED_REMOTE_BATCHED,
                selector.select(AssetSyncQuery.withFilters(
                        Arrays.asList("IT", "HR"),
                        Arrays.asList("Lầu 5 - TT16"),
                        Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5"),
                        Arrays.asList("LAPTOP"),
                        300
                ))
        );
    }

    @Test
    public void select_returnsFilteredRemoteBatchedForMultiAssetTypeQuery() {
        assertEquals(
                AssetSyncStrategy.FILTERED_REMOTE_BATCHED,
                selector.select(AssetSyncQuery.withFilters(
                        Arrays.asList("IT"),
                        Arrays.asList("Lầu 5 - TT16"),
                        Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5"),
                        Arrays.asList("LAPTOP", "MONITOR"),
                        300
                ))
        );
    }

    @Test
    public void select_returnsFilteredRemoteBatchedForSingleFilteredQuery() {
        assertEquals(
                AssetSyncStrategy.FILTERED_REMOTE_BATCHED,
                selector.select(AssetSyncQuery.withFilters(
                        Arrays.asList("IT"),
                        Arrays.asList("Lầu 5 - TT16"),
                        Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5"),
                        Arrays.asList("LAPTOP"),
                        300
                ))
        );
    }

    @Test
    public void select_returnsLocalFallbackWhenExplicitlyRequested() {
        assertEquals(
                AssetSyncStrategy.LOCAL_FILTER_FALLBACK,
                selector.select(AssetSyncQuery.localFilterOnly(
                        Arrays.asList("IT"),
                        Arrays.asList("Lầu 5 - TT16"),
                        Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5"),
                        Arrays.asList("LAPTOP"),
                        300
                ))
        );
    }
}
