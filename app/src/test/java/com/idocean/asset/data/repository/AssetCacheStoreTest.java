package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;

import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class AssetCacheStoreTest {

    @Test
    public void applyCacheSnapshot_populatesSnapshotAndDistinctValues() {
        AssetCacheStore cacheStore = newCacheStore();
        List<Asset> assets = Arrays.asList(
                asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "LAPTOP", "Dang su dung"),
                asset("TS-002", "E280002", "HR", "Bob", "L\u1ea7u 5 - TT16", "MONITOR", "Cho phan bo")
        );

        cacheStore.applyCacheSnapshot(assets, "API");
        AssetRepository.CacheSnapshot snapshot = cacheStore.snapshotCurrentState();

        assertEquals(2, snapshot.getAssetCount());
        assertEquals("API", snapshot.getSource());
        assertEquals(Arrays.asList("IT", "HR"), snapshot.getDistinctValues("department"));
        assertEquals(Arrays.asList("Alice", "Bob"), snapshot.getDistinctValues("assignedUser"));
    }

    @Test
    public void replaceCachedAsset_updatesExistingAssetByIdentity() {
        AssetCacheStore cacheStore = newCacheStore();
        Asset original = asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "LAPTOP", "Dang su dung");
        Asset updated = asset("TS-001", "E280001", "BOD", "Truong Vu", "L\u1ea7u 2 - TT16", "LAPTOP", "Dang su dung");

        cacheStore.applyCacheSnapshot(Arrays.asList(original), "API");
        cacheStore.replaceCachedAsset(original, updated);

        List<Asset> cachedAssets = cacheStore.getCachedAssets();
        assertEquals(1, cachedAssets.size());
        assertEquals("BOD", cachedAssets.get(0).getDepartment());
        assertEquals("Truong Vu", cachedAssets.get(0).getAssignedUser());
        assertEquals("L\u1ea7u 2 - TT16", cachedAssets.get(0).getLocation());
    }

    @Test
    public void replaceCachedAsset_usesOriginalCodeWhenUpdatedCodeChanges() {
        AssetCacheStore cacheStore = newCacheStore();
        Asset original = new Asset(
                null,
                "TS-001",
                "",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-001",
                "IT",
                "Alice",
                "Warehouse",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updated = new Asset(
                null,
                "TS-999",
                "",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-001",
                "IT",
                "Alice",
                "Warehouse",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        cacheStore.applyCacheSnapshot(Arrays.asList(original), "API");
        cacheStore.replaceCachedAsset(original, updated);

        List<Asset> cachedAssets = cacheStore.getCachedAssets();
        assertEquals(1, cachedAssets.size());
        assertEquals("TS-999", cachedAssets.get(0).getAssetCode());
    }

    @Test
    public void clearMemoryCache_resetsAssetsAndSource() {
        AssetCacheStore cacheStore = newCacheStore();
        cacheStore.applyCacheSnapshot(
                Arrays.asList(asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "LAPTOP", "Dang su dung")),
                "API"
        );

        cacheStore.clearMemoryCache();
        AssetRepository.CacheSnapshot snapshot = cacheStore.snapshotCurrentState();

        assertEquals(0, snapshot.getAssetCount());
        assertEquals("CACHE", snapshot.getSource());
    }

    private static AssetCacheStore newCacheStore() {
        return new AssetCacheStore(
                new AssetDiskCacheStore(),
                new AssetFilterService(),
                DashboardMetricsRepository.getInstance(),
                LogRepository.getInstance(),
                Executors.newSingleThreadExecutor()
        );
    }

    private static Asset asset(
            String code,
            String tid,
            String department,
            String assignedUser,
            String location,
            String assetType,
            String inventoryStatus
    ) {
        return new Asset(
                1,
                code,
                tid,
                "",
                "",
                "Laptop Dell",
                assetType,
                "SN-001",
                department,
                assignedUser,
                location,
                inventoryStatus,
                "",
                "",
                "",
                "",
                "API"
        );
    }
}
