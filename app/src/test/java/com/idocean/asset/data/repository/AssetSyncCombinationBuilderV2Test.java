package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AssetSyncCombinationBuilderV2Test {
    @Test
    public void build_filteredMultiValueCreatesCartesianProduct() throws Exception {
        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Arrays.asList(" IT ", "HR"),
                Arrays.asList(" Lau 5 - TT16 ", "Lau 6 - TT16"),
                Collections.<String>emptyList()
        );

        AssetSyncCombinationBuilderV2.BuildResult result =
                new AssetSyncCombinationBuilderV2().build(query);

        assertEquals(4, result.getCombinationCount());

        List<AssetSyncQueryV2> subQueries = result.getSubQueries();
        assertEquals("IT", subQueries.get(0).getSingleDepartment());
        assertEquals("TT16_F5", subQueries.get(0).getSingleLocation());
        assertEquals("IT", subQueries.get(1).getSingleDepartment());
        assertEquals("TT16_F6", subQueries.get(1).getSingleLocation());
        assertEquals("HR", subQueries.get(2).getSingleDepartment());
        assertEquals("TT16_F5", subQueries.get(2).getSingleLocation());
        assertEquals("HR", subQueries.get(3).getSingleDepartment());
        assertEquals("TT16_F6", subQueries.get(3).getSingleLocation());
    }

    @Test
    public void build_tooManyCombinationsFailsEarly() {
        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Arrays.asList("D1", "D2", "D3", "D4", "D5", "D6"),
                Arrays.asList("L1", "L2", "L3", "L4", "L5", "L6"),
                Collections.<String>emptyList()
        );

        try {
            new AssetSyncCombinationBuilderV2().build(query);
        } catch (AssetSyncCombinationBuilderV2.CombinationLimitException exception) {
            assertTrue(exception.getMessage().contains("filtered"));
            assertTrue(exception.getMessage().contains("36"));
            assertTrue(exception.getMessage().contains("30"));
            return;
        }

        throw new AssertionError("Expected combination limit exception");
    }
}
