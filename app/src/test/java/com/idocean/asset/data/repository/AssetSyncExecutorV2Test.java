package com.idocean.asset.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonParseException;

import com.idocean.asset.data.sync.AssetSyncExecutorV2;
import com.idocean.asset.data.sync.AssetSyncQueryV2;
import org.junit.Test;

import java.util.Collections;

public class AssetSyncExecutorV2Test {
    @Test
    public void resolveDisplayFallbackLocation_returnsCanonicalDisplayForKnownLocationKeyWhenBodyBlank() {
        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Collections.<String>emptyList(),
                Collections.singletonList("Lau 5 - TT16"),
                Collections.<String>emptyList()
        );

        String fallbackLocation = AssetSyncExecutorV2.resolveDisplayFallbackLocation(query, "");

        assertEquals("L\u1ea7u 5 - TT16", fallbackLocation);
    }

    @Test
    public void resolveDisplayFallbackLocation_returnsEmptyWhenBodyIsPresent() {
        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Collections.<String>emptyList(),
                Collections.singletonList("Lau 5 - TT16"),
                Collections.<String>emptyList()
        );

        String fallbackLocation = AssetSyncExecutorV2.resolveDisplayFallbackLocation(
                query,
                "{\"success\":true,\"items\":[]}"
        );

        assertEquals("", fallbackLocation);
    }

    @Test
    public void resolveDisplayFallbackLocation_returnsEmptyWhenLocationAlreadyUsesDisplayValue() {
        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Collections.<String>emptyList(),
                Collections.singletonList("Warehouse"),
                Collections.<String>emptyList()
        );

        String fallbackLocation = AssetSyncExecutorV2.resolveDisplayFallbackLocation(query, "");

        assertEquals("", fallbackLocation);
    }

    @Test
    public void buildExecutionResult_returnsEmptyAssetsWhenFilteredQueryBodyBlank() {
        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Collections.singletonList("IT"),
                Collections.singletonList("Lau 5 - TT16"),
                Collections.<String>emptyList()
        );

        AssetSyncExecutorV2.ExecutionResult result = AssetSyncExecutorV2.buildExecutionResult(
                query,
                "mock://sync-v2/filtered",
                ""
        );

        assertEquals(0, result.getAssets().size());
        assertEquals("mock://sync-v2/filtered", result.getRequestUrl());
    }

    @Test
    public void buildExecutionResult_throwsWhenFullSyncBodyBlank() {
        try {
            AssetSyncExecutorV2.buildExecutionResult(
                    AssetSyncQueryV2.fullSync(),
                    "mock://sync-v2/full",
                    ""
            );
        } catch (JsonParseException exception) {
            assertTrue(exception.getMessage().contains("khong tra ve du lieu tai san"));
            return;
        }

        throw new AssertionError("Expected blank full-sync response to fail");
    }
}
