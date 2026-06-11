package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;

import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AssetSyncDeduplicatorV2Test {
    @Test
    public void merge_sameTidAndCodeKeepsRicherAsset() {
        Asset poorer = asset("CODE-01", "TID-01", "", "", "", "");
        Asset richer = asset("CODE-01", "TID-01", "Laptop Dell", "SN-01", "IT", "Lau 5 - TT16");

        AssetSyncDeduplicatorV2 deduplicator = new AssetSyncDeduplicatorV2();
        deduplicator.merge(Arrays.asList(poorer, richer));

        List<Asset> mergedAssets = deduplicator.getMergedAssets();
        assertEquals(1, mergedAssets.size());
        assertEquals("Laptop Dell", mergedAssets.get(0).getAssetName());
        assertEquals("IT", mergedAssets.get(0).getDepartment());
        assertEquals("Lau 5 - TT16", mergedAssets.get(0).getLocation());
    }

    @Test
    public void merge_blankTidAndCodeUsesFallbackKey() {
        Asset left = asset("", "", "Monitor A", "SN-01", "IT", "Lau 5 - TT16");
        Asset right = asset("", "", "Monitor B", "SN-02", "IT", "Lau 5 - TT16");

        AssetSyncDeduplicatorV2 deduplicator = new AssetSyncDeduplicatorV2();
        deduplicator.merge(Arrays.asList(left, right));

        assertEquals(2, deduplicator.size());
    }

    private Asset asset(
            String assetCode,
            String tid,
            String assetName,
            String serialNumber,
            String department,
            String location
    ) {
        return new Asset(
                1,
                assetCode,
                tid,
                "",
                "",
                assetName,
                "Laptop",
                serialNumber,
                department,
                "",
                location,
                "",
                "",
                "",
                "",
                "",
                "API"
        );
    }
}
