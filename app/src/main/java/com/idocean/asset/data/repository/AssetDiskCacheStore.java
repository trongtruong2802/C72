package com.idocean.asset.data.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.idocean.asset.model.Asset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Luu cache tai san trong bo nho noi bo de app mo lai van co du lieu runtime gan nhat.
 */
final class AssetDiskCacheStore {
    private static final String FILE_NAME = "ido_asset_runtime_cache.json";

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    CacheSnapshot read(Context context) throws IOException {
        File cacheFile = resolveFile(context);
        if (!cacheFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(cacheFile), StandardCharsets.UTF_8))) {
            StringBuilder rawJson = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                rawJson.append(line);
            }
            return deserialize(rawJson.toString());
        } catch (RuntimeException runtimeException) {
            throw new IOException("Khong doc duoc file cache tai san noi bo.", runtimeException);
        }
    }

    void write(Context context, List<Asset> assets, String source) throws IOException {
        File cacheFile = resolveFile(context);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(cacheFile, false), StandardCharsets.UTF_8))) {
            writer.write(serialize(assets, source, System.currentTimeMillis()));
        }
    }

    String serialize(List<Asset> assets, String source, long cachedAt) {
        SnapshotDto snapshotDto = new SnapshotDto();
        snapshotDto.source = safe(source);
        snapshotDto.cachedAt = cachedAt;
        snapshotDto.assets = new ArrayList<>();
        if (assets != null) {
            for (Asset asset : assets) {
                if (asset != null) {
                    snapshotDto.assets.add(AssetDto.fromAsset(asset));
                }
            }
        }
        return gson.toJson(snapshotDto);
    }

    CacheSnapshot deserialize(String rawJson) throws IOException {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return null;
        }

        SnapshotDto snapshotDto;
        try {
            snapshotDto = gson.fromJson(rawJson, SnapshotDto.class);
        } catch (RuntimeException runtimeException) {
            throw new IOException("Khong doc duoc file cache tai san noi bo.", runtimeException);
        }
        if (snapshotDto == null || snapshotDto.assets == null || snapshotDto.assets.isEmpty()) {
            return null;
        }

        List<Asset> assets = new ArrayList<>();
        for (AssetDto assetDto : snapshotDto.assets) {
            if (assetDto != null) {
                assets.add(assetDto.toAsset());
            }
        }
        if (assets.isEmpty()) {
            return null;
        }
        return new CacheSnapshot(assets, safe(snapshotDto.source), snapshotDto.cachedAt == null ? 0L : snapshotDto.cachedAt);
    }

    void clear(Context context) {
        File cacheFile = resolveFile(context);
        if (cacheFile.exists()) {
            // Best effort cleanup. Failure here should not break app flow.
            cacheFile.delete();
        }
    }

    private File resolveFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    static final class CacheSnapshot {
        final List<Asset> assets;
        final String source;
        final long cachedAt;

        CacheSnapshot(List<Asset> assets, String source, long cachedAt) {
            this.assets = assets == null ? new ArrayList<>() : assets;
            this.source = source == null ? "" : source;
            this.cachedAt = cachedAt;
        }
    }

    private static final class SnapshotDto {
        String source;
        Long cachedAt;
        List<AssetDto> assets;
    }

    private static final class AssetDto {
        Integer rowNumber;
        String assetCode;
        String tid;
        String oldCode;
        String oldSerial;
        String assetName;
        String assetType;
        String serialNumber;
        String department;
        String assignedUser;
        String location;
        String inventoryStatus;
        String assetCondition;
        String tagDate;
        String tagBy;
        String note;
        String source;

        static AssetDto fromAsset(Asset asset) {
            AssetDto dto = new AssetDto();
            dto.rowNumber = asset.getRowNumber();
            dto.assetCode = asset.getAssetCode();
            dto.tid = asset.getTid();
            dto.oldCode = asset.getOldCode();
            dto.oldSerial = asset.getOldSerial();
            dto.assetName = asset.getAssetName();
            dto.assetType = asset.getAssetType();
            dto.serialNumber = asset.getSerialNumber();
            dto.department = asset.getDepartment();
            dto.assignedUser = asset.getAssignedUser();
            dto.location = asset.getLocation();
            dto.inventoryStatus = asset.getInventoryStatus();
            dto.assetCondition = asset.getAssetCondition();
            dto.tagDate = asset.getTagDate();
            dto.tagBy = asset.getTagBy();
            dto.note = asset.getNote();
            dto.source = asset.getSource();
            return dto;
        }

        Asset toAsset() {
            return new Asset(
                    rowNumber,
                    assetCode,
                    tid,
                    oldCode,
                    oldSerial,
                    assetName,
                    assetType,
                    serialNumber,
                    department,
                    assignedUser,
                    location,
                    inventoryStatus,
                    assetCondition,
                    tagDate,
                    tagBy,
                    note,
                    source
            );
        }
    }
}
