package com.idocean.asset.data.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.ContextWrapper;

import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class AssetDiskCacheStoreTest {

    @Test
    public void serializeAndDeserialize_roundTripsAssetsAndSource() throws IOException {
        AssetDiskCacheStore store = new AssetDiskCacheStore();
        Asset asset = new Asset(
                7,
                "TS-007",
                "E280007",
                "OLD-007",
                "SER-OLD-007",
                "Laptop Dell 007",
                "LAPTOP",
                "SER-007",
                "IT",
                "Thang Nguyen",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "2026-04-09",
                "Admin",
                "Ghi chu",
                "API"
        );

        String rawJson = store.serialize(Collections.singletonList(asset), "API", 123456789L);
        AssetDiskCacheStore.CacheSnapshot snapshot = store.deserialize(rawJson);

        assertNotNull(snapshot);
        assertEquals("API", snapshot.source);
        assertEquals(123456789L, snapshot.cachedAt);
        assertEquals(1, snapshot.assets.size());
        assertEquals("TS-007", snapshot.assets.get(0).getAssetCode());
        assertEquals("Thang Nguyen", snapshot.assets.get(0).getAssignedUser());
    }

    @Test
    public void deserialize_returnsNullWhenSnapshotHasNoAssets() throws IOException {
        AssetDiskCacheStore store = new AssetDiskCacheStore();

        AssetDiskCacheStore.CacheSnapshot snapshot = store.deserialize(
                "{\"source\":\"API\",\"cachedAt\":1,\"assets\":[]}"
        );

        assertNull(snapshot);
    }

    @Test
    public void writeReadAndClear_roundTripThroughContextFilesDir() throws IOException {
        AssetDiskCacheStore store = new AssetDiskCacheStore();
        File tempDir = createTempDir();
        try {
            Context context = new FilesDirContext(tempDir);
            Asset asset = new Asset(
                    7,
                    "TS-007",
                    "E280007",
                    "OLD-007",
                    "SER-OLD-007",
                    "Laptop Dell 007",
                    "LAPTOP",
                    "SER-007",
                    "IT",
                    "Thang Nguyen",
                    "L\u1ea7u 5 - TT16",
                    "Dang su dung",
                    "",
                    "2026-04-09",
                    "Admin",
                    "Ghi chu",
                    "API"
            );

            store.write(context, Collections.singletonList(asset), "API");
            AssetDiskCacheStore.CacheSnapshot snapshot = store.read(context);

            assertNotNull(snapshot);
            assertEquals("API", snapshot.source);
            assertEquals(1, snapshot.assets.size());
            assertEquals("TS-007", snapshot.assets.get(0).getAssetCode());

            store.clear(context);
            assertFalse(new File(tempDir, "ido_asset_runtime_cache.json").exists());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static File createTempDir() throws IOException {
        File tempFile = File.createTempFile("asset-disk-cache-store", "");
        if (!tempFile.delete()) {
            throw new IOException("Khong the xoa file tam de tao thu muc test.");
        }
        if (!tempFile.mkdir()) {
            throw new IOException("Khong the tao thu muc test.");
        }
        return tempFile;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private static final class FilesDirContext extends ContextWrapper {
        private final File filesDir;

        FilesDirContext(File filesDir) {
            super(null);
            this.filesDir = filesDir;
        }

        @Override
        public File getFilesDir() {
            return filesDir;
        }
    }
}
