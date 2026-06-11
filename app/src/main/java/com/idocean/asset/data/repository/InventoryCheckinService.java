package com.idocean.asset.data.repository;

import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.api.AssetApiService;
import com.idocean.asset.data.dto.InventoryCheckinBatchRequestDto;
import com.idocean.asset.data.dto.InventoryCheckinRequestItemDto;
import com.idocean.asset.data.dto.InventoryCheckinResponseDto;
import com.idocean.asset.data.mapper.InventoryCheckinPayloadMapper;
import com.idocean.asset.data.mapper.InventoryCheckinResponseParser;
import com.idocean.asset.model.InventorySessionItem;

import java.io.IOException;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Service doc lap de gui batch kiem ke tu inventory len webhook.
 * Chua duoc noi vao UI o phase nay.
 */
public final class InventoryCheckinService {
    private static final String LOG_TAG = "CHECKIN_UPLOAD";
    private static final String ENDPOINT_NAME = "checkin-assets";
    private static final String ACTION_LABEL = "gui kiem ke tai san";
    private static final int LOG_CHUNK_SIZE = 700;

    private final LogRepository logRepository;
    private final AssetApiService apiService;
    private final InventoryCheckinPayloadMapper payloadMapper;
    private final InventoryCheckinResponseParser responseParser;

    public InventoryCheckinService() {
        this(
                LogRepository.getInstance(),
                ApiClient.getAssetApiService(),
                new InventoryCheckinPayloadMapper(),
                new InventoryCheckinResponseParser()
        );
    }

    InventoryCheckinService(
            LogRepository logRepository,
            AssetApiService apiService,
            InventoryCheckinPayloadMapper payloadMapper,
            InventoryCheckinResponseParser responseParser
    ) {
        this.logRepository = logRepository == null ? LogRepository.getInstance() : logRepository;
        this.apiService = apiService == null ? ApiClient.getAssetApiService() : apiService;
        this.payloadMapper = payloadMapper == null ? new InventoryCheckinPayloadMapper() : payloadMapper;
        this.responseParser = responseParser == null ? new InventoryCheckinResponseParser() : responseParser;
    }

    public InventoryCheckinBatchRequestDto buildRequest(List<InventorySessionItem> items) {
        return payloadMapper.buildRequest(items);
    }

    public InventoryCheckinUploadResult uploadSnapshot(List<InventorySessionItem> items)
            throws UploadFailureException {
        return uploadRequest(buildRequest(items));
    }

    public InventoryCheckinUploadResult uploadRequest(InventoryCheckinBatchRequestDto requestDto)
            throws UploadFailureException {
        if (requestDto.isEmpty()) {
            throw new UploadFailureException("Khong co du lieu kiem ke de gui.", null);
        }

        long startedAtNanos = System.nanoTime();
        String endpointUrl = buildCheckinEndpointUrl(ApiClient.getResolvedBaseUrl());
        JsonObject payload = requestDto.toJson();
        logRepository.logInfo(
                LOG_TAG,
                "Bat dau gui batch kiem ke",
                "items=" + requestDto.size()
                        + " | sample=" + describeFirstItem(requestDto)
                        + " | " + endpointUrl
        );
        logAndroidBody("REQUEST", payload == null ? "" : payload.toString());

        try {
            Response<ResponseBody> response = apiService.checkinAssets(endpointUrl, payload).execute();
            if (!response.isSuccessful()) {
                String errorBody = readResponseBody(response.errorBody());
                String message = AssetErrorFormatter.buildHttpMessage(
                        response.code(),
                        errorBody,
                        ENDPOINT_NAME,
                        ACTION_LABEL
                );
                logAndroidError("HTTP " + response.code()
                        + " | contentType=" + readContentType(response.errorBody())
                        + " | body=" + abbreviateForLog(errorBody));
                logRepository.logError(
                        LOG_TAG,
                        "Backend tu choi batch kiem ke",
                        "durationMs=" + toDurationMillis(startedAtNanos)
                                + " | HTTP " + response.code()
                                + " | body=" + abbreviateForLog(errorBody)
                );
                throw new UploadFailureException(message, null);
            }

            ResponseBody responseBody = response.body();
            String contentType = readContentType(responseBody);
            String rawBody = readResponseBody(responseBody);
            logAndroidInfo("HTTP " + response.code()
                    + " | contentType=" + contentType
                    + " | rawPreview=" + abbreviateForLog(rawBody));
            InventoryCheckinResponseDto responseDto = responseParser.parse(rawBody);
            if (!responseDto.isSuccess()) {
                String message = AssetErrorFormatter.buildBusinessMessage(
                        ENDPOINT_NAME,
                        ACTION_LABEL,
                        responseDto.getMessage()
                );
                logRepository.logError(
                        LOG_TAG,
                        "Backend bao xu ly batch kiem ke khong thanh cong",
                        "durationMs=" + toDurationMillis(startedAtNanos)
                                + " | " + abbreviateForLog(rawBody)
                );
                throw new UploadFailureException(message, null);
            }

            InventoryCheckinUploadResult result = responseDto.isWarningOnly()
                    ? InventoryCheckinUploadResult.warning(responseDto, buildWarningMessage(responseDto))
                    : InventoryCheckinUploadResult.success(responseDto, buildSuccessMessage(responseDto));

            logRepository.logInfo(
                    LOG_TAG,
                    result.isWarning() ? "Batch kiem ke hoan tat voi canh bao" : "Batch kiem ke hoan tat",
                    buildSummaryForLog(responseDto, requestDto.size(), toDurationMillis(startedAtNanos))
            );
            return result;
        } catch (JsonParseException parseException) {
            logAndroidError("Parse fail | " + parseException.getMessage());
            logRepository.logError(
                    LOG_TAG,
                    "Khong doc duoc phan hoi checkin-assets",
                    AssetErrorFormatter.buildDebugSummary(
                            ENDPOINT_NAME,
                            ACTION_LABEL,
                            parseException,
                            "durationMs=" + toDurationMillis(startedAtNanos)
                    )
            );
            throw new UploadFailureException(
                    "Phan hoi tu backend " + ENDPOINT_NAME + " khong hop le.",
                    parseException
            );
        } catch (IOException ioException) {
            logRepository.logError(
                    LOG_TAG,
                    "Khong gui duoc batch kiem ke",
                    AssetErrorFormatter.buildDebugSummary(
                            ENDPOINT_NAME,
                            ACTION_LABEL,
                            ioException,
                            "durationMs=" + toDurationMillis(startedAtNanos)
                    )
            );
            throw new UploadFailureException(
                    AssetErrorFormatter.buildConnectivityMessage(ENDPOINT_NAME, ACTION_LABEL, ioException),
                    ioException
            );
        }
    }

    static String buildCheckinEndpointUrl(String baseUrl) {
        String safeBaseUrl = safe(baseUrl);
        HttpUrl parsedBaseUrl = HttpUrl.parse(safeBaseUrl);
        if (parsedBaseUrl == null) {
            return buildFallbackEndpointUrl(safeBaseUrl);
        }

        HttpUrl.Builder builder = parsedBaseUrl.newBuilder();
        boolean hasWebhookSegment = false;
        for (String segment : parsedBaseUrl.pathSegments()) {
            if ("webhook".equalsIgnoreCase(segment)) {
                hasWebhookSegment = true;
                break;
            }
        }
        if (!hasWebhookSegment) {
            builder.addPathSegment("webhook");
        }
        builder.addPathSegment(ENDPOINT_NAME);
        return builder.build().toString();
    }

    private static String buildFallbackEndpointUrl(String baseUrl) {
        String safeBaseUrl = safe(baseUrl);
        if (safeBaseUrl.isEmpty()) {
            return "webhook/" + ENDPOINT_NAME;
        }
        String normalizedBaseUrl = safeBaseUrl.endsWith("/") ? safeBaseUrl : safeBaseUrl + "/";
        if (normalizedBaseUrl.toLowerCase().contains("/webhook/")) {
            return normalizedBaseUrl + ENDPOINT_NAME;
        }
        return normalizedBaseUrl + "webhook/" + ENDPOINT_NAME;
    }

    private String buildSuccessMessage(InventoryCheckinResponseDto responseDto) {
        if (responseDto != null && !responseDto.getMessage().isEmpty()) {
            return responseDto.getMessage();
        }
        return "Gui batch kiem ke thanh cong.";
    }

    private String buildWarningMessage(InventoryCheckinResponseDto responseDto) {
        if (responseDto != null && !responseDto.getMessage().isEmpty()) {
            return responseDto.getMessage();
        }
        return "Khong co san pham nao du dieu kien kiem ke.";
    }

    private String buildSummaryForLog(InventoryCheckinResponseDto responseDto, int requestItemCount, long durationMillis) {
        if (responseDto == null) {
            return "";
        }
        return "durationMs=" + durationMillis
                + " | requestItems=" + requestItemCount
                + " | session_id=" + responseDto.getSessionId()
                + " | received=" + responseDto.getTotalReceived()
                + " | valid=" + responseDto.getTotalScannedValid()
                + " | skipped=" + responseDto.getTotalSkipped()
                + " | inserted=" + responseDto.getTotalInserted();
    }

    private String readResponseBody(ResponseBody body) throws IOException {
        if (body == null) {
            return "";
        }
        return body.string();
    }

    public String describeFirstItem(InventoryCheckinBatchRequestDto requestDto) {
        if (requestDto == null || requestDto.getItems().isEmpty()) {
            return "-";
        }
        InventoryCheckinRequestItemDto item = requestDto.getItems().get(0);
        if (item == null) {
            return "-";
        }
        return "code=" + safe(item.getCode())
                + ",tid=" + safe(item.getTid())
                + ",status=" + safe(item.getInventoryStatus())
                + ",scanned_at=" + safe(item.getScannedAt());
    }

    private String readContentType(ResponseBody body) {
        if (body == null || body.contentType() == null) {
            return "";
        }
        return body.contentType().toString();
    }

    private String abbreviateForLog(String value) {
        String safeValue = safe(value).replace('\n', ' ').replace('\r', ' ');
        if (safeValue.length() <= 800) {
            return safeValue;
        }
        return safeValue.substring(0, 800) + "...";
    }

    private void logAndroidInfo(String detail) {
        try {
            Log.i(LOG_TAG, safe(detail));
        } catch (Throwable ignored) {
        }
    }

    private void logAndroidError(String detail) {
        try {
            Log.e(LOG_TAG, safe(detail));
        } catch (Throwable ignored) {
        }
    }

    private void logAndroidBody(String label, String body) {
        String safeBody = body == null ? "" : body.trim();
        if (safeBody.isEmpty()) {
            logAndroidInfo(label + " body=<empty>");
            return;
        }
        int totalChunks = (safeBody.length() + LOG_CHUNK_SIZE - 1) / LOG_CHUNK_SIZE;
        for (int index = 0; index < totalChunks; index++) {
            int start = index * LOG_CHUNK_SIZE;
            int end = Math.min(safeBody.length(), start + LOG_CHUNK_SIZE);
            logAndroidInfo(label + " body[" + (index + 1) + "/" + totalChunks + "]=" + safeBody.substring(start, end));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private long toDurationMillis(long startedAtNanos) {
        long elapsedNanos = System.nanoTime() - startedAtNanos;
        if (elapsedNanos <= 0L) {
            return 0L;
        }
        return elapsedNanos / 1_000_000L;
    }

    public static final class UploadFailureException extends Exception {
        public UploadFailureException(String message, Throwable cause) {
            super(message == null || message.trim().isEmpty()
                    ? "Gui batch kiem ke that bai."
                    : message.trim(), cause);
        }
    }
}
