package com.idocean.asset.data.repository;

import android.os.Handler;
import android.os.SystemClock;

import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.mapper.AssetApiResponseParser;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetSyncQuery;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Legacy pre-coordinator sync implementation kept temporarily for rollback-only verification.
 */
@Deprecated
final class AssetSyncService {
    private static final String SYNC_SOURCE = "API";
    private static final int DEFAULT_SYNC_BATCH_SIZE = 300;

    interface SyncHost {
        void logSyncPhase(long syncStartedAt, String phase, String detail);

        void applyCacheSnapshot(List<Asset> assets, String source);

        void persistCacheNow(List<Asset> assets, String source) throws IOException;

        List<Asset> fetchAssetsWithLocalFilterFallback(AssetSyncQuery query, long syncStartedAt) throws IOException;
    }

    private final Handler mainHandler;
    private final LogRepository logRepository;
    private final SyncHost host;
    private final AssetSyncRequestBuilder requestBuilder = new AssetSyncRequestBuilder();

    AssetSyncService(Handler mainHandler, LogRepository logRepository, SyncHost host) {
        this.mainHandler = mainHandler;
        this.logRepository = logRepository;
        this.host = host;
    }

    void performBatchedSync(AssetSyncQuery query, AssetSyncProgressCallback callback, long syncStartedAt) {
        try {
            int batchSize = query.getBatchSize() > 0 ? query.getBatchSize() : DEFAULT_SYNC_BATCH_SIZE;

            // Dung luon page dau tien lam preview de giam 1 round-trip mang cho moi lan sync.
            host.logSyncPhase(syncStartedAt, "request start", buildRequestUrl(query, null, batchSize, 0));
            AssetApiResponseParser.AssetPageResult firstPage = fetchAssetPage(query, batchSize, 0, syncStartedAt, "batch 1");
            List<Asset> firstBatchAssets = AssetApiResponseParser.mapAssetsAllowEmpty(firstPage.assetArray);
            int totalCount = AssetSyncPagePolicy.resolveInitialTotalCount(
                    firstPage.hasExplicitTotalCount,
                    firstPage.totalCount,
                    firstBatchAssets.size()
            );
            dispatchCountReady(callback, totalCount, batchSize, query.describe());

            if (firstBatchAssets.isEmpty()) {
                if (query.hasAnyFilter()) {
                    host.logSyncPhase(syncStartedAt, "server filter empty", query.describe());
                    List<Asset> locallyFilteredAssets = host.fetchAssetsWithLocalFilterFallback(query, syncStartedAt);
                    if (!locallyFilteredAssets.isEmpty()) {
                        completeSyncWithAssets(callback, locallyFilteredAssets, -1, syncStartedAt);
                        return;
                    }
                }
                deliverEmptySyncResult(callback, query, syncStartedAt);
                return;
            }

            LinkedHashMap<String, Asset> aggregatedAssetMap = new LinkedHashMap<>();
            int totalBatches = AssetSyncPagePolicy.estimateTotalBatches(totalCount, batchSize);
            boolean retriedWithoutPagination = false;
            int firstPageAddedCount = mergeUniqueAssets(aggregatedAssetMap, firstBatchAssets);
            int firstLoadedCount = aggregatedAssetMap.size();
            dispatchBatchProgress(callback, firstLoadedCount, totalCount, 1, totalBatches);

            if (!retriedWithoutPagination && AssetSyncPagePolicy.shouldRetryWithoutPagination(
                    batchSize,
                    firstBatchAssets.size(),
                    firstPageAddedCount,
                    firstLoadedCount,
                    totalCount,
                    0
            )) {
                host.logSyncPhase(syncStartedAt, "retry without pagination", "offset=0 | page=" + firstBatchAssets.size());
                List<Asset> unpagedAssets = fetchAssetsWithoutPagination(query, syncStartedAt, "unpaged fallback");
                int unpagedAdded = mergeUniqueAssets(aggregatedAssetMap, unpagedAssets);
                retriedWithoutPagination = true;
                host.logSyncPhase(
                        syncStartedAt,
                        "retry without pagination done",
                        "assets=" + unpagedAssets.size() + " | added=" + unpagedAdded
                );
            }

            if (AssetSyncPagePolicy.shouldStopPaging(
                    batchSize,
                    firstBatchAssets.size(),
                    firstPageAddedCount,
                    aggregatedAssetMap.size(),
                    totalCount
            )) {
                completeSyncWithAssets(callback, new ArrayList<>(aggregatedAssetMap.values()), totalCount, syncStartedAt);
                return;
            }

            for (int offset = batchSize, batchIndex = 1; ; offset += batchSize, batchIndex++) {
                AssetApiResponseParser.AssetPageResult pageResult =
                        fetchAssetPage(query, batchSize, offset, syncStartedAt, "batch " + (batchIndex + 1));
                List<Asset> batchAssets = AssetApiResponseParser.mapAssetsAllowEmpty(pageResult.assetArray);
                int addedCount = mergeUniqueAssets(aggregatedAssetMap, batchAssets);

                if (batchAssets.isEmpty()) {
                    host.logSyncPhase(syncStartedAt, "batch empty", "offset=" + offset);
                    break;
                }

                int pageSize = batchAssets.size();
                int loadedCount = aggregatedAssetMap.size();
                dispatchBatchProgress(callback, loadedCount, totalCount, batchIndex + 1, totalBatches);

                if (!retriedWithoutPagination && AssetSyncPagePolicy.shouldRetryWithoutPagination(
                        batchSize,
                        pageSize,
                        addedCount,
                        loadedCount,
                        totalCount,
                        offset
                )) {
                    host.logSyncPhase(syncStartedAt, "retry without pagination", "offset=" + offset + " | page=" + pageSize);
                    List<Asset> unpagedAssets = fetchAssetsWithoutPagination(query, syncStartedAt, "unpaged fallback");
                    int unpagedAdded = mergeUniqueAssets(aggregatedAssetMap, unpagedAssets);
                    retriedWithoutPagination = true;
                    host.logSyncPhase(
                            syncStartedAt,
                            "retry without pagination done",
                            "assets=" + unpagedAssets.size() + " | added=" + unpagedAdded
                    );
                }

                if (AssetSyncPagePolicy.shouldStopPaging(
                        batchSize,
                        pageSize,
                        addedCount,
                        aggregatedAssetMap.size(),
                        totalCount
                )) {
                    break;
                }
            }

            if (totalCount >= 0 && aggregatedAssetMap.size() < totalCount && !retriedWithoutPagination) {
                host.logSyncPhase(
                        syncStartedAt,
                        "fallback without pagination",
                        "loaded=" + aggregatedAssetMap.size() + " | expected=" + totalCount
                );
                List<Asset> unpagedAssets = fetchAssetsWithoutPagination(query, syncStartedAt, "unpaged final");
                int unpagedAdded = mergeUniqueAssets(aggregatedAssetMap, unpagedAssets);
                host.logSyncPhase(
                        syncStartedAt,
                        "fallback without pagination done",
                        "assets=" + unpagedAssets.size() + " | added=" + unpagedAdded
                );
            }

            completeSyncWithAssets(callback, new ArrayList<>(aggregatedAssetMap.values()), totalCount, syncStartedAt);
        } catch (JsonParseException parseException) {
            host.logSyncPhase(syncStartedAt, "parse failed", parseException.getMessage());
            logRepository.logError("LOAD_API", "Khong doc duoc du lieu tra ve", parseException.getMessage());
            dispatchSyncError(callback, AssetSyncErrorType.PARSE, parseException.getMessage());
            host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
        } catch (IOException ioException) {
            host.logSyncPhase(syncStartedAt, "sync failed", ioException.getMessage());
            logRepository.logError("LOAD_API", "Khong the ket noi hoac luu cache noi bo", ioException.getMessage());
            dispatchSyncError(callback, classifyIOException(ioException), ioException.getMessage());
            host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
        } catch (Exception exception) {
            host.logSyncPhase(syncStartedAt, "sync failed", exception.getMessage());
            logRepository.logError("LOAD_API", "Xu ly du lieu sync that bai", exception.getMessage());
            dispatchSyncError(callback, AssetSyncErrorType.UNKNOWN, exception.getMessage());
            host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
        }
    }

    void performBatchedSyncForLocationAliases(
            AssetSyncQuery query,
            AssetSyncProgressCallback callback,
            long syncStartedAt
    ) {
        try {
            int batchSize = query.getBatchSize() > 0 ? query.getBatchSize() : DEFAULT_SYNC_BATCH_SIZE;
            dispatchCountReady(callback, -1, batchSize, query.describe());

            LinkedHashMap<String, Asset> aggregatedAssetMap = new LinkedHashMap<>();
            int globalBatchIndex = 0;

            for (String locationQuery : query.getLocationQueries()) {
                if (locationQuery == null || locationQuery.trim().isEmpty()) {
                    continue;
                }

                host.logSyncPhase(syncStartedAt, "location alias start", locationQuery);
                AssetApiResponseParser.AssetPageResult previewPage =
                        fetchAssetPage(query, locationQuery, 1, 0, syncStartedAt, "preview [" + locationQuery + "]");
                List<Asset> previewAssets = AssetApiResponseParser.mapAssetsAllowEmpty(previewPage.assetArray);
                int totalCount = AssetSyncPagePolicy.resolveInitialTotalCount(
                        previewPage.hasExplicitTotalCount,
                        previewPage.totalCount,
                        previewAssets.size()
                );

                if (previewAssets.isEmpty()) {
                    host.logSyncPhase(syncStartedAt, "location alias empty", locationQuery);
                    continue;
                }

                boolean retriedWithoutPagination = false;
                int totalBatches = AssetSyncPagePolicy.estimateTotalBatches(totalCount, batchSize);
                int aliasStartSize = aggregatedAssetMap.size();

                for (int offset = 0, batchIndex = 0; ; offset += batchSize, batchIndex++) {
                    AssetApiResponseParser.AssetPageResult pageResult = fetchAssetPage(
                            query,
                            locationQuery,
                            batchSize,
                            offset,
                            syncStartedAt,
                            "batch " + (batchIndex + 1) + " [" + locationQuery + "]"
                    );
                    List<Asset> batchAssets = AssetApiResponseParser.mapAssetsAllowEmpty(pageResult.assetArray);
                    int addedCount = mergeUniqueAssets(aggregatedAssetMap, batchAssets);

                    if (batchAssets.isEmpty()) {
                        host.logSyncPhase(syncStartedAt, "batch empty", "location=" + locationQuery + " | offset=" + offset);
                        break;
                    }

                    globalBatchIndex++;
                    dispatchBatchProgress(callback, aggregatedAssetMap.size(), -1, globalBatchIndex, totalBatches);

                    int pageSize = batchAssets.size();
                    int aliasLoadedCount = aggregatedAssetMap.size() - aliasStartSize;
                    if (!retriedWithoutPagination && AssetSyncPagePolicy.shouldRetryWithoutPagination(
                            batchSize,
                            pageSize,
                            addedCount,
                            aliasLoadedCount,
                            totalCount,
                            offset
                    )) {
                        host.logSyncPhase(
                                syncStartedAt,
                                "retry without pagination",
                                "location=" + locationQuery + " | offset=" + offset + " | page=" + pageSize
                        );
                        List<Asset> unpagedAssets = fetchAssetsWithoutPagination(
                                query,
                                locationQuery,
                                syncStartedAt,
                                "unpaged fallback [" + locationQuery + "]"
                        );
                        int unpagedAdded = mergeUniqueAssets(aggregatedAssetMap, unpagedAssets);
                        retriedWithoutPagination = true;
                        host.logSyncPhase(
                                syncStartedAt,
                                "retry without pagination done",
                                "location=" + locationQuery + " | assets=" + unpagedAssets.size() + " | added=" + unpagedAdded
                        );
                    }

                    if (AssetSyncPagePolicy.shouldStopPaging(
                            batchSize,
                            pageSize,
                            addedCount,
                            aliasLoadedCount,
                            totalCount
                    )) {
                        break;
                    }
                }

                int aliasLoadedCount = aggregatedAssetMap.size() - aliasStartSize;
                if (totalCount >= 0 && aliasLoadedCount < totalCount && !retriedWithoutPagination) {
                    host.logSyncPhase(
                            syncStartedAt,
                            "fallback without pagination check",
                            "location=" + locationQuery + " | loaded=" + aliasLoadedCount + " | expected=" + totalCount
                    );
                    List<Asset> unpagedAssets = fetchAssetsWithoutPagination(
                            query,
                            locationQuery,
                            syncStartedAt,
                            "unpaged final [" + locationQuery + "]"
                    );
                    int unpagedAdded = mergeUniqueAssets(aggregatedAssetMap, unpagedAssets);
                    if (unpagedAdded > 0) {
                        host.logSyncPhase(
                                syncStartedAt,
                                "fallback without pagination done",
                                "location=" + locationQuery + " | assets=" + unpagedAssets.size() + " | added=" + unpagedAdded
                        );
                    }
                }
            }

            List<Asset> aggregatedAssets = new ArrayList<>(aggregatedAssetMap.values());
            if (aggregatedAssets.isEmpty()) {
                if (query.hasAnyFilter()) {
                    host.logSyncPhase(syncStartedAt, "location alias fallback local filter", query.describe());
                    List<Asset> locallyFilteredAssets = host.fetchAssetsWithLocalFilterFallback(query, syncStartedAt);
                    if (!locallyFilteredAssets.isEmpty()) {
                        completeSyncWithAssets(callback, locallyFilteredAssets, -1, syncStartedAt);
                        return;
                    }
                }
                deliverEmptySyncResult(callback, query, syncStartedAt);
                return;
            }

            completeSyncWithAssets(callback, aggregatedAssets, -1, syncStartedAt);
        } catch (JsonParseException parseException) {
            host.logSyncPhase(syncStartedAt, "parse failed", parseException.getMessage());
            logRepository.logError("LOAD_API", "Khong doc duoc du lieu tra ve", parseException.getMessage());
            dispatchSyncError(callback, AssetSyncErrorType.PARSE, parseException.getMessage());
            host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
        } catch (IOException ioException) {
            host.logSyncPhase(syncStartedAt, "sync failed", ioException.getMessage());
            logRepository.logError("LOAD_API", "Khong the ket noi hoac luu cache noi bo", ioException.getMessage());
            dispatchSyncError(callback, classifyIOException(ioException), ioException.getMessage());
            host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
        } catch (Exception exception) {
            host.logSyncPhase(syncStartedAt, "sync failed", exception.getMessage());
            logRepository.logError("LOAD_API", "Xu ly du lieu sync that bai", exception.getMessage());
            dispatchSyncError(callback, AssetSyncErrorType.UNKNOWN, exception.getMessage());
            host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
        }
    }

    private void deliverEmptySyncResult(
            AssetSyncProgressCallback callback,
            AssetSyncQuery query,
            long syncStartedAt
    ) throws IOException {
        host.logSyncPhase(syncStartedAt, "update cache start", "assets=0");
        host.applyCacheSnapshot(Collections.emptyList(), SYNC_SOURCE);
        host.logSyncPhase(syncStartedAt, "update cache end", "cached=0");

        host.logSyncPhase(syncStartedAt, "persist cache start", "assets=0");
        try {
            host.persistCacheNow(Collections.emptyList(), SYNC_SOURCE);
        } catch (IOException ioException) {
            throw new IOException("CACHE_WRITE: " + ioException.getMessage(), ioException);
        }
        host.logSyncPhase(syncStartedAt, "persist cache end", "assets=0");

        logRepository.logInfo("LOAD_API", "Khong co tai san phu hop voi bo loc", query.describe());
        host.logSyncPhase(syncStartedAt, "deliver result to UI", "assets=0");
        dispatchSyncSuccess(callback, Collections.emptyList(), "Khong co tai san phu hop voi bo loc da chon.");
        host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
    }

    private void completeSyncWithAssets(
            AssetSyncProgressCallback callback,
            List<Asset> assets,
            int totalCount,
            long syncStartedAt
    ) throws IOException {
        List<Asset> safeAssets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        host.logSyncPhase(syncStartedAt, "update cache start", "assets=" + safeAssets.size());
        host.applyCacheSnapshot(safeAssets, SYNC_SOURCE);
        host.logSyncPhase(syncStartedAt, "update cache end", "cached=" + safeAssets.size());

        host.logSyncPhase(syncStartedAt, "persist cache start", "assets=" + safeAssets.size());
        try {
            host.persistCacheNow(safeAssets, SYNC_SOURCE);
        } catch (IOException ioException) {
            throw new IOException("CACHE_WRITE: " + ioException.getMessage(), ioException);
        }
        host.logSyncPhase(syncStartedAt, "persist cache end", "assets=" + safeAssets.size());

        logRepository.logInfo("LOAD_API", "Da tai du lieu tai san tu API", safeAssets.size() + " asset(s)");
        host.logSyncPhase(syncStartedAt, "deliver result to UI", "assets=" + safeAssets.size());
        dispatchSyncSuccess(
                callback,
                safeAssets,
                totalCount >= 0
                        ? "Da tai " + safeAssets.size() + " / " + totalCount + " tai san tu API."
                        : "Da tai " + safeAssets.size() + " tai san tu API."
        );
        host.logSyncPhase(syncStartedAt, "total duration", totalDurationDetail(syncStartedAt));
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
        String requestUrl = buildRequestUrl(query, locationOverride, limit, offset);
        host.logSyncPhase(syncStartedAt, "request " + phaseLabel, requestUrl);
        Response<ResponseBody> response = ApiClient.getAssetApiService().getAssets(requestUrl).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP " + response.code());
        }

        host.logSyncPhase(syncStartedAt, "response " + phaseLabel, "HTTP " + response.code());
        host.logSyncPhase(syncStartedAt, "read body start", phaseLabel);
        String rawBody = readResponseBody(response.body());
        host.logSyncPhase(syncStartedAt, "read body end", phaseLabel + " | chars=" + rawBody.length());

        host.logSyncPhase(syncStartedAt, "parse start", phaseLabel);
        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawBody);
        host.logSyncPhase(
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

    private int mergeUniqueAssets(Map<String, Asset> target, List<Asset> assets) {
        if (target == null || assets == null || assets.isEmpty()) {
            return 0;
        }
        int beforeSize = target.size();
        for (Asset asset : assets) {
            String key = buildAssetIdentityKey(asset);
            Asset existing = target.get(key);
            if (existing == null || countFilledFields(asset) > countFilledFields(existing)) {
                target.put(key, asset);
            }
        }
        return target.size() - beforeSize;
    }

    private String buildAssetIdentityKey(Asset asset) {
        if (asset == null) {
            return "EMPTY_ASSET";
        }
        String normalizedTid = normalizeKey(asset.getTid());
        if (!normalizedTid.isEmpty()) {
            return "TID:" + normalizedTid;
        }
        String normalizedCode = normalizeKey(asset.getAssetCode());
        if (!normalizedCode.isEmpty()) {
            return "CODE:" + normalizedCode;
        }
        String normalizedSerial = normalizeKey(asset.getSerialNumber());
        if (!normalizedSerial.isEmpty()) {
            return "SERIAL:" + normalizedSerial;
        }
        String normalizedName = normalizeKey(asset.getAssetName());
        String normalizedDepartment = normalizeKey(asset.getDepartment());
        String normalizedLocation = normalizeKey(asset.getLocation());
        String rowNumber = asset.getRowNumber() == null ? "" : String.valueOf(asset.getRowNumber());
        return "ROW:" + rowNumber
                + "|NAME:" + normalizedName
                + "|DEPT:" + normalizedDepartment
                + "|LOC:" + normalizedLocation;
    }

    private int countFilledFields(Asset asset) {
        if (asset == null) {
            return 0;
        }
        int count = 0;
        if (!normalizeKey(asset.getAssetCode()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getTid()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getAssetName()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getAssetType()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getSerialNumber()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getDepartment()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getAssignedUser()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getLocation()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getInventoryStatus()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getAssetCondition()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getTagDate()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getTagBy()).isEmpty()) {
            count++;
        }
        if (!normalizeKey(asset.getNote()).isEmpty()) {
            count++;
        }
        return count;
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

    static AssetSyncErrorType classifyIOException(IOException exception) {
        if (exception == null) {
            return AssetSyncErrorType.UNKNOWN;
        }
        String message = exception.getMessage();
        if (message == null) {
            return AssetSyncErrorType.NETWORK;
        }
        String normalized = message.trim().toUpperCase(Locale.ROOT);
        if (AssetErrorFormatter.isTimeout(exception)) {
            return AssetSyncErrorType.TIMEOUT;
        }
        if (normalized.startsWith("HTTP ")) {
            return AssetSyncErrorType.API;
        }
        if (normalized.startsWith("CACHE_WRITE:")) {
            return AssetSyncErrorType.STORAGE;
        }
        return AssetSyncErrorType.NETWORK;
    }

    private static String readResponseBody(ResponseBody responseBody) throws IOException {
        if (responseBody == null) {
            return "";
        }
        String raw = responseBody.string();
        return raw == null ? "" : raw.trim();
    }

    private String buildRequestUrl(AssetSyncQuery query, String locationOverride, Integer limit, Integer offset) {
        return requestBuilder.buildGetDbUrl(ApiClient.getResolvedBaseUrl(), query, locationOverride, limit, offset);
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String totalDurationDetail(long syncStartedAt) {
        return (SystemClock.elapsedRealtime() - syncStartedAt) + "ms";
    }
}
