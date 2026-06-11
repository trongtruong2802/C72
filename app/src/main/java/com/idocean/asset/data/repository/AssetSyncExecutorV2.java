package com.idocean.asset.data.repository;

import com.google.gson.JsonParseException;
import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.mapper.AssetApiResponseParser;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.AssetLocationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Chi chiu trach nhiem thuc thi 1 request Sync V2 va parse response.
 */
public final class AssetSyncExecutorV2 implements AssetSyncExecutionClientV2 {
    private final AssetSyncRequestBuilderV2 requestBuilder;

    public AssetSyncExecutorV2() {
        this(new AssetSyncRequestBuilderV2());
    }

    public AssetSyncExecutorV2(AssetSyncRequestBuilderV2 requestBuilder) {
        this.requestBuilder = requestBuilder == null ? new AssetSyncRequestBuilderV2() : requestBuilder;
    }

    @Override
    public ExecutionResult execute(AssetSyncQueryV2 query) throws IOException, JsonParseException {
        AssetSyncQueryV2 safeQuery = query == null ? AssetSyncQueryV2.fullSync() : query;
        String baseUrl = ApiClient.getResolvedBaseUrl();
        String requestUrl = requestBuilder.buildGetDbUrl(baseUrl, safeQuery);
        String rawBody = executeRequest(requestUrl);

        String fallbackLocation = resolveDisplayFallbackLocation(safeQuery, rawBody);
        if (!fallbackLocation.isEmpty()) {
            String fallbackRequestUrl = requestBuilder.buildGetDbUrl(baseUrl, safeQuery, fallbackLocation);
            if (!fallbackRequestUrl.equals(requestUrl)) {
                requestUrl = fallbackRequestUrl;
                rawBody = executeRequest(requestUrl);
            }
        }
        return buildExecutionResult(safeQuery, requestUrl, rawBody);
    }

    static ExecutionResult buildExecutionResult(
            AssetSyncQueryV2 query,
            String requestUrl,
            String rawBody
    ) throws JsonParseException {
        AssetSyncQueryV2 safeQuery = query == null ? AssetSyncQueryV2.fullSync() : query;
        if (shouldTreatBlankBodyAsEmptyResult(safeQuery, rawBody)) {
            return new ExecutionResult(safeQuery, requestUrl, Collections.<Asset>emptyList());
        }
        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawBody);
        List<Asset> assets = AssetApiResponseParser.mapAssetsAllowEmpty(pageResult.assetArray);
        return new ExecutionResult(safeQuery, requestUrl, assets);
    }

    static String resolveDisplayFallbackLocation(AssetSyncQueryV2 query, String rawBody) {
        if (!isBlankBody(rawBody) || query == null) {
            return "";
        }
        String requestLocation = safe(query.getRequestLocationValue());
        if (requestLocation.isEmpty()) {
            return "";
        }
        String canonicalDisplay = safe(AssetLocationUtils.normalizeLocationForDisplay(requestLocation));
        if (canonicalDisplay.isEmpty()) {
            return "";
        }
        if (canonicalDisplay.equalsIgnoreCase(requestLocation)) {
            return "";
        }
        return canonicalDisplay;
    }

    private String executeRequest(String requestUrl) throws IOException {
        Response<ResponseBody> response = ApiClient.getAssetApiService().getAssets(requestUrl).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP " + response.code());
        }
        return readResponseBody(response.body());
    }

    private String readResponseBody(ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return body.string();
    }

    private static boolean isBlankBody(String rawBody) {
        return rawBody == null || rawBody.trim().isEmpty();
    }

    static boolean shouldTreatBlankBodyAsEmptyResult(AssetSyncQueryV2 query, String rawBody) {
        if (!isBlankBody(rawBody) || query == null) {
            return false;
        }
        return query.hasAnyFilter();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ExecutionResult {
        private final AssetSyncQueryV2 query;
        private final String requestUrl;
        private final List<Asset> assets;

        ExecutionResult(AssetSyncQueryV2 query, String requestUrl, List<Asset> assets) {
            this.query = query;
            this.requestUrl = requestUrl == null ? "" : requestUrl;
            this.assets = Collections.unmodifiableList(
                    assets == null ? Collections.<Asset>emptyList() : new ArrayList<>(assets)
            );
        }

        public AssetSyncQueryV2 getQuery() {
            return query;
        }

        public String getRequestUrl() {
            return requestUrl;
        }

        public List<Asset> getAssets() {
            return assets;
        }
    }
}
