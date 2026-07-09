package com.idocean.asset.data.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class AssetSyncLogFormatterV2 {
    static final String ACTION = "SYNC_V2";

    private static final int MAX_LIST_PREVIEW = 3;
    private static final int MAX_VALUE_LENGTH = 48;

    private AssetSyncLogFormatterV2() {
    }

    static String describeSyncStart(AssetSyncCombinationBuilderV2.BuildResult buildResult) {
        AssetSyncQueryV2 query = buildResult == null ? AssetSyncQueryV2.fullSync() : buildResult.getQuery();
        int requestCount = buildResult == null ? 0 : buildResult.getCombinationCount();
        return "type=" + syncType(query)
                + " | requests=" + requestCount
                + " | " + summarizeList("departments", query.getDepartments())
                + " | " + summarizeList("locationKeys", query.getLocations())
                + " | " + summarizeList("assetTypes", query.getAssetTypes());
    }

    static String describeSubRequest(int requestIndex, int totalRequests, AssetSyncQueryV2 query) {
        AssetSyncQueryV2 safeQuery = safeQuery(query);
        StringBuilder builder = new StringBuilder();
        builder.append("request=").append(requestIndex).append('/').append(totalRequests);
        builder.append(" | type=").append(syncType(safeQuery));
        builder.append(" | department=").append(safeValue(safeQuery.getSingleDepartment()));
        builder.append(" | locationKey=").append(safeValue(safeQuery.getSingleLocation()));
        builder.append(" | assetType=").append(safeValue(safeQuery.getSingleAssetType()));
        String requestLocation = safeValue(safeQuery.getRequestLocationValue());
        if (!requestLocation.equals(safeValue(safeQuery.getSingleLocation()))) {
            builder.append(" | requestLocation=").append(requestLocation);
        }
        return builder.toString();
    }

    static String describeSubRequestSuccess(
            int requestIndex,
            int totalRequests,
            AssetSyncQueryV2 query,
            long durationMs,
            int remoteRequestCount,
            int requestRecordCount,
            int matchedRecordCount,
            int totalRecordsBeforeMerge,
            int recordsAfterDeduplicate
    ) {
        return describeSubRequest(requestIndex, totalRequests, query)
                + " | durationMs=" + Math.max(0L, durationMs)
                + " | remoteCalls=" + Math.max(0, remoteRequestCount)
                + " | requestRecords=" + Math.max(0, requestRecordCount)
                + " | matchedAfterLocationFilter=" + Math.max(0, matchedRecordCount)
                + " | recordsBeforeMerge=" + Math.max(0, totalRecordsBeforeMerge)
                + " | recordsBeforeDeduplicate=" + Math.max(0, totalRecordsBeforeMerge)
                + " | recordsAfterDeduplicate=" + Math.max(0, recordsAfterDeduplicate);
    }

    static String describeSubRequestFailure(
            int requestIndex,
            int totalRequests,
            AssetSyncQueryV2 query,
            long durationMs,
            AssetSyncErrorType errorType,
            Throwable cause
    ) {
        return describeSubRequest(requestIndex, totalRequests, query)
                + " | durationMs=" + Math.max(0L, durationMs)
                + " | reason=" + safeErrorType(errorType).name()
                + buildTechnicalDetailFragment(cause);
    }

    static String describeSyncCompletion(
            AssetSyncQueryV2 query,
            int requestCount,
            int totalRecordsBeforeMerge,
            int recordsAfterDeduplicate
    ) {
        return "type=" + syncType(query)
                + " | requests=" + Math.max(0, requestCount)
                + " | recordsBeforeMerge=" + Math.max(0, totalRecordsBeforeMerge)
                + " | recordsBeforeDeduplicate=" + Math.max(0, totalRecordsBeforeMerge)
                + " | recordsAfterDeduplicate=" + Math.max(0, recordsAfterDeduplicate);
    }

    static String describeCombinationLimit(
            AssetSyncQueryV2 query,
            int combinationCount,
            int maxCombinations
    ) {
        return "type=" + syncType(query)
                + " | requests=" + Math.max(0, combinationCount)
                + " | maxRequests=" + Math.max(0, maxCombinations)
                + " | " + summarizeList("departments", safeQuery(query).getDepartments())
                + " | " + summarizeList("locationKeys", safeQuery(query).getLocations())
                + " | " + summarizeList("assetTypes", safeQuery(query).getAssetTypes());
    }

    static String describeUnexpectedFailure(AssetSyncQueryV2 query, Throwable cause) {
        return "type=" + syncType(query)
                + " | reason=" + sanitizeText(cause == null ? "" : cause.getClass().getSimpleName());
    }

    static String buildCombinationLimitMessage(
            AssetSyncQueryV2 query,
            int combinationCount,
            int maxCombinations
    ) {
        return "Sync V2 dung som: sync " + syncType(query)
                + " tao ra " + combinationCount
                + " to hop request con, vuot gioi han toi da " + maxCombinations + ".";
    }

    static String buildRequestFailureMessage(
            AssetSyncErrorType errorType,
            int requestIndex,
            int totalRequests
    ) {
        String prefix = "Sync V2 request con that bai o to hop " + requestIndex + "/" + totalRequests + ".";
        switch (safeErrorType(errorType)) {
            case TIMEOUT:
                return prefix + " Het thoi gian cho may chu phan hoi.";
            case API:
                return prefix + " May chu tra ve loi API.";
            case PARSE:
                return prefix + " Du lieu tra ve khong hop le.";
            case NETWORK:
                return prefix + " Khong the ket noi may chu.";
            case STORAGE:
                return prefix + " Khong the ghi du lieu tam.";
            case UNKNOWN:
            case NONE:
            default:
                return prefix + " Vui long thu lai.";
        }
    }

    static String buildUnexpectedFailureMessage() {
        return "Sync V2 that bai do loi khong xac dinh.";
    }

    private static String buildTechnicalDetailFragment(Throwable cause) {
        if (cause == null) {
            return "";
        }
        String httpCode = extractHttpCode(cause);
        if (!httpCode.isEmpty()) {
            return " | http=" + httpCode;
        }
        return "";
    }

    private static String extractHttpCode(Throwable cause) {
        String message = cause == null ? "" : sanitizeText(cause.getMessage());
        if (message.startsWith("HTTP ")) {
            return message.substring("HTTP ".length()).trim();
        }
        return "";
    }

    private static String summarizeList(String label, List<String> values) {
        List<String> safeValues = sanitizeValues(values);
        if (safeValues.isEmpty()) {
            return label + "(0)=-";
        }
        StringBuilder builder = new StringBuilder();
        int previewCount = Math.min(safeValues.size(), MAX_LIST_PREVIEW);
        for (int index = 0; index < previewCount; index++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(safeValues.get(index));
        }
        if (safeValues.size() > previewCount) {
            builder.append(", ...");
        }
        return label + "(" + safeValues.size() + ")=" + builder;
    }

    private static List<String> sanitizeValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String safeValue = safeValue(value);
            if (!"-".equals(safeValue)) {
                result.add(safeValue);
            }
        }
        return result;
    }

    private static String syncType(AssetSyncQueryV2 query) {
        return safeQuery(query).getMode().name().toLowerCase(Locale.ROOT);
    }

    private static AssetSyncQueryV2 safeQuery(AssetSyncQueryV2 query) {
        return query == null ? AssetSyncQueryV2.fullSync() : query;
    }

    private static AssetSyncErrorType safeErrorType(AssetSyncErrorType errorType) {
        return errorType == null ? AssetSyncErrorType.UNKNOWN : errorType;
    }

    private static String safeValue(String value) {
        String safe = sanitizeText(value);
        return safe.isEmpty() ? "-" : safe;
    }

    private static String sanitizeText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        if (normalized.length() <= MAX_VALUE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_VALUE_LENGTH - 3) + "...";
    }
}
