package com.idocean.asset.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AssetSyncQueryTest {

    @Test
    public void localFilterOnly_marksQueryForClientSideFiltering() {
        AssetSyncQuery query = AssetSyncQuery.localFilterOnly(
                "IT",
                "L\u1ea7u 5 - TT16",
                Arrays.asList("L\u1ea7u 5 - TT16", "Idoplex-5"),
                "LAPTOP",
                300
        );

        assertTrue(query.isLocalFilterOnly());
        assertEquals("IT", query.getDepartment());
        assertEquals("L\u1ea7u 5 - TT16", query.getLocation());
        assertEquals("LAPTOP", query.getAssetType());
        assertEquals(2, query.getLocationQueries().size());
    }

    @Test
    public void localFilterOnly_supportsMultipleSelections() {
        AssetSyncQuery query = AssetSyncQuery.localFilterOnly(
                Arrays.asList("IT", "HR"),
                Arrays.asList("Warehouse", "L\u1ea7u 5 - TT16"),
                Arrays.asList("Warehouse", "L\u1ea7u 5 - TT16", "Idoplex-5"),
                Arrays.asList("LAPTOP", "PC BUILD"),
                300
        );

        assertTrue(query.isLocalFilterOnly());
        assertEquals(Arrays.asList("IT", "HR"), query.getDepartments());
        assertEquals(Arrays.asList("Warehouse", "L\u1ea7u 5 - TT16"), query.getLocations());
        assertEquals(Arrays.asList("LAPTOP", "PC BUILD"), query.getAssetTypes());
        assertEquals(Arrays.asList("Warehouse", "L\u1ea7u 5 - TT16", "Idoplex-5"), query.getLocationQueries());
    }

    @Test
    public void getRequestLocationValues_prefersLocationQueriesWhenAvailable() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Arrays.asList("IT"),
                Arrays.asList("L\u1ea7u 5 - TT16", "L\u1ea7u 6 - TT16"),
                Arrays.asList("Idoplex-5", "Idoplex-6"),
                Arrays.asList("LAPTOP"),
                300
        );

        assertEquals(Arrays.asList("Idoplex-5", "Idoplex-6"), query.getRequestLocationValues());
    }

    @Test
    public void getRequestLocationValues_fallsBackToLocationsWhenNoAliasesExist() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Arrays.asList("IT"),
                Arrays.asList("L\u1ea7u 5 - TT16", "L\u1ea7u 6 - TT16"),
                Collections.emptyList(),
                Arrays.asList("LAPTOP"),
                300
        );

        assertEquals(Arrays.asList("L\u1ea7u 5 - TT16", "L\u1ea7u 6 - TT16"), query.getRequestLocationValues());
    }

    @Test
    public void getRequestLocationValues_dedupesAndPreservesAliasOrder() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Arrays.asList("IT"),
                Arrays.asList("L\u1ea7u 5 - TT16", "L\u1ea7u 6 - TT16"),
                Arrays.asList(
                        "L\u1ea7u 5 - TT16",
                        "Idoplex - 5",
                        "Idoplex-5",
                        "Idoplex - 5",
                        "L\u1ea7u 6 - TT16",
                        "Idoplex - 6",
                        "Idoplex-6"
                ),
                Arrays.asList("LAPTOP"),
                300
        );

        assertEquals(
                Arrays.asList(
                        "L\u1ea7u 5 - TT16",
                        "Idoplex - 5",
                        "Idoplex-5",
                        "L\u1ea7u 6 - TT16",
                        "Idoplex - 6",
                        "Idoplex-6"
                ),
                query.getRequestLocationValues()
        );
    }

    @Test
    public void withFilters_withoutValues_hasNoFilter() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                300
        );

        assertFalse(query.hasAnyFilter());
        assertEquals("", query.getDepartment());
        assertEquals("", query.getLocation());
        assertEquals("", query.getAssetType());
    }
}
