package com.idocean.asset.data.repository;

import com.idocean.asset.data.repository.AssetFilterService;
import com.idocean.asset.data.repository.DashboardMetricsRepository;
import com.idocean.asset.data.repository.LogRepository;

import static org.junit.Assert.assertEquals;

import com.idocean.asset.data.cache.AssetCacheStore;
import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class AssetCacheStoreUpdateTest {

    @Test
    public void replaceCachedAsset_keepsExistingAssetsIntact() {
        AssetCacheStore cacheStore = new AssetCacheStore(
                new AssetFilterService(),
                DashboardMetricsRepository.getInstance(),
                LogRepository.getInstance(),
                Executors.newSingleThreadExecutor()
        );

        Asset asset1 = createAsset("TS-001", "TID-001", "Laptop A", "IT", "Lầu 1");
        Asset asset2 = createAsset("TS-002", "TID-002", "Laptop B", "HR", "Lầu 2");

        cacheStore.applyCacheSnapshot(Arrays.asList(asset1, asset2), "API");

        Asset updatedAsset1 = createAsset("TS-001", "TID-001", "Laptop A (Đã cập nhật)", "BOD", "Lầu 3");
        cacheStore.replaceCachedAsset(asset1, updatedAsset1);

        List<Asset> cached = cacheStore.getCachedAssets();
        assertEquals(2, cached.size());
        assertEquals("Laptop A (Đã cập nhật)", cached.get(0).getAssetName());
        assertEquals("BOD", cached.get(0).getDepartment());
        assertEquals("Lầu 3", cached.get(0).getLocation());
        assertEquals("TS-002", cached.get(1).getAssetCode());
    }

    @Test
    public void persistCacheAsync_doesNotThrowWhenSnapshotIsEmpty() {
        AssetCacheStore cacheStore = new AssetCacheStore(
                new AssetFilterService(),
                DashboardMetricsRepository.getInstance(),
                LogRepository.getInstance(),
                Executors.newSingleThreadExecutor()
        );

        // Memory cache empty
        cacheStore.persistCacheAsync();
        assertEquals(0, cacheStore.getCachedAssetCount());
    }

    private Asset createAsset(String code, String tid, String name, String department, String location) {
        return new Asset(
                1,
                code,
                tid,
                "",
                "",
                name,
                "Laptop",
                "SN-123",
                department,
                "User X",
                location,
                "Đang sử dụng",
                "",
                "",
                "",
                "",
                "API"
        );
    }
}
