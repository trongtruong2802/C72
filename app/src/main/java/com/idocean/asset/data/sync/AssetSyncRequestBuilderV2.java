package com.idocean.asset.data.sync;

import com.idocean.asset.data.api.ApiClient;

import okhttp3.HttpUrl;

/**
 * Builder cho request Sync V2.
 * Moi request chi duoc mang toi da 1 gia tri cho moi field.
 */
public final class AssetSyncRequestBuilderV2 {
    private static final String GET_DB_PATH = "get-db";

    public String buildGetDbUrl(AssetSyncQueryV2 query) {
        return buildGetDbUrl(ApiClient.getResolvedBaseUrl(), query, null);
    }

    public String buildGetDbUrl(String baseUrl, AssetSyncQueryV2 query) {
        return buildGetDbUrl(baseUrl, query, null);
    }

    public String buildGetDbUrl(String baseUrl, AssetSyncQueryV2 query, String locationOverride) {
        AssetSyncQueryV2 safeQuery = query == null ? AssetSyncQueryV2.fullSync() : query;
        validateSingleValueQuery(safeQuery);

        String safeBaseUrl = safe(baseUrl);
        HttpUrl parsedBaseUrl = HttpUrl.parse(safeBaseUrl);
        if (parsedBaseUrl == null) {
            return buildFallbackUrl(safeBaseUrl, safeQuery, locationOverride);
        }

        HttpUrl.Builder builder = parsedBaseUrl.newBuilder().addPathSegment(GET_DB_PATH);
        appendSingleValue(builder, "department", safeQuery.getSingleDepartment());
        appendSingleValue(builder, "location", resolveRequestLocation(safeQuery, locationOverride));
        appendSingleValue(builder, "assetType", safeQuery.getSingleAssetType());
        return builder.build().toString();
    }

    private void validateSingleValueQuery(AssetSyncQueryV2 query) {
        if (query == null) {
            return;
        }
        if (query.getDepartments().size() > 1
                || query.getLocations().size() > 1
                || query.getAssetTypes().size() > 1) {
            throw new IllegalArgumentException(
                    "AssetSyncRequestBuilderV2 chi ho tro query don gia tri cho tung field."
            );
        }
    }

    private void appendSingleValue(HttpUrl.Builder builder, String key, String value) {
        if (builder == null) {
            return;
        }
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return;
        }
        builder.addQueryParameter(key, safeValue);
    }

    private String buildFallbackUrl(String baseUrl, AssetSyncQueryV2 query, String locationOverride) {
        StringBuilder builder = new StringBuilder();
        if (!baseUrl.isEmpty()) {
            builder.append(baseUrl);
            if (!baseUrl.endsWith("/")) {
                builder.append('/');
            }
        }
        builder.append(GET_DB_PATH);

        boolean hasQuery = false;
        hasQuery = appendFallbackSingleValue(builder, hasQuery, "department", query.getSingleDepartment());
        hasQuery = appendFallbackSingleValue(builder, hasQuery, "location", resolveRequestLocation(query, locationOverride));
        appendFallbackSingleValue(builder, hasQuery, "assetType", query.getSingleAssetType());
        return builder.toString();
    }

    private boolean appendFallbackSingleValue(
            StringBuilder builder,
            boolean hasQuery,
            String key,
            String value
    ) {
        if (builder == null) {
            return hasQuery;
        }
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return hasQuery;
        }
        builder.append(hasQuery ? '&' : '?');
        builder.append(key).append('=').append(safeValue);
        return true;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveRequestLocation(AssetSyncQueryV2 query, String locationOverride) {
        String safeOverride = safe(locationOverride);
        if (!safeOverride.isEmpty()) {
            return safeOverride;
        }
        return query == null ? "" : query.getRequestLocationValue();
    }
}
