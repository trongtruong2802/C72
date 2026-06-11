package com.idocean.asset.data.repository;

import com.idocean.asset.model.Asset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Merge ket qua request con theo khoa dedup chinh tid+code.
 */
public final class AssetSyncDeduplicatorV2 {
    private final LinkedHashMap<String, Asset> mergedAssets = new LinkedHashMap<>();

    public void reset() {
        mergedAssets.clear();
    }

    public int merge(List<Asset> assets) {
        if (assets == null || assets.isEmpty()) {
            return 0;
        }

        int beforeSize = mergedAssets.size();
        for (Asset asset : assets) {
            if (asset == null) {
                continue;
            }
            String key = buildDedupKey(asset);
            Asset existing = mergedAssets.get(key);
            if (existing == null || countFilledFields(asset) > countFilledFields(existing)) {
                mergedAssets.put(key, asset);
            }
        }
        return mergedAssets.size() - beforeSize;
    }

    public List<Asset> getMergedAssets() {
        return new ArrayList<>(mergedAssets.values());
    }

    public int size() {
        return mergedAssets.size();
    }

    public List<Asset> deduplicate(List<Asset> assets) {
        reset();
        merge(assets);
        return getMergedAssets();
    }

    private String buildDedupKey(Asset asset) {
        String normalizedTid = normalize(asset == null ? null : asset.getTid());
        String normalizedCode = normalize(asset == null ? null : asset.getAssetCode());
        if (!normalizedTid.isEmpty() || !normalizedCode.isEmpty()) {
            return "PAIR:" + normalizedTid + '|' + normalizedCode;
        }

        // Khi khong co tid/code, giu mot fallback de tranh collapse tat ca record blank thanh 1.
        return "UNKEYED:"
                + normalize(asset == null ? null : asset.getSerialNumber()) + '|'
                + normalize(asset == null ? null : asset.getAssetName()) + '|'
                + normalize(asset == null ? null : asset.getDepartment()) + '|'
                + normalize(asset == null ? null : asset.getLocation());
    }

    private int countFilledFields(Asset asset) {
        if (asset == null) {
            return 0;
        }

        int count = 0;
        count += hasValue(asset.getAssetCode()) ? 1 : 0;
        count += hasValue(asset.getTid()) ? 1 : 0;
        count += hasValue(asset.getAssetName()) ? 1 : 0;
        count += hasValue(asset.getAssetType()) ? 1 : 0;
        count += hasValue(asset.getSerialNumber()) ? 1 : 0;
        count += hasValue(asset.getDepartment()) ? 1 : 0;
        count += hasValue(asset.getAssignedUser()) ? 1 : 0;
        count += hasValue(asset.getLocation()) ? 1 : 0;
        count += hasValue(asset.getInventoryStatus()) ? 1 : 0;
        count += hasValue(asset.getAssetCondition()) ? 1 : 0;
        count += hasValue(asset.getTagDate()) ? 1 : 0;
        count += hasValue(asset.getTagBy()) ? 1 : 0;
        count += hasValue(asset.getNote()) ? 1 : 0;
        return count;
    }

    private boolean hasValue(String value) {
        return !normalize(value).isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
