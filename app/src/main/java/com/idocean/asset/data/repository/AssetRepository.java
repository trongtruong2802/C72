package com.idocean.asset.data.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.mapper.AssetApiResponseParser;
import com.idocean.asset.importer.AssetImportManager;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetFilterCriteria;
import com.idocean.asset.model.AssetSyncQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Runtime repository: chi cache trong memory khi app dang chay.
 */
public class AssetRepository {
    private static final String SYNC_TAG = "IDO_SYNC";
    private static AssetRepository instance;
    private static final int DEFAULT_SYNC_BATCH_SIZE = 300;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AssetImportManager importManager = new AssetImportManager();
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private final AssetFilterService assetFilterService = new AssetFilterService();
    private final LogRepository logRepository = LogRepository.getInstance();
    private final AssetCacheStore assetCacheStore = new AssetCacheStore(
            new AssetDiskCacheStore(),
            assetFilterService,
            DashboardMetricsRepository.getInstance(),
            logRepository
    );
    /*
     * Rollback-only legacy sync implementations. Active sync now goes through
     * AssetSyncLegacyAdapterV2 -> AssetSyncCoordinatorV2, but these fields stay for the
     * current rollback window until the final sync cleanup phase removes them together.
     */
    @SuppressWarnings("unused")
    private final AssetSyncService assetSyncService = new AssetSyncService(
            mainHandler,
            logRepository,
            new AssetSyncService.SyncHost() {
                @Override
                public void logSyncPhase(long syncStartedAt, String phase, String detail) {
                    AssetRepository.this.logSyncPhase(syncStartedAt, phase, detail);
                }

                @Override
                public void applyCacheSnapshot(List<Asset> assets, String source) {
                    AssetRepository.this.applyCacheSnapshot(assets, source);
                }

                @Override
                public void persistCacheNow(List<Asset> assets, String source) throws IOException {
                    AssetRepository.this.persistCacheNow(assets, source);
                }

                @Override
                public List<Asset> fetchAssetsWithLocalFilterFallback(AssetSyncQuery query, long syncStartedAt) throws IOException {
                    return AssetRepository.this.fetchAssetsWithLocalFilterFallback(query, syncStartedAt);
                }
            }
    );
    @SuppressWarnings("unused")
    private final AssetSyncV2Service assetSyncV2Service = new AssetSyncV2Service(
            mainHandler,
            logRepository,
            new AssetSyncV2Service.SyncHost() {
                @Override
                public void logSyncPhase(long syncStartedAt, String phase, String detail) {
                    AssetRepository.this.logSyncPhase(syncStartedAt, phase, detail);
                }

                @Override
                public void applyCacheSnapshot(List<Asset> assets, String source) {
                    AssetRepository.this.applyCacheSnapshot(assets, source);
                }

                @Override
                public void persistCacheNow(List<Asset> assets, String source) throws IOException {
                    AssetRepository.this.persistCacheNow(assets, source);
                }
            }
    );
    private final AssetSyncLegacyAdapterV2 assetSyncLegacyAdapterV2 = new AssetSyncLegacyAdapterV2();
    private final AssetSyncRequestBuilder assetSyncRequestBuilder = new AssetSyncRequestBuilder();
    private final AssetMutationService assetMutationService = new AssetMutationService(
            mainHandler,
            logRepository,
            new AssetMutationService.MutationHost() {
                @Override
                public void replaceCachedAsset(Asset originalAsset, Asset updatedAsset) {
                    AssetRepository.this.replaceCachedAsset(originalAsset, updatedAsset);
                }

                @Override
                public void persistCacheAsync() {
                    AssetRepository.this.persistCacheAsync();
                }
            }
    );

    private AssetRepository() {
    }

    public static synchronized AssetRepository getInstance() {
        if (instance == null) {
            instance = new AssetRepository();
        }
        return instance;
    }

    public void loadCacheSnapshotAsync(CacheSnapshotCallback callback) {
        assetCacheStore.loadCacheSnapshotAsync(mainHandler, callback);
    }

    public synchronized List<Asset> getCachedAssets() {
        ensureDiskCacheLoaded();
        return assetCacheStore.getCachedAssets();
    }

    public synchronized int getCachedAssetCount() {
        ensureDiskCacheLoaded();
        return assetCacheStore.getCachedAssetCount();
    }

    public synchronized String getLastSource() {
        ensureDiskCacheLoaded();
        return assetCacheStore.getLastSource();
    }

    public Asset findAsset(String code, String tid) {
        ensureDiskCacheLoaded();
        String normalizedCode = normalizeKey(code);
        String normalizedTid = normalizeKey(tid);
        for (Asset asset : getCachedAssets()) {
            if (!normalizedCode.isEmpty() && normalizedCode.equals(normalizeKey(asset.getAssetCode()))) {
                return asset;
            }
            if (!normalizedTid.isEmpty() && normalizedTid.equals(normalizeKey(asset.getTid()))) {
                return asset;
            }
        }
        return null;
    }

    public Asset findAssetByAnyIdentifier(String identifier) {
        ensureDiskCacheLoaded();
        String normalized = normalizeKey(identifier);
        if (normalized.isEmpty()) {
            return null;
        }
        for (Asset asset : getCachedAssets()) {
            if (normalized.equals(normalizeKey(asset.getAssetCode()))
                    || normalized.equals(normalizeKey(asset.getTid()))) {
                return asset;
            }
        }
        return null;
    }

    public List<Asset> filterAssets(AssetFilterCriteria criteria) {
        ensureDiskCacheLoaded();
        return assetFilterService.filterAssets(getCachedAssets(), criteria);
    }

    public List<Asset> filterAssets(List<Asset> assets, AssetFilterCriteria criteria) {
        return assetFilterService.filterAssets(assets, criteria);
    }

    public List<String> collectDistinctValues(String fieldName) {
        ensureDiskCacheLoaded();
        return assetFilterService.collectDistinctValues(getCachedAssets(), fieldName);
    }

    public void loadAssetsFromApi(final AssetRepositoryCallback callback) {
        syncAssetsFromApi(
                new AssetSyncQuery("", "", "", DEFAULT_SYNC_BATCH_SIZE),
                AssetSyncEntrypointMode.FULL,
                new AssetSyncProgressCallback() {
            @Override
            public void onCountReady(int totalCount, int batchSize, String description) {
            }

            @Override
            public void onProgress(int loadedCount, int totalCount, int batchIndex, int totalBatches) {
            }

            @Override
            public void onSuccess(List<Asset> assets, String message) {
                if (callback != null) {
                    callback.onSuccess(assets, message);
                }
            }

            @Override
            public void onError(AssetSyncErrorType errorType, String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    public void syncAssetsFromApi(AssetSyncQuery query, AssetSyncProgressCallback callback) {
        syncAssetsFromApi(query, AssetSyncEntrypointMode.inferFromQuery(query), callback);
    }

    public void syncAssetsFromApi(
            AssetSyncQuery query,
            AssetSyncEntrypointMode entrypointMode,
            AssetSyncProgressCallback callback
    ) {
        final AssetSyncQuery safeQuery = query == null
                ? new AssetSyncQuery("", "", "", DEFAULT_SYNC_BATCH_SIZE)
                : query;
        final AssetSyncEntrypointMode safeEntrypointMode = entrypointMode == null
                ? AssetSyncEntrypointMode.inferFromQuery(safeQuery)
                : entrypointMode;
        final long syncStartedAt = SystemClock.elapsedRealtime();
        logSyncPhase(
                syncStartedAt,
                "click received",
                "mode=" + safeEntrypointMode.name().toLowerCase(Locale.ROOT)
                        + " | source=" + assetCacheStore.getLastSource()
        );
        if (!safeQuery.getLocations().isEmpty()) {
            logSyncPhase(
                    syncStartedAt,
                    "location filters",
                    "selected=" + joinValues(safeQuery.getLocations())
                            + " | expanded=" + joinValues(safeQuery.getRequestLocationValues())
            );
        }
        logRepository.logInfo(
                "LOAD_API",
                "Bat dau tai danh sach tai san tu API",
                "Sync V2 active | mode=" + safeEntrypointMode.name() + " | " + safeQuery.describe()
        );
        syncExecutor.execute(() -> {
            ensureDiskCacheLoaded();
            runSyncV2ThroughCoordinator(safeQuery, safeEntrypointMode, callback, syncStartedAt);
        });
    }

    private void runSyncV2ThroughCoordinator(
            AssetSyncQuery query,
            AssetSyncEntrypointMode entrypointMode,
            AssetSyncProgressCallback callback,
            long syncStartedAt
    ) {
        try {
            logSyncPhase(
                    syncStartedAt,
                    "sync v2 start",
                    "mode=" + entrypointMode.name().toLowerCase(Locale.ROOT) + " | " + query.describe()
            );
            AssetSyncCoordinatorV2.SyncResult result = assetSyncLegacyAdapterV2.execute(
                    query,
                    entrypointMode,
                    new AssetSyncProgressCallback() {
                        @Override
                        public void onCountReady(int totalCount, int batchSize, String description) {
                            dispatchCountReady(callback, totalCount, batchSize, description);
                        }

                        @Override
                        public void onProgress(int loadedCount, int totalCount, int batchIndex, int totalBatches) {
                            dispatchBatchProgress(callback, loadedCount, totalCount, batchIndex, totalBatches);
                        }

                        @Override
                        public void onSuccess(List<Asset> assets, String message) {
                        }

                        @Override
                        public void onError(AssetSyncErrorType errorType, String message) {
                        }
                    }
            );
            logSyncPhase(
                    syncStartedAt,
                    "sync v2 result",
                    "requests=" + result.getRequestCount() + " | assets=" + result.getAssets().size()
            );

            if (query.hasAnyFilter() && result.getAssets().isEmpty()) {
                deliverEmptySyncResult(callback, query, syncStartedAt);
                return;
            }

            completeSyncWithAssets(
                    callback,
                    result.getAssets(),
                    AssetSyncCoordinatorV2.UNKNOWN_TOTAL_COUNT,
                    syncStartedAt
            );
        } catch (AssetSyncCoordinatorV2.SyncFailureException failureException) {
            logSyncPhase(syncStartedAt, "sync failed", failureException.getMessage());
            logRepository.logError(
                    "LOAD_API",
                    "Sync V2 that bai",
                    "mode=" + entrypointMode.name() + " | " + failureException.getMessage()
            );
            dispatchSyncError(callback, failureException.getErrorType(), failureException.getMessage());
            logSyncPhase(syncStartedAt, "total duration", (SystemClock.elapsedRealtime() - syncStartedAt) + "ms");
        } catch (IllegalArgumentException illegalArgumentException) {
            logSyncPhase(syncStartedAt, "sync failed", illegalArgumentException.getMessage());
            logRepository.logError("LOAD_API", "Sync V2 khong hop le", illegalArgumentException.getMessage());
            dispatchSyncError(callback, AssetSyncErrorType.UNKNOWN, illegalArgumentException.getMessage());
            logSyncPhase(syncStartedAt, "total duration", (SystemClock.elapsedRealtime() - syncStartedAt) + "ms");
        } catch (IOException ioException) {
            logSyncPhase(syncStartedAt, "sync failed", ioException.getMessage());
            logRepository.logError("LOAD_API", "Khong the luu cache noi bo", ioException.getMessage());
            dispatchSyncError(callback, AssetSyncCoordinatorV2.classifyIOException(ioException), ioException.getMessage());
            logSyncPhase(syncStartedAt, "total duration", (SystemClock.elapsedRealtime() - syncStartedAt) + "ms");
        }
    }

    private AssetApiResponseParser.AssetPageResult fetchAssetPage(
            AssetSyncQuery query,
            Integer limit,
            Integer offset,
            long syncStartedAt,
            String phaseLabel
    ) throws IOException {
        return fetchAssetPage(query, null, limit, offset, syncStartedAt, phaseLabel);
    }

    private AssetApiResponseParser.AssetPageResult fetchAssetPage(
            AssetSyncQuery query,
            String locationOverride,
            Integer limit,
            Integer offset,
            long syncStartedAt,
            String phaseLabel
    ) throws IOException {
        String requestUrl = assetSyncRequestBuilder.buildGetDbUrl(
                ApiClient.getResolvedBaseUrl(),
                query,
                locationOverride,
                limit,
                offset
        );
        logSyncPhase(syncStartedAt, "request " + phaseLabel, requestUrl);
        Response<ResponseBody> response = ApiClient.getAssetApiService().getAssets(requestUrl).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP " + response.code());
        }

        logSyncPhase(syncStartedAt, "response " + phaseLabel, "HTTP " + response.code());
        logSyncPhase(syncStartedAt, "read body start", phaseLabel);
        String rawBody = readResponseBody(response.body());
        logSyncPhase(syncStartedAt, "read body end", phaseLabel + " | chars=" + rawBody.length());

        logSyncPhase(syncStartedAt, "parse start", phaseLabel);
        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawBody);
        logSyncPhase(
                syncStartedAt,
                "parse end",
                phaseLabel + " | records=" + pageResult.assetArray.size() + " | total=" + pageResult.totalCount
        );
        return pageResult;
    }

    private List<Asset> fetchAssetsWithoutPagination(
            AssetSyncQuery query,
            long syncStartedAt,
            String phaseLabel
    ) throws IOException {
        return fetchAssetsWithoutPagination(query, null, syncStartedAt, phaseLabel);
    }

    private List<Asset> fetchAssetsWithoutPagination(
            AssetSyncQuery query,
            String locationOverride,
            long syncStartedAt,
            String phaseLabel
    ) throws IOException {
        AssetApiResponseParser.AssetPageResult pageResult =
                fetchAssetPage(query, locationOverride, null, null, syncStartedAt, phaseLabel);
        return AssetApiResponseParser.mapAssetsAllowEmpty(pageResult.assetArray);
    }

    private List<Asset> fetchAssetsWithLocalFilterFallback(AssetSyncQuery query, long syncStartedAt) throws IOException {
        AssetSyncQuery unfilteredQuery = new AssetSyncQuery("", "", "", DEFAULT_SYNC_BATCH_SIZE);
        List<Asset> allAssets = fetchAssetsWithoutPagination(
                unfilteredQuery,
                syncStartedAt,
                "unpaged local filter fallback"
        );
        List<Asset> filteredAssets = filterAssetsBySyncQuery(allAssets, query);
        logSyncPhase(
                syncStartedAt,
                "local filter fallback result",
                "all=" + allAssets.size() + " | matched=" + filteredAssets.size()
        );
        return filteredAssets;
    }

    private List<Asset> filterAssetsBySyncQuery(List<Asset> assets, AssetSyncQuery query) {
        return assetFilterService.filterAssetsBySyncQuery(assets, query);
    }

    private void completeSyncWithAssets(
            AssetSyncProgressCallback callback,
            List<Asset> assets,
            int totalCount,
            long syncStartedAt
    ) throws IOException {
        List<Asset> safeAssets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        logSyncPhase(syncStartedAt, "update cache start", "assets=" + safeAssets.size());
        applyCacheSnapshot(safeAssets, "API");
        logSyncPhase(syncStartedAt, "update cache end", "cached=" + safeAssets.size());

        logSyncPhase(syncStartedAt, "persist cache start", "assets=" + safeAssets.size());
        persistCacheNow(safeAssets, "API");
        logSyncPhase(syncStartedAt, "persist cache end", "assets=" + safeAssets.size());

        logRepository.logInfo("LOAD_API", "Da tai du lieu tai san tu API", safeAssets.size() + " asset(s)");
        logSyncPhase(syncStartedAt, "deliver result to UI", "assets=" + safeAssets.size());
        dispatchSyncSuccess(
                callback,
                safeAssets,
                totalCount >= 0
                        ? "Da tai " + safeAssets.size() + " / " + totalCount + " tai san tu API."
                        : "Da tai " + safeAssets.size() + " tai san tu API."
        );
        logSyncPhase(syncStartedAt, "total duration", (SystemClock.elapsedRealtime() - syncStartedAt) + "ms");
    }

    private void deliverEmptySyncResult(
            AssetSyncProgressCallback callback,
            AssetSyncQuery query,
            long syncStartedAt
    ) throws IOException {
        logSyncPhase(syncStartedAt, "update cache start", "assets=0");
        applyCacheSnapshot(Collections.emptyList(), "API");
        logSyncPhase(syncStartedAt, "update cache end", "cached=0");

        logSyncPhase(syncStartedAt, "persist cache start", "assets=0");
        persistCacheNow(Collections.emptyList(), "API");
        logSyncPhase(syncStartedAt, "persist cache end", "assets=0");

        logRepository.logInfo("LOAD_API", "Khong co tai san phu hop voi bo loc", query == null ? "" : query.describe());
        logSyncPhase(syncStartedAt, "deliver result to UI", "assets=0");
        dispatchSyncSuccess(callback, Collections.emptyList(), "Khong co tai san phu hop voi bo loc da chon.");
        logSyncPhase(syncStartedAt, "total duration", (SystemClock.elapsedRealtime() - syncStartedAt) + "ms");
    }


    public void importAssetsFromCsv(final Context context, final Uri uri, final AssetRepositoryCallback callback) {
        ensureDiskCacheLoaded();
        new Thread(() -> {
            try {
                List<Asset> assets = importManager.importFromUri(context, uri);
                if (assets == null || assets.isEmpty()) {
                    logRepository.logError("IMPORT_FILE", "File CSV tai san khong co du lieu hop le");
                    dispatchError(callback, "File CSV khong co dong du lieu hop le.");
                    return;
                }
                updateCache(assets, "CSV");
                logRepository.logInfo("IMPORT_FILE", "Da import danh sach tai san tu CSV", assets.size() + " asset(s)");
                dispatchSuccess(callback, assets, "Da import " + assets.size() + " tai san tu CSV.");
            } catch (IllegalArgumentException illegalArgumentException) {
                logRepository.logError("IMPORT_FILE", "Import CSV tai san that bai", illegalArgumentException.getMessage());
                dispatchError(callback, illegalArgumentException.getMessage());
            } catch (Exception exception) {
                logRepository.logError("IMPORT_FILE", "Khong the import CSV tai san", exception.getMessage());
                dispatchError(callback, "Khong the import CSV. Vui long kiem tra dinh dang file.");
            }
        }).start();
    }

    public synchronized void clearMemoryCache() {
        ensureDiskCacheLoaded();
        assetCacheStore.clearMemoryCache();
    }

    public void updateAsset(Asset originalAsset, Asset asset, AssetUpdateCallback callback) {
        ensureDiskCacheLoaded();
        assetMutationService.updateAsset(originalAsset, asset, callback);
    }

    public void handoverAsset(Asset originalAsset, Asset asset, String handoverDate, AssetUpdateCallback callback) {
        ensureDiskCacheLoaded();
        assetMutationService.handoverAsset(originalAsset, asset, handoverDate, callback);
    }

    private synchronized void updateCache(List<Asset> assets, String source) {
        assetCacheStore.updateCache(assets, source);
    }

    private synchronized void applyCacheSnapshot(List<Asset> assets, String source) {
        assetCacheStore.applyCacheSnapshot(assets, source);
    }

    private synchronized void replaceCachedAsset(Asset originalAsset, Asset updatedAsset) {
        assetCacheStore.replaceCachedAsset(originalAsset, updatedAsset);
    }

    private synchronized void ensureDiskCacheLoaded() {
        assetCacheStore.ensureDiskCacheLoaded();
    }

    private void persistCacheAsync() {
        assetCacheStore.persistCacheAsync();
    }

    private void persistCacheNow(List<Asset> assets, String source) throws IOException {
        assetCacheStore.persistCacheNow(assets, source);
    }

    private static String readResponseBody(ResponseBody responseBody) {
        if (responseBody == null) {
            return "";
        }
        try {
            String raw = responseBody.string();
            return raw == null ? "" : raw.trim();
        } catch (IOException exception) {
            return "";
        }
    }

    private void logSyncPhase(long syncStartedAt, String phase, String detail) {
        long elapsed = SystemClock.elapsedRealtime() - syncStartedAt;
        String safeDetail = detail == null || detail.trim().isEmpty() ? "" : " | " + detail.trim();
        String message = "[SYNC] " + phase + " | +" + elapsed + "ms | thread=" + Thread.currentThread().getName() + safeDetail;
        Log.d(SYNC_TAG, message);
        logRepository.logInfo("SYNC", phase, "+" + elapsed + "ms | " + Thread.currentThread().getName() + safeDetail);
    }


    private void dispatchSuccess(AssetRepositoryCallback callback, List<Asset> assets, String message) {
        if (callback == null) {
            return;
        }
        List<Asset> safeAssets = assets == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(assets);
        mainHandler.post(() -> callback.onSuccess(safeAssets, message));
    }

    private void dispatchError(AssetRepositoryCallback callback, String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onError(message));
    }

    private void dispatchCountReady(AssetSyncProgressCallback callback, int totalCount, int batchSize, String description) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onCountReady(totalCount, batchSize, description));
    }

    private void dispatchBatchProgress(
            AssetSyncProgressCallback callback,
            int loadedCount,
            int totalCount,
            int batchIndex,
            int totalBatches
    ) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onProgress(loadedCount, totalCount, batchIndex, totalBatches));
    }

    private void dispatchSyncSuccess(AssetSyncProgressCallback callback, List<Asset> assets, String message) {
        if (callback == null) {
            return;
        }
        List<Asset> safeAssets = assets == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(assets));
        mainHandler.post(() -> callback.onSuccess(safeAssets, message));
    }

    private void dispatchSyncError(AssetSyncProgressCallback callback, AssetSyncErrorType errorType, String message) {
        if (callback == null) {
            return;
        }
        AssetSyncErrorType safeType = errorType == null ? AssetSyncErrorType.UNKNOWN : errorType;
        String safeMessage = message == null ? "" : message;
        mainHandler.post(() -> callback.onError(safeType, safeMessage));
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String safeValue = safeTrim(value);
            if (safeValue.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(safeValue);
        }
        return builder.toString();
    }

    public interface CacheSnapshotCallback {
        void onReady(CacheSnapshot snapshot);
    }

    public static final class CacheSnapshot {
        private final List<Asset> assets;
        private final String source;
        private final Map<String, List<String>> distinctValues;

        CacheSnapshot(List<Asset> assets, String source, Map<String, List<String>> distinctValues) {
            this.assets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
            this.source = source == null ? "CACHE" : source;
            this.distinctValues = distinctValues == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(distinctValues);
        }

        public List<Asset> getAssets() {
            return new ArrayList<>(assets);
        }

        public int getAssetCount() {
            return assets.size();
        }

        public String getSource() {
            return source;
        }

        public List<String> getDistinctValues(String fieldName) {
            List<String> values = distinctValues.get(fieldName);
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        }
    }
}
