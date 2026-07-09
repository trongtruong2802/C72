package com.idocean.asset.data.sync;

import com.google.gson.JsonParseException;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.data.repository.AssetFilterService;
import com.idocean.asset.data.repository.AssetErrorFormatter;
import com.idocean.asset.model.Asset;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Dieu phoi Sync V2 moi.
 * Class nay co chu y khong quan ly thread va khong ghi cache, de giu doc lap voi flow runtime hien tai.
 */
public final class AssetSyncCoordinatorV2 {
    public static final int UNKNOWN_TOTAL_COUNT = -1;

    private final LogRepository logRepository;
    private final AssetSyncCombinationBuilderV2 combinationBuilder;
    private final AssetSyncExecutionClientV2 executor;
    private final AssetFilterService filterService;

    public AssetSyncCoordinatorV2() {
        this(LogRepository.getInstance(), new AssetSyncCombinationBuilderV2(), new AssetSyncExecutorV2());
    }

    public AssetSyncCoordinatorV2(LogRepository logRepository) {
        this(logRepository, new AssetSyncCombinationBuilderV2(), new AssetSyncExecutorV2());
    }

    public AssetSyncCoordinatorV2(
            LogRepository logRepository,
            AssetSyncCombinationBuilderV2 combinationBuilder,
            AssetSyncExecutionClientV2 executor
    ) {
        this.logRepository = logRepository;
        this.combinationBuilder = combinationBuilder == null
                ? new AssetSyncCombinationBuilderV2()
                : combinationBuilder;
        this.executor = executor == null ? new AssetSyncExecutorV2() : executor;
        this.filterService = new AssetFilterService();
    }

    public SyncResult sync(AssetSyncQueryV2 query) throws SyncFailureException {
        return sync(query, null);
    }

    public SyncResult sync(AssetSyncQueryV2 query, AssetSyncProgressCallback callback) throws SyncFailureException {
        AssetSyncQueryV2 safeQuery = query == null ? AssetSyncQueryV2.fullSync() : query;
        try {
            AssetSyncCombinationBuilderV2.BuildResult buildResult = combinationBuilder.build(safeQuery);
            int totalRequests = buildResult.getCombinationCount();

            logInfo(
                    AssetSyncLogFormatterV2.ACTION,
                    "Bat dau sync V2",
                    AssetSyncLogFormatterV2.describeSyncStart(buildResult)
            );
            dispatchCountReady(callback, UNKNOWN_TOTAL_COUNT, totalRequests, buildResult.getDescription());

            AssetSyncDeduplicatorV2 deduplicator = new AssetSyncDeduplicatorV2();
            List<String> executedRequestUrls = new ArrayList<>();
            int totalRecordsBeforeMerge = 0;

            int requestIndex = 0;
            for (AssetSyncQueryV2 subQuery : buildResult.getSubQueries()) {
                requestIndex++;
                logInfo(
                        AssetSyncLogFormatterV2.ACTION,
                        "Request con bat dau",
                        AssetSyncLogFormatterV2.describeSubRequest(requestIndex, totalRequests, subQuery)
                );

                long requestStartedAt = System.nanoTime();
                try {
                    SubQueryExecutionResult executionResult = executeSubQuery(subQuery, executedRequestUrls);
                    totalRecordsBeforeMerge += executionResult.getFetchedRecordCount();
                    deduplicator.merge(executionResult.getMatchedAssets());

                    logInfo(
                            AssetSyncLogFormatterV2.ACTION,
                            "Request con hoan tat",
                            AssetSyncLogFormatterV2.describeSubRequestSuccess(
                                    requestIndex,
                                    totalRequests,
                                    subQuery,
                                    toDurationMillis(requestStartedAt),
                                    executionResult.getRemoteRequestCount(),
                                    executionResult.getFetchedRecordCount(),
                                    executionResult.getMatchedAssets().size(),
                                    totalRecordsBeforeMerge,
                                    deduplicator.size()
                            )
                    );
                } catch (JsonParseException parseException) {
                    throw failSubRequest(
                            callback,
                            subQuery,
                            requestIndex,
                            totalRequests,
                            AssetSyncErrorType.PARSE,
                            requestStartedAt,
                            parseException
                    );
                } catch (IOException ioException) {
                    throw failSubRequest(
                            callback,
                            subQuery,
                            requestIndex,
                            totalRequests,
                            classifyIOException(ioException),
                            requestStartedAt,
                            ioException
                    );
                } catch (Exception exception) {
                    throw failSubRequest(
                            callback,
                            subQuery,
                            requestIndex,
                            totalRequests,
                            AssetSyncErrorType.UNKNOWN,
                            requestStartedAt,
                            exception
                    );
                }

                dispatchProgress(
                        callback,
                        deduplicator.size(),
                        UNKNOWN_TOTAL_COUNT,
                        requestIndex,
                        totalRequests
                );
            }

            SyncResult result = new SyncResult(
                    safeQuery,
                    totalRequests,
                    executedRequestUrls,
                    deduplicator.getMergedAssets()
            );
            String successMessage = buildSuccessMessage(result);
            logInfo(
                    AssetSyncLogFormatterV2.ACTION,
                    "Sync V2 hoan tat",
                    AssetSyncLogFormatterV2.describeSyncCompletion(
                            result.getQuery(),
                            totalRequests,
                            totalRecordsBeforeMerge,
                            result.getAssets().size()
                    )
            );
            dispatchSuccess(callback, result.getAssets(), successMessage);
            return result;
        } catch (AssetSyncCombinationBuilderV2.CombinationLimitException limitException) {
            throw fail(
                    callback,
                    AssetSyncErrorType.UNKNOWN,
                    AssetSyncLogFormatterV2.buildCombinationLimitMessage(
                            safeQuery,
                            limitException.getCombinationCount(),
                            limitException.getMaxCombinations()
                    ),
                    AssetSyncLogFormatterV2.describeCombinationLimit(
                            safeQuery,
                            limitException.getCombinationCount(),
                            limitException.getMaxCombinations()
                    ),
                    limitException
            );
        } catch (SyncFailureException failureException) {
            throw failureException;
        } catch (Exception exception) {
            throw fail(
                    callback,
                    AssetSyncErrorType.UNKNOWN,
                    AssetSyncLogFormatterV2.buildUnexpectedFailureMessage(),
                    AssetSyncLogFormatterV2.describeUnexpectedFailure(safeQuery, exception),
                    exception
            );
        }
    }

    public static AssetSyncErrorType classifyIOException(IOException exception) {
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

    private SyncFailureException failSubRequest(
            AssetSyncProgressCallback callback,
            AssetSyncQueryV2 subQuery,
            int requestIndex,
            int totalRequests,
            AssetSyncErrorType errorType,
            long requestStartedAt,
            Exception cause
    ) {
        return fail(
                callback,
                errorType,
                AssetSyncLogFormatterV2.buildRequestFailureMessage(errorType, requestIndex, totalRequests),
                AssetSyncLogFormatterV2.describeSubRequestFailure(
                        requestIndex,
                        totalRequests,
                        subQuery,
                        toDurationMillis(requestStartedAt),
                        errorType,
                        cause
                ),
                cause
        );
    }

    private SyncFailureException fail(
            AssetSyncProgressCallback callback,
            AssetSyncErrorType errorType,
            String message,
            String detail,
            Exception cause
    ) {
        AssetSyncErrorType safeErrorType = errorType == null ? AssetSyncErrorType.UNKNOWN : errorType;
        String safeMessage = safeMessage(message);
        logError(
                AssetSyncLogFormatterV2.ACTION,
                "Sync V2 that bai",
                detail == null || detail.trim().isEmpty()
                        ? safeErrorType.name() + " | " + safeMessage
                        : detail
        );
        dispatchError(callback, safeErrorType, safeMessage);
        return new SyncFailureException(safeErrorType, safeMessage, cause);
    }

    private String buildSuccessMessage(SyncResult result) {
        int assetCount = result == null ? 0 : result.getAssets().size();
        int requestCount = result == null ? 0 : result.getRequestCount();
        return "Sync V2 da tai " + assetCount + " tai san tu "
                + requestCount + " request con.";
    }

    private void dispatchCountReady(
            AssetSyncProgressCallback callback,
            int totalCount,
            int batchSize,
            String description
    ) {
        if (callback == null) {
            return;
        }
        callback.onCountReady(totalCount, batchSize, description == null ? "" : description);
    }

    private void dispatchProgress(
            AssetSyncProgressCallback callback,
            int loadedCount,
            int totalCount,
            int batchIndex,
            int totalBatches
    ) {
        if (callback == null) {
            return;
        }
        callback.onProgress(loadedCount, totalCount, batchIndex, totalBatches);
    }

    private void dispatchSuccess(AssetSyncProgressCallback callback, List<Asset> assets, String message) {
        if (callback == null) {
            return;
        }
        List<Asset> safeAssets = assets == null
                ? Collections.<Asset>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(assets));
        callback.onSuccess(safeAssets, message == null ? "" : message);
    }

    private void dispatchError(AssetSyncProgressCallback callback, AssetSyncErrorType errorType, String message) {
        if (callback == null) {
            return;
        }
        callback.onError(
                errorType == null ? AssetSyncErrorType.UNKNOWN : errorType,
                message == null ? "" : message
        );
    }

    private void logInfo(String action, String message, String detail) {
        if (logRepository == null) {
            return;
        }
        logRepository.logInfo(action, safeMessage(message), detail == null ? "" : detail);
    }

    private void logError(String action, String message, String detail) {
        if (logRepository == null) {
            return;
        }
        logRepository.logError(action, safeMessage(message), detail == null ? "" : detail);
    }

    private String safeMessage(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Sync V2 that bai do loi khong xac dinh.";
        }
        return value.trim();
    }

    private long toDurationMillis(long startedAtNanos) {
        long elapsedNanos = System.nanoTime() - startedAtNanos;
        if (elapsedNanos <= 0L) {
            return 0L;
        }
        return elapsedNanos / 1_000_000L;
    }

    private SubQueryExecutionResult executeSubQuery(
            AssetSyncQueryV2 subQuery,
            List<String> executedRequestUrls
    ) throws IOException, JsonParseException {
        List<String> requestLocationValues = resolveRequestLocationValues(subQuery);
        AssetSyncDeduplicatorV2 subQueryDeduplicator = new AssetSyncDeduplicatorV2();
        int fetchedRecordCount = 0;
        int remoteRequestCount = 0;

        for (String requestLocationValue : requestLocationValues) {
            AssetSyncQueryV2 requestQuery = subQuery.withRequestLocationValue(requestLocationValue);
            AssetSyncExecutorV2.ExecutionResult executionResult = executor.execute(requestQuery);
            executedRequestUrls.add(executionResult.getRequestUrl());
            remoteRequestCount++;
            fetchedRecordCount += executionResult.getAssets().size();
            subQueryDeduplicator.merge(executionResult.getAssets());
        }

        List<Asset> matchedAssets = applyLocationFilterIfNeeded(
                subQueryDeduplicator.getMergedAssets(),
                subQuery
        );
        if (shouldRunLocationFallback(subQuery, matchedAssets)) {
            AssetSyncExecutorV2.ExecutionResult fallbackResult = executor.execute(subQuery.withoutLocationFilter());
            executedRequestUrls.add(fallbackResult.getRequestUrl());
            remoteRequestCount++;
            fetchedRecordCount += fallbackResult.getAssets().size();
            subQueryDeduplicator.merge(fallbackResult.getAssets());
            matchedAssets = applyLocationFilterIfNeeded(
                    subQueryDeduplicator.getMergedAssets(),
                    subQuery
            );
        }
        return new SubQueryExecutionResult(
                remoteRequestCount,
                fetchedRecordCount,
                matchedAssets
        );
    }

    private List<String> resolveRequestLocationValues(AssetSyncQueryV2 subQuery) {
        if (subQuery == null) {
            return Collections.singletonList("");
        }
        String requestLocationValue = subQuery.getRequestLocationValue().trim();
        if (requestLocationValue.isEmpty()) {
            return Collections.singletonList("");
        }
        return Collections.singletonList(requestLocationValue);
    }

    private List<Asset> applyLocationFilterIfNeeded(List<Asset> assets, AssetSyncQueryV2 subQuery) {
        if (subQuery == null || subQuery.getSingleLocation().trim().isEmpty()) {
            return assets == null ? Collections.<Asset>emptyList() : assets;
        }
        return filterService.filterAssetsBySyncQuery(assets, subQuery);
    }

    private boolean shouldRunLocationFallback(AssetSyncQueryV2 subQuery, List<Asset> matchedAssets) {
        if (subQuery == null) {
            return false;
        }
        if (subQuery.getSingleLocation().trim().isEmpty()) {
            return false;
        }
        return matchedAssets == null || matchedAssets.isEmpty();
    }

    public static final class SyncResult {
        private final AssetSyncQueryV2 query;
        private final int requestCount;
        private final List<String> executedRequestUrls;
        private final List<Asset> assets;

        SyncResult(
                AssetSyncQueryV2 query,
                int requestCount,
                List<String> executedRequestUrls,
                List<Asset> assets
        ) {
            this.query = query;
            this.requestCount = requestCount;
            this.executedRequestUrls = Collections.unmodifiableList(
                    executedRequestUrls == null
                            ? Collections.<String>emptyList()
                            : new ArrayList<>(executedRequestUrls)
            );
            this.assets = Collections.unmodifiableList(
                    assets == null ? Collections.<Asset>emptyList() : new ArrayList<>(assets)
            );
        }

        public AssetSyncQueryV2 getQuery() {
            return query;
        }

        public int getRequestCount() {
            return requestCount;
        }

        public List<String> getExecutedRequestUrls() {
            return executedRequestUrls;
        }

        public List<Asset> getAssets() {
            return assets;
        }
    }

    public static final class SyncFailureException extends Exception {
        private final AssetSyncErrorType errorType;

        SyncFailureException(AssetSyncErrorType errorType, String message, Throwable cause) {
            super(message, cause);
            this.errorType = errorType == null ? AssetSyncErrorType.UNKNOWN : errorType;
        }

        public AssetSyncErrorType getErrorType() {
            return errorType;
        }
    }

    private static final class SubQueryExecutionResult {
        private final int remoteRequestCount;
        private final int fetchedRecordCount;
        private final List<Asset> matchedAssets;

        SubQueryExecutionResult(int remoteRequestCount, int fetchedRecordCount, List<Asset> matchedAssets) {
            this.remoteRequestCount = remoteRequestCount;
            this.fetchedRecordCount = fetchedRecordCount;
            this.matchedAssets = matchedAssets == null
                    ? Collections.<Asset>emptyList()
                    : new ArrayList<>(matchedAssets);
        }

        int getRemoteRequestCount() {
            return remoteRequestCount;
        }

        int getFetchedRecordCount() {
            return fetchedRecordCount;
        }

        List<Asset> getMatchedAssets() {
            return new ArrayList<>(matchedAssets);
        }
    }
}
