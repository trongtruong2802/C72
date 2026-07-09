package com.idocean.asset.data.sync;

import com.idocean.asset.model.AssetSyncQuery;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.HttpUrl;

/**
 * Legacy multi-value sync request builder kept only for rollback-only sync implementations.
 */
@Deprecated
public final class AssetSyncRequestBuilder {
    private static final String GET_DB_PATH = "get-db";

    public String buildGetDbUrl(String baseUrl, AssetSyncQuery query, Integer limit, Integer offset) {
        return buildGetDbUrl(baseUrl, query, null, limit, offset);
    }

    public String buildGetDbUrl(
            String baseUrl,
            AssetSyncQuery query,
            String locationOverride,
            Integer limit,
            Integer offset
    ) {
        String safeBaseUrl = baseUrl == null ? "" : baseUrl.trim();
        HttpUrl parsedBaseUrl = HttpUrl.parse(safeBaseUrl);
        if (parsedBaseUrl == null) {
            return buildFallbackUrl(safeBaseUrl, query, locationOverride, limit, offset);
        }

        HttpUrl.Builder builder = parsedBaseUrl.newBuilder().addPathSegment(GET_DB_PATH);
        // Backend get-db da ho tro multi-value theo pattern singular/plural CSV,
        // nen request runtime can gui day du danh sach department/location/assetType.
        appendRemoteFilter(builder, "department", "departments", query == null ? null : query.getDepartments());
        appendRemoteFilter(builder, "location", "locations", resolveLocationQueryAliasesForRemote(query, locationOverride));
        appendRemoteFilter(builder, "assetType", "assetTypes", query == null ? null : query.getAssetTypes());
        appendNumericValue(builder, "limit", limit);
        appendNumericValue(builder, "offset", offset);
        return builder.build().toString();
    }

    private List<String> resolveLocationQueryAliasesForRemote(AssetSyncQuery query, String locationOverride) {
        if (!safe(locationOverride).isEmpty()) {
            List<String> overrideValues = new ArrayList<>();
            overrideValues.add(safe(locationOverride));
            return overrideValues;
        }
        if (query == null) {
            return new ArrayList<>();
        }
        // Reuse source-of-truth alias list da duoc query mang theo tu DashboardController/AssetLocationUtils.
        return sanitizeValues(query.getRequestLocationValues());
    }

    private void appendRemoteFilter(HttpUrl.Builder builder, String singleKey, String multiKey, List<String> values) {
        if (builder == null) {
            return;
        }
        List<String> safeValues = sanitizeValues(values);
        if (safeValues.isEmpty()) {
            return;
        }
        if (safeValues.size() == 1) {
            builder.addQueryParameter(singleKey, safeValues.get(0));
            return;
        }
        builder.addQueryParameter(multiKey, joinCsv(safeValues));
    }

    private void appendNumericValue(HttpUrl.Builder builder, String key, Integer value) {
        if (builder == null || value == null || value < 0) {
            return;
        }
        builder.addQueryParameter(key, String.valueOf(value));
    }

    private String buildFallbackUrl(
            String baseUrl,
            AssetSyncQuery query,
            String locationOverride,
            Integer limit,
            Integer offset
    ) {
        StringBuilder builder = new StringBuilder();
        if (!baseUrl.isEmpty()) {
            builder.append(baseUrl);
            if (!baseUrl.endsWith("/")) {
                builder.append('/');
            }
        }
        builder.append(GET_DB_PATH);
        boolean hasQuery = false;
        hasQuery = appendFallbackRemoteFilter(builder, hasQuery, "department", "departments", query == null ? null : query.getDepartments());
        hasQuery = appendFallbackRemoteFilter(builder, hasQuery, "location", "locations", resolveLocationQueryAliasesForRemote(query, locationOverride));
        hasQuery = appendFallbackRemoteFilter(builder, hasQuery, "assetType", "assetTypes", query == null ? null : query.getAssetTypes());
        hasQuery = appendFallbackNumeric(builder, hasQuery, "limit", limit);
        hasQuery = appendFallbackNumeric(builder, hasQuery, "offset", offset);
        return builder.toString();
    }

    private boolean appendFallbackRemoteFilter(
            StringBuilder builder,
            boolean hasQuery,
            String singleKey,
            String multiKey,
            List<String> values
    ) {
        if (builder == null) {
            return hasQuery;
        }
        List<String> safeValues = sanitizeValues(values);
        if (safeValues.isEmpty()) {
            return hasQuery;
        }
        builder.append(hasQuery ? '&' : '?');
        if (safeValues.size() == 1) {
            builder.append(singleKey).append('=').append(safeValues.get(0));
            return true;
        }
        builder.append(multiKey).append('=').append(joinCsv(safeValues));
        return true;
    }

    private boolean appendFallbackNumeric(StringBuilder builder, boolean hasQuery, String key, Integer value) {
        if (builder == null || value == null || value < 0) {
            return hasQuery;
        }
        builder.append(hasQuery ? '&' : '?');
        builder.append(key).append('=').append(value);
        return true;
    }

    private List<String> sanitizeValues(List<String> values) {
        Set<String> safeValues = new LinkedHashSet<>();
        if (values == null) {
            return new ArrayList<>();
        }
        for (String value : values) {
            String safeValue = safe(value);
            if (!safeValue.isEmpty()) {
                safeValues.add(safeValue);
            }
        }
        return new ArrayList<>(safeValues);
    }

    private String joinCsv(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
