package com.idocean.asset.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.data.db.AppDatabase;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.model.Asset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AssetCacheStore {
    private static final String SOURCE_CACHE = "CACHE";
    private static final String SOURCE_DISK_CACHE = "DISK_CACHE";
    private static final String SCREEN = "Cache";
    private static final String FLOW_CACHE_IO = "cache_io";

    private final AssetDiskCacheStore diskCacheStore;
    private final AssetFilterService assetFilterService;
    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final LogRepository logRepository;
    private final ExecutorService cacheIoExecutor;

    private List<Asset> inMemoryCache = new ArrayList<>();
    private String lastSource = SOURCE_CACHE;
    private boolean diskCacheLoaded;

    AssetCacheStore(
            AssetDiskCacheStore diskCacheStore,
            AssetFilterService assetFilterService,
            DashboardMetricsRepository dashboardMetricsRepository,
            LogRepository logRepository
    ) {
        this(diskCacheStore, assetFilterService, dashboardMetricsRepository, logRepository, Executors.newSingleThreadExecutor());
    }

    AssetCacheStore(
            AssetDiskCacheStore diskCacheStore,
            AssetFilterService assetFilterService,
            DashboardMetricsRepository dashboardMetricsRepository,
            LogRepository logRepository,
            ExecutorService cacheIoExecutor
    ) {
        this.diskCacheStore = diskCacheStore == null ? new AssetDiskCacheStore() : diskCacheStore;
        this.assetFilterService = assetFilterService == null ? new AssetFilterService() : assetFilterService;
        this.dashboardMetricsRepository = dashboardMetricsRepository == null
                ? DashboardMetricsRepository.getInstance()
                : dashboardMetricsRepository;
        this.logRepository = logRepository == null ? LogRepository.getInstance() : logRepository;
        this.cacheIoExecutor = cacheIoExecutor == null ? Executors.newSingleThreadExecutor() : cacheIoExecutor;
    }

    void loadCacheSnapshotAsync(Handler mainHandler, AssetRepository.CacheSnapshotCallback callback) {
        cacheIoExecutor.execute(() -> {
            PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_CACHE_IO, "snapshot_requested", "mode=async");
            trace.markStart(logRepository);
            ensureDiskCacheLoaded();
            AssetRepository.CacheSnapshot snapshot = snapshotCurrentState();
            trace.finish(
                    logRepository,
                    "snapshot_ready",
                    "assetCount=" + snapshot.getAssetCount() + " | source=" + snapshot.getSource()
            );
            if (callback == null || mainHandler == null) {
                return;
            }
            mainHandler.post(() -> callback.onReady(snapshot));
        });
    }

    synchronized List<Asset> getCachedAssets() {
        return new ArrayList<>(inMemoryCache);
    }

    synchronized int getCachedAssetCount() {
        return inMemoryCache.size();
    }

    synchronized String getLastSource() {
        return lastSource;
    }

    synchronized void clearMemoryCache() {
        inMemoryCache = new ArrayList<>();
        lastSource = SOURCE_CACHE;
        dashboardMetricsRepository.clear();
        clearDiskCacheAsync();
        logRepository.logInfo("CACHE", "Da xoa cache database tai san");
    }

    synchronized void updateCache(List<Asset> assets, String source) {
        applyCacheSnapshot(assets, source);
        persistCacheAsync();
    }

    synchronized void applyCacheSnapshot(List<Asset> assets, String source) {
        inMemoryCache = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        lastSource = source;
        dashboardMetricsRepository.clear();
    }

    synchronized void replaceCachedAsset(Asset originalAsset, Asset updatedAsset) {
        if (updatedAsset == null) {
            return;
        }
        Integer originalRowNumber = originalAsset == null ? null : originalAsset.getRowNumber();
        String normalizedOriginalCode = normalizeKey(originalAsset == null ? "" : originalAsset.getAssetCode());
        String normalizedOriginalTid = normalizeKey(originalAsset == null ? "" : originalAsset.getTid());
        String normalizedUpdatedCode = normalizeKey(updatedAsset.getAssetCode());
        String normalizedUpdatedTid = normalizeKey(updatedAsset.getTid());
        for (int index = 0; index < inMemoryCache.size(); index++) {
            Asset current = inMemoryCache.get(index);
            boolean sameRowNumber = originalRowNumber != null
                    && current.getRowNumber() != null
                    && originalRowNumber.equals(current.getRowNumber());
            boolean sameOriginalTid = !normalizedOriginalTid.isEmpty()
                    && normalizedOriginalTid.equals(normalizeKey(current.getTid()));
            boolean sameOriginalCode = !normalizedOriginalCode.isEmpty()
                    && normalizedOriginalCode.equals(normalizeKey(current.getAssetCode()));
            boolean sameUpdatedTid = !normalizedUpdatedTid.isEmpty()
                    && normalizedUpdatedTid.equals(normalizeKey(current.getTid()));
            boolean sameUpdatedCode = !normalizedUpdatedCode.isEmpty()
                    && normalizedUpdatedCode.equals(normalizeKey(current.getAssetCode()));
            if (sameRowNumber || sameOriginalTid || sameOriginalCode || sameUpdatedTid || sameUpdatedCode) {
                inMemoryCache.set(index, updatedAsset);
                persistCacheAsync();
                return;
            }
        }
        // Thêm tài sản mới vào cache nếu không tìm thấy để cập nhật
        inMemoryCache.add(updatedAsset);
        persistCacheAsync();
    }

    synchronized void ensureDiskCacheLoaded() {
        if (diskCacheLoaded) {
            return;
        }
        diskCacheLoaded = true;

        Context appContext = AppRuntimeContext.get();
        if (appContext == null) {
            return;
        }

        try {
            List<Asset> dbAssets = AppDatabase.getInstance(appContext).assetDao().getAll();
            if (dbAssets == null || dbAssets.isEmpty()) {
                return;
            }
            inMemoryCache = new ArrayList<>(dbAssets);
            SharedPreferences prefs = appContext.getSharedPreferences("ido_asset_prefs", Context.MODE_PRIVATE);
            lastSource = prefs.getString("last_sync_source", SOURCE_DISK_CACHE);
            logRepository.logInfo(
                    "CACHE",
                    "Da khoi phuc cache tai san tu SQLite database",
                    dbAssets.size() + " asset(s) | " + lastSource
            );
        } catch (Exception exception) {
            DebugEventLogger.error(
                    logRepository,
                    SCREEN,
                    FLOW_CACHE_IO,
                    "cache_read_failed",
                    AppErrorCodes.CACHE_READ_FAILED,
                    exception.getMessage()
            );
            logRepository.logError("CACHE", "Khong doc duoc cache tai san tu database", exception.getMessage());
            clearDiskCacheAsync();
        }
    }

    synchronized AssetRepository.CacheSnapshot snapshotCurrentState() {
        List<Asset> assets = new ArrayList<>(inMemoryCache);
        return new AssetRepository.CacheSnapshot(assets, lastSource, assetFilterService.buildDistinctValueMap(assets));
    }

    void persistCacheAsync() {
        Context appContext = AppRuntimeContext.get();
        if (appContext == null) {
            return;
        }
        List<Asset> snapshot;
        String source;
        synchronized (this) {
            snapshot = new ArrayList<>(inMemoryCache);
            source = lastSource;
        }
        cacheIoExecutor.execute(() -> {
            try {
                persistCacheNow(appContext, snapshot, source);
            } catch (Exception exception) {
                DebugEventLogger.error(
                        logRepository,
                        SCREEN,
                        FLOW_CACHE_IO,
                        "cache_write_failed",
                        AppErrorCodes.CACHE_WRITE_FAILED,
                        exception.getMessage()
                );
                logRepository.logError("CACHE", "Khong luu duoc cache tai san vao database", exception.getMessage());
            }
        });
    }

    void clearDiskCacheAsync() {
        Context appContext = AppRuntimeContext.get();
        if (appContext == null) {
            return;
        }
        cacheIoExecutor.execute(() -> {
            AppDatabase.getInstance(appContext).assetDao().clear();
            appContext.getSharedPreferences("ido_asset_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .remove("last_sync_source")
                    .apply();
        });
    }

    void persistCacheNow(List<Asset> assets, String source) throws IOException {
        Context appContext = AppRuntimeContext.get();
        if (appContext == null) {
            return;
        }
        persistCacheNow(appContext, assets, source);
    }

    void persistCacheNow(Context appContext, List<Asset> assets, String source) throws IOException {
        AppDatabase db = AppDatabase.getInstance(appContext);
        db.runInTransaction(() -> {
            db.assetDao().clear();
            if (assets != null && !assets.isEmpty()) {
                db.assetDao().insertOrReplaceAll(assets);
            }
        });
        appContext.getSharedPreferences("ido_asset_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("last_sync_source", source)
                .apply();
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
