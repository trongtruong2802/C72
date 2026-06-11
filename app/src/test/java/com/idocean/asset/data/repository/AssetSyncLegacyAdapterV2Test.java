package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;

import com.idocean.asset.model.AssetSyncQuery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AssetSyncLegacyAdapterV2Test {
    @Test
    public void toQueryV2_fullModeDropsLegacyFiltersAndUsesFullSync() {
        AssetSyncLegacyAdapterV2 adapter = new AssetSyncLegacyAdapterV2();

        AssetSyncQueryV2 queryV2 = adapter.toQueryV2(
                AssetSyncQuery.withFilters(
                        Arrays.asList("IT"),
                        Arrays.asList("Lau 5 - TT16"),
                        Arrays.asList("Idoplex - 5"),
                        Arrays.asList("Laptop"),
                        300
                ),
                AssetSyncEntrypointMode.FULL
        );

        assertEquals(AssetSyncQueryV2.Mode.FULL, queryV2.getMode());
        assertEquals(0, queryV2.getDepartments().size());
        assertEquals(0, queryV2.getLocations().size());
        assertEquals(0, queryV2.getAssetTypes().size());
    }

    @Test
    public void toQueryV2_sessionModeKeepsCanonicalSelectedLocation() {
        AssetSyncLegacyAdapterV2 adapter = new AssetSyncLegacyAdapterV2();

        AssetSyncQuery legacyQuery = AssetSyncQuery.withFilters(
                Arrays.asList(" IT "),
                Arrays.asList("Lau 5 - TT16"),
                Arrays.asList("Idoplex - 5", "Idoplex-5"),
                Collections.singletonList(" Laptop "),
                300
        );

        AssetSyncQueryV2 queryV2 = adapter.toQueryV2(legacyQuery, AssetSyncEntrypointMode.SESSION);

        assertEquals(AssetSyncQueryV2.Mode.SESSION, queryV2.getMode());
        assertEquals(Collections.singletonList("IT"), queryV2.getDepartments());
        assertEquals(Collections.singletonList("TT16_F5"), queryV2.getLocations());
        assertEquals(Collections.singletonList("Laptop"), queryV2.getAssetTypes());
    }
}
