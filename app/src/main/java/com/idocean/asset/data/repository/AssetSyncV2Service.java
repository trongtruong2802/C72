package com.idocean.asset.data.repository;

import android.os.Handler;
import android.os.SystemClock;

import com.google.gson.JsonParseException;
import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.mapper.AssetApiResponseParser;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetSyncQuery;
import com.idocean.asset.utils.AssetLocationUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Legacy paged/local-fallback V2 implementation kept temporarily for rollback-only verification.
 */
@Deprecated
final class AssetSyncV2Service {
    private static final String SYNC_SOURCE = "API";
    private static final int DEFAULT_SYNC_BATCH_SIZE = 300;

    interface SyncHost {
        void logSyncPhase(long syncStartedAt, String phase, String detail);

        void applyCacheSnapshot(List<Asset> assets, String source);

        void persistCacheNow(List<Asset> assets, String source) throws IOException;
    }

    private final Handler mainHandler;
    private final LogRepository logRepository;
    private final SyncHost host;
    private final AssetSyncRequestBuilder requestBuilder = new AssetSyncRequestBuilder();
    private final AssetSyncStrategySelector strategySelector = new AssetSyncStrategySelector();
    private final AssetFilterService filterService = new AssetFilterService();

    AssetSyncV2Service(Handler mainHandler, LogRepository logRepository, SyncHost host) {
        this.mainHandler = mainHandler;
        this.logRepository = logRepository;
        this.host = host;
    }

    void syncAssets(AssetSyncQuery query, AssetSyncProgressCallback callback, long syncStartedAt) {
        AssetSyncQuery safeQuery = query == null
                ? new AssetSyncQuery("", "", "", DEFAULT_SYNC_BATCH_SIZE)
                : query;
        AssetSyncStrategy strategy = strategySelector.select(safeQuery);
        logRepository.logInfo(
                "LOAD_API",
                "Chon chien luoc dong bo",
                "V2 | " + strategy.name() + " | " + safeQuery.describe()
        );

        if (strategy == AssetSyncStrategy.LOCAL_FILTER_FALLBACK) {
            performLocalFilterSync(safeQuery, callback, syncStartedAt);
            return;
        }
        performBatchedSync(safeQuery, callback, syncStartedAt);
    }

    void performBatchedSync(AssetSyncQuery query, AssetSyncProgressCallback callback, long syncStartedAt) {
        try {
            int batchSize = query.getBatchSize() > 0 ? query.getBatchSize() : DEFAULT_SYNC_BATCH_SIZE;

            host.logSyncPhase(syncStartedAt, "request start", buildRequestUrl(query, null, batchSize, 0));
            AssetApiResponseParser.AssetPageResult firstPage =
                    fetchAssetPage(query, batchSize, 0, syncStartedAt, "batch 1");
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
                    List<Asset> locallyFilteredAssets = fetchAssetsWithLocalFilterFallback(query, syncStartedAt);
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

    private void performLocalFilterSync(AssetSyncQuery query, AssetSyncProgressCallback callback, long syncStartedAt) {
        try {
            int batchSize = query.getBatchSize() > 0 ? query.getBatchSize() : DEFAULT_SYNC_BATCH_SIZE;
            dispatchCountReady(callback, AssetSyncPagePolicy.UNKNOWN_TOTAL_COUNT, batchSize, query.describe());

            List<AssetSyncQuery> sourceQueries = buildSourceQueriesForLocalFilter(query, batchSize);
            LinkedHashMap<String, Asset> filteredAssetMap = new LinkedHashMap<>();
            int totalFetchedAssets = 0;
            int processedSourceQueries = 0;
            int totalSourceQueries = 0;

            for (AssetSyncQuery sourceQuery : sourceQueries) {
                if (sourceQuery != null && sourceQuery.hasAnyFilter()) {
                    totalSourceQueries++;
                }
            }

            for (AssetSyncQuery sourceQuery : sourceQueries) {
                if (sourceQuery == null || !sourceQuery.hasAnyFilter()) {
                    continue;
                }
                processedSourceQueries++;
                host.logSyncPhase(
                        syncStartedAt,
                        "local filter source start",
                        query.describe() + " | source=" + sourceQuery.describe()
                );
                try {
                    List<Asset> sourceAssets = fetchSourceAssetsForLocalFilter(sourceQuery, syncStartedAt);
                    totalFetchedAssets += sourceAssets.size();
                    List<Asset> filteredAssets = filterService.filterAssetsBySyncQuery(sourceAssets, query);
                    int addedCount = mergeUniqueAssets(filteredAssetMap, filteredAssets);
                    host.logSyncPhase(
                            syncStartedAt,
                            "local filter source result",
                            "source=" + sourceAssets.size() + " | matched=" + filteredAssets.size()
                                    + " | added=" + addedCount
                                    + " | merged=" + filteredAssetMap.size()
                    );
                    dispatchBatchProgress(
                            callback,
                            filteredAssetMap.size(),
                            AssetSyncPagePolicy.UNKNOWN_TOTAL_COUNT,
                            processedSourceQueries,
                            totalSourceQueries
                    );
                } catch (JsonParseException | IOException sourceException) {
                    host.logSyncPhase(
                            syncStartedAt,
                            "local filter source failed",
                            sourceQuery.describe() + " | " + sourceException.getMessage()
                    );
                }
            }

            if (!filteredAssetMap.isEmpty()) {
                List<Asset> filteredAssets = new ArrayList<>(filteredAssetMap.values());
                host.logSyncPhase(
                        syncStartedAt,
                        "local filter sync result",
                        "sourceQueries=" + totalSourceQueries + " | fetched=" + totalFetchedAssets + " | matched=" + filteredAssets.size()
                );
                completeSyncWithAssets(callback, filteredAssets, filteredAssets.size(), syncStartedAt);
                return;
            }

            host.logSyncPhase(syncStartedAt, "local filter fallback start", query.describe());
            List<Asset> unifiedAssets = fetchAssetsWithLocalFilterFallback(query, syncStartedAt);
            if (unifiedAssets.isEmpty()) {
                deliverEmptySyncResult(callback, query, syncStartedAt);
                return;
            }
            dispatchBatchProgress(callback, unifiedAssets.size(), unifiedAssets.size(), 1, 1);
            completeSyncWithAssets(callback, unifiedAssets, unifiedAssets.size(), syncStartedAt);
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

    private List<AssetSyncQuery> buildSourceQueriesForLocalFilter(AssetSyncQuery query, int batchSize) {
        List<AssetSyncQuery> sourceQueries = new ArrayList<>();
        Set<String> addedKeys = new LinkedHashSet<>();
        if (query == null) {
            addSourceQueryIfNeeded(sourceQueries, addedKeys, new AssetSyncQuery("", "", "", batchSize));
            return sourceQueries;
        }

        for (String department : query.getDepartments()) {
            addSourceQueryIfNeeded(sourceQueries, addedKeys, new AssetSyncQuery(department, "", "", batchSize));
        }
        for (String location : query.getLocations()) {
            List<String> locationAliases = AssetLocationUtils.resolveLocationQueryAliases(location);
            addSourceQueryIfNeeded(
                    sourceQueries,
                    addedKeys,
                    AssetSyncQuery.withLocationQueries("", location, locationAliases, "", batchSize)
            );
        }
        for (String assetType : query.getAssetTypes()) {
            addSourceQueryIfNeeded(sourceQueries, addedKeys, new AssetSyncQuery("", "", assetType, batchSize));
        }

        if (sourceQueries.isEmpty()) {
            addSourceQueryIfNeeded(sourceQueries, addedKeys, new AssetSyncQuery("", "", "", batchSize));
        }
        return sourceQueries;
    }

    private void addSourceQueryIfNeeded(
            List<AssetSyncQuery> sourceQueries,
            Set<String> addedKeys,
            AssetSyncQuery candidate
    ) {
        if (candidate == null) {
            return;
        }
        String key = candidate.toQueryMap(null, null).toString();
        if (addedKeys.contains(key)) {
            return;
        }
        addedKeys.add(key);
        sourceQueries.add(candidate);
    }

    private List<Asset> fetchSourceAssetsForLocalFilter(AssetSyncQuery sourceQuery, long syncStartedAt) throws IOException {
        if (sourceQuery == null) {
            return new ArrayList<>();
        }
        if (sourceQuery.getLocationQueries().size() <= 1) {
            return fetchAssetsWithoutPagination(sourceQuery, syncStartedAt, "unpaged local filter source");
        }

        LinkedHashMap<String, Asset> assetMap = new LinkedHashMap<>();
        for (String locationQuery : sourceQuery.getLocationQueries()) {
            if (safeTrim(locationQuery).isEmpty()) {
                continue;
            }
            List<Asset> assets = fetchAssetsWithoutPagination(
                    sourceQuery,
                    locationQuery,
                    syncStartedAt,
                    "unpaged local filter source [" + locationQuery + "]"
            );
            mergeUniqueAssets(assetMap, assets);
        }
        return new ArrayList<>(assetMap.values());
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

    private List<Asset> fetchAssetsWithLocalFilterFallback(AssetSyncQuery query, long syncStartedAt) throws IOException {
        AssetSyncQuery unfilteredQuery = new AssetSyncQuery("", "", "", DEFAULT_SYNC_BATCH_SIZE);
        List<Asset> allAssets = fetchAssetsWithoutPagination(
                unfilteredQuery,
                syncStartedAt,
                "unpaged local filter fallback"
        );
        List<Asset> filteredAssets = filterService.filterAssetsBySyncQuery(allAssets, query);
        host.logSyncPhase(
                syncStartedAt,
                "local filter fallback result",
                "all=" + allAssets.size() + " | matched=" + filteredAssets.size()
        );
        return filteredAssets;
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
        if (exception instanceof SocketTimeoutException || AssetErrorFormatter.isTimeout(exception)) {
            return AssetSyncErrorType.TIMEOUT;
        }
        String message = exception.getMessage();
        if (message == null) {
            return AssetSyncErrorType.NETWORK;
        }
        String normalized = message.trim().toUpperCase(Locale.ROOT);
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
