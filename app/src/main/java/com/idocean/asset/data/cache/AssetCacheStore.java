package com.idocean.asset.data.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.data.db.AppDatabase;
import com.idocean.asset.data.repository.AssetFilterService;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.DashboardMetricsRepository;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AssetCacheStore {
    private static final String SOURCE_CACHE = "CACHE";
    private static final String SOURCE_DISK_CACHE = "DISK_CACHE";
    private static final String SCREEN = "Cache";
    private static final String FLOW_CACHE_IO = "cache_io";

    private final AssetFilterService assetFilterService;
    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final LogRepository logRepository;
    private final ExecutorService cacheIoExecutor;

    private List<Asset> inMemoryCache = new ArrayList<>();
    private String lastSource = SOURCE_CACHE;
    private boolean diskCacheLoaded;

    public AssetCacheStore(
            AssetFilterService assetFilterService,
            DashboardMetricsRepository dashboardMetricsRepository,
            LogRepository logRepository
    ) {
        this(assetFilterService, dashboardMetricsRepository, logRepository, Executors.newSingleThreadExecutor());
    }

    public AssetCacheStore(
            AssetFilterService assetFilterService,
            DashboardMetricsRepository dashboardMetricsRepository,
            LogRepository logRepository,
            ExecutorService cacheIoExecutor
    ) {
        this.assetFilterService = assetFilterService == null ? new AssetFilterService() : assetFilterService;
        this.dashboardMetricsRepository = dashboardMetricsRepository == null
                ? DashboardMetricsRepository.getInstance()
                : dashboardMetricsRepository;
        this.logRepository = logRepository == null ? LogRepository.getInstance() : logRepository;
        this.cacheIoExecutor = cacheIoExecutor == null ? Executors.newSingleThreadExecutor() : cacheIoExecutor;
    }

    public void loadCacheSnapshotAsync(Handler mainHandler, AssetRepository.CacheSnapshotCallback callback) {
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

    public synchronized List<Asset> getCachedAssets() {
        Context appContext = AppRuntimeContext.get();
        if (appContext != null) {
            try {
                return AppDatabase.getInstance(appContext).assetDao().getAll();
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(inMemoryCache);
    }

    public synchronized int getCachedAssetCount() {
        Context appContext = AppRuntimeContext.get();
        if (appContext != null) {
            try {
                return AppDatabase.getInstance(appContext).assetDao().count();
            } catch (Exception ignored) {}
        }
        return inMemoryCache.size();
    }

    public synchronized String getLastSource() {
        return lastSource;
    }

    public synchronized void clearMemoryCache() {
        inMemoryCache = new ArrayList<>();
        lastSource = SOURCE_CACHE;
        dashboardMetricsRepository.clear();
        clearDiskCacheAsync();
        logRepository.logInfo("CACHE", "Da xoa cache database tai san");
    }

    public synchronized void updateCache(List<Asset> assets, String source) {
        applyCacheSnapshot(assets, source);
        persistCacheAsync();
    }

    public synchronized void applyCacheSnapshot(List<Asset> assets, String source) {
        inMemoryCache = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        lastSource = source;
        dashboardMetricsRepository.clear();
    }

    public synchronized void replaceCachedAsset(Asset originalAsset, Asset updatedAsset) {
        if (updatedAsset == null) {
            return;
        }

        Context appContext = AppRuntimeContext.get();
        if (appContext != null) {
            try {
                AppDatabase.getInstance(appContext).assetDao().insertOrReplace(updatedAsset);
                logRepository.logInfo("CACHE", "Da cap nhat tai san vao SQLite database", updatedAsset.getAssetCode());
            } catch (Exception ignored) {}
        }

        Integer originalRowNumber = originalAsset == null ? null : originalAsset.getRowNumber();
        String normalizedOriginalCode = StringUtils.normalizeKey(originalAsset == null ? "" : originalAsset.getAssetCode());
        String normalizedOriginalTid = StringUtils.normalizeKey(originalAsset == null ? "" : originalAsset.getTid());
        String normalizedUpdatedCode = StringUtils.normalizeKey(updatedAsset.getAssetCode());
        String normalizedUpdatedTid = StringUtils.normalizeKey(updatedAsset.getTid());
        boolean matchedInMemory = false;
        for (int index = 0; index < inMemoryCache.size(); index++) {
            Asset current = inMemoryCache.get(index);
            boolean sameRowNumber = originalRowNumber != null
                    && current.getRowNumber() != null
                    && originalRowNumber.equals(current.getRowNumber());
            boolean sameOriginalTid = !normalizedOriginalTid.isEmpty()
                    && normalizedOriginalTid.equals(StringUtils.normalizeKey(current.getTid()));
            boolean sameOriginalCode = !normalizedOriginalCode.isEmpty()
                    && normalizedOriginalCode.equals(StringUtils.normalizeKey(current.getAssetCode()));
            boolean sameUpdatedTid = !normalizedUpdatedTid.isEmpty()
                    && normalizedUpdatedTid.equals(StringUtils.normalizeKey(current.getTid()));
            boolean sameUpdatedCode = !normalizedUpdatedCode.isEmpty()
                    && normalizedUpdatedCode.equals(StringUtils.normalizeKey(current.getAssetCode()));
            if (sameRowNumber || sameOriginalTid || sameOriginalCode || sameUpdatedTid || sameUpdatedCode) {
                inMemoryCache.set(index, updatedAsset);
                matchedInMemory = true;
                break;
            }
        }
        if (!matchedInMemory && !inMemoryCache.isEmpty()) {
            // Thêm tài sản mới vào cache nếu không tìm thấy để cập nhật
            inMemoryCache.add(updatedAsset);
        }

        if (appContext == null) {
            persistCacheAsync();
        }
    }

    public synchronized void ensureDiskCacheLoaded() {
        if (diskCacheLoaded) {
            return;
        }
        diskCacheLoaded = true;

        Context appContext = AppRuntimeContext.get();
        if (appContext == null) {
            return;
        }

        try {
            SharedPreferences prefs = appContext.getSharedPreferences("ido_asset_prefs", Context.MODE_PRIVATE);
            lastSource = prefs.getString("last_sync_source", SOURCE_DISK_CACHE);
            int count = AppDatabase.getInstance(appContext).assetDao().count();
            logRepository.logInfo(
                    "CACHE",
                    "Da khoi phuc cache tai san tu SQLite database",
                    count + " asset(s) | " + lastSource
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

    public synchronized AssetRepository.CacheSnapshot snapshotCurrentState() {
        List<Asset> assets = getCachedAssets();
        return new AssetRepository.CacheSnapshot(assets, lastSource, assetFilterService.buildDistinctValueMap(assets));
    }

    public void persistCacheAsync() {
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
        if (snapshot.isEmpty()) {
            return;
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

    public void clearDiskCacheAsync() {
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

    public void persistCacheNow(List<Asset> assets, String source) throws IOException {
        Context appContext = AppRuntimeContext.get();
        if (appContext == null) {
            return;
        }
        persistCacheNow(appContext, assets, source);
    }

    public void persistCacheNow(Context appContext, List<Asset> assets, String source) throws IOException {
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
        
        synchronized (this) {
            inMemoryCache = new ArrayList<>();
        }
    }
}
