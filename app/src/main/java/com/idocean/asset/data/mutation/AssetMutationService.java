package com.idocean.asset.data.mutation;

import android.content.Context;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.data.db.AppDatabase;
import com.idocean.asset.data.db.PendingMutation;
import com.idocean.asset.data.db.SyncWorker;
import com.idocean.asset.utils.NetworkUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.dto.AssetHandoverRequestDto;
import com.idocean.asset.data.dto.AssetUpdateRequestDto;
import com.idocean.asset.data.mapper.AssetApiResponseParser;
import com.idocean.asset.data.mapper.AssetMapper;
import com.idocean.asset.data.repository.AssetErrorFormatter;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.model.Asset;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AssetMutationService {
    public interface MutationHost {
        void replaceCachedAsset(Asset originalAsset, Asset updatedAsset);

        void persistCacheAsync();
    }

    private final Handler mainHandler;
    private final LogRepository logRepository;
    private final MutationHost host;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final ExecutorService offlineMutationExecutor = Executors.newSingleThreadExecutor();

    public AssetMutationService(Handler mainHandler, LogRepository logRepository, MutationHost host) {
        this.mainHandler = mainHandler;
        this.logRepository = logRepository == null ? LogRepository.getInstance() : logRepository;
        this.host = host == null
                ? new MutationHost() {
                    @Override
                    public void replaceCachedAsset(Asset originalAsset, Asset updatedAsset) {
                    }

                    @Override
                    public void persistCacheAsync() {
                    }
                }
                : host;
    }

    public void updateAsset(Asset originalAsset, Asset asset, AssetUpdateCallback callback) {
        if (asset == null) {
            dispatchUpdateError(callback, "Khong tim thay du lieu tai san de cap nhat tai san.");
            return;
        }

        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, asset);
        if (!requestDto.hasChanges()) {
            dispatchUpdateSuccess(callback, asset, "Khong co thay doi de cap nhat.");
            return;
        }

        Context appContext = AppRuntimeContext.get();
        if (appContext != null && !NetworkUtils.isConnected(appContext)) {
            queueOfflineMutation("UPDATE", asset.getAssetCode(), gson.toJson(requestDto.getPayload()));
            host.replaceCachedAsset(originalAsset, asset);
            dispatchUpdateSuccess(callback, asset, "Đã lưu tạm thời ngoại tuyến. Dữ liệu sẽ tự động đồng bộ khi có mạng.");
            return;
        }

        executeMutationRequest(
                requestDto.getPayload(),
                requestDto,
                asset,
                "UPDATE_ASSET",
                "update-asset",
                "cap nhat tai san",
                true,
                new MutationCompletionCallback() {
                    @Override
                    public void onSuccess(UpdateResponseResult result) {
                        dispatchUpdateSuccess(callback, result.asset == null ? asset : result.asset, result.message);
                    }

                    @Override
                    public void onError(String message) {
                        dispatchUpdateError(callback, message);
                    }
                }
        );
    }

    public void handoverAsset(Asset originalAsset, Asset asset, String handoverDate, AssetUpdateCallback callback) {
        if (asset == null) {
            dispatchUpdateError(callback, "Khong tim thay du lieu tai san de ban giao.");
            return;
        }

        AssetHandoverRequestDto handoverRequestDto = AssetHandoverRequestDto.fromAssets(
                originalAsset,
                asset,
                handoverDate
        );
        if (!handoverRequestDto.hasRequiredFields()) {
            dispatchUpdateError(callback, "Thieu du lieu bat buoc de ghi nhan ban giao.");
            return;
        }

        Context appContext = AppRuntimeContext.get();
        if (appContext != null && !NetworkUtils.isConnected(appContext)) {
            queueOfflineMutation("HANDOVER", asset.getAssetCode(), gson.toJson(handoverRequestDto.getPayload()));
            AssetUpdateRequestDto updateRequestDto = AssetUpdateRequestDto.fromAssets(originalAsset, asset);
            if (updateRequestDto.hasChanges()) {
                queueOfflineMutation("UPDATE", asset.getAssetCode(), gson.toJson(updateRequestDto.getPayload()));
            }
            host.replaceCachedAsset(originalAsset, asset);
            dispatchUpdateSuccess(callback, asset, "Đã lưu tạm thời ngoại tuyến. Dữ liệu bàn giao sẽ tự động đồng bộ khi có mạng.");
            return;
        }

        executeMutationRequest(
                handoverRequestDto.getPayload(),
                null,
                asset,
                "HANDOVER_ASSET",
                "checkout-asset",
                "ghi nhan ban giao tai san",
                false,
                new MutationCompletionCallback() {
                    @Override
                    public void onSuccess(UpdateResponseResult handoverResult) {
                        AssetUpdateRequestDto updateRequestDto = AssetUpdateRequestDto.fromAssets(originalAsset, asset);
                        if (!updateRequestDto.hasChanges()) {
                            dispatchUpdateSuccess(callback, asset, handoverResult.message);
                            return;
                        }

                        executeMutationRequest(
                                updateRequestDto.getPayload(),
                                updateRequestDto,
                                asset,
                                "HANDOVER_ASSET",
                                "update-asset",
                                "cap nhat tai san sau ban giao",
                                true,
                                new MutationCompletionCallback() {
                                    @Override
                                    public void onSuccess(UpdateResponseResult updateResult) {
                                        Asset resolvedAsset = updateResult.asset == null ? asset : updateResult.asset;
                                        dispatchUpdateSuccess(
                                                callback,
                                                resolvedAsset,
                                                combineMutationMessages(handoverResult.message, updateResult.message)
                                        );
                                    }

                                    @Override
                                    public void onError(String message) {
                                        dispatchUpdateError(
                                                callback,
                                                "Da ghi nhan ban giao nhung chua cap nhat du lieu tai san. " + message
                                        );
                                    }
                                }
                        );
                    }

                    @Override
                    public void onError(String message) {
                        dispatchUpdateError(callback, message);
                    }
                }
        );
    }

    private void executeMutationRequest(
            JsonObject payload,
            AssetUpdateRequestDto requestDto,
            Asset asset,
            String logTag,
            String endpointName,
            String actionLabel,
            boolean updateCacheOnSuccess,
            MutationCompletionCallback callback
    ) {
        MutationCompletionCallback safeCallback = callback == null
                ? new MutationCompletionCallback() {
                    @Override
                    public void onSuccess(UpdateResponseResult result) {
                    }

                    @Override
                    public void onError(String message) {
                    }
                }
                : callback;
        JsonObject safePayload = payload == null ? new JsonObject() : payload;
        String safeAssetCode = asset == null ? "" : safeTrim(asset.getAssetCode());
        String requestJson = gson.toJson(safePayload);
        logRepository.logInfo(
                logTag,
                "Bat dau gui yeu cau " + actionLabel,
                safeAssetCode + " | " + buildMutationEndpointUrl(endpointName)
        );
        logRepository.logInfo(logTag, "Payload " + endpointName, abbreviateForLog(requestJson));

        Call<ResponseBody> mutationCall = "checkout-asset".equals(endpointName)
                ? ApiClient.getAssetApiService().checkoutAsset(safePayload)
                : ApiClient.getAssetApiService().updateAsset(safePayload);

        mutationCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    String errorBody = readResponseBody(response.errorBody());
                    String userMessage = AssetErrorFormatter.buildHttpMessage(
                            response.code(),
                            errorBody,
                            endpointName,
                            actionLabel
                    );
                    logRepository.logError(
                            logTag,
                            "Backend tu choi " + actionLabel,
                            "HTTP " + response.code() + " | " + AssetErrorFormatter.buildDebugSummary(
                                    endpointName,
                                    actionLabel,
                                    null,
                                    safeAssetCode + " | " + abbreviateForLog(errorBody)
                            )
                    );
                    safeCallback.onError(userMessage);
                    return;
                }

                UpdateResponseResult updateResult = parseUpdateResponse(
                        response.body(),
                        requestDto,
                        endpointName,
                        actionLabel
                );
                if (!updateResult.success) {
                    logRepository.logError(
                            logTag,
                            "Backend tra ve ket qua " + actionLabel + " khong thanh cong",
                            safeAssetCode + " | " + abbreviateForLog(updateResult.rawResponse)
                    );
                    safeCallback.onError(updateResult.message);
                    return;
                }

                Asset resolvedAsset = updateResult.asset == null ? asset : updateResult.asset;
                if (updateCacheOnSuccess && resolvedAsset != null) {
                    Asset originalIdentityAsset = requestDto == null ? resolvedAsset : requestDto.getOriginalAsset();
                    host.replaceCachedAsset(originalIdentityAsset, resolvedAsset);
                    host.persistCacheAsync();
                }
                logRepository.logInfo(
                        logTag,
                        "Da gui " + actionLabel + " len " + endpointName,
                        safeAssetCode + " | " + abbreviateForLog(updateResult.rawResponse)
                );
                safeCallback.onSuccess(UpdateResponseResult.success(updateResult.message, updateResult.rawResponse, resolvedAsset));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                String userMessage = AssetErrorFormatter.buildConnectivityMessage(endpointName, actionLabel, throwable);
                logRepository.logError(
                        logTag,
                        "Khong the " + actionLabel,
                        AssetErrorFormatter.buildDebugSummary(endpointName, actionLabel, throwable, safeAssetCode)
                );
                safeCallback.onError(userMessage);
            }
        });
    }

    static UpdateResponseResult parseUpdateResponse(
            ResponseBody responseBody,
            AssetUpdateRequestDto requestDto,
            String endpointName,
            String actionLabel
    ) {
        String rawResponse = readResponseBody(responseBody);
        if (rawResponse.isEmpty()) {
            return UpdateResponseResult.success(
                    buildMutationSuccessMessage(endpointName, actionLabel, "Vui long kiem tra DB de xac nhan."),
                    "",
                    null
            );
        }

        UpdateResponseAssetMatch assetMatch = inspectUpdateResponseAssetData(rawResponse, requestDto);

        try {
            JsonElement element = JsonParser.parseString(rawResponse);
            UpdateResponseResult parsed = parseUpdateResponseElement(
                    element,
                    rawResponse,
                    endpointName,
                    actionLabel
            );
            if (assetMatch.hasAssetData) {
                if (!parsed.success) {
                    return parsed;
                }
                if (assetMatch.matchedAsset == null) {
                    return UpdateResponseResult.error(
                            "Backend tra ve danh sach tai san nhung khong xac dinh duoc ban ghi vua cap nhat.",
                            rawResponse,
                            null
                    );
                }
                if (!requestDto.matchesReturnedAsset(assetMatch.matchedAsset)) {
                    return UpdateResponseResult.error(
                            "Backend da phan hoi nhung thay doi vua luu chua duoc ap dung dung voi tai san nay.",
                            rawResponse,
                            assetMatch.matchedAsset
                    );
                }
                return UpdateResponseResult.success(parsed.message, rawResponse, assetMatch.matchedAsset);
            }
            if (parsed != null) {
                return parsed;
            }
        } catch (RuntimeException ignored) {
        }

        return UpdateResponseResult.success(
                buildMutationSuccessMessage(
                        endpointName,
                        actionLabel,
                        "Phan hoi: " + abbreviateForUser(rawResponse)
                ),
                rawResponse,
                assetMatch.matchedAsset
        );
    }

    private static UpdateResponseResult parseUpdateResponseElement(
            JsonElement element,
            String rawResponse,
            String endpointName,
            String actionLabel
    ) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonArray()) {
            if (element.getAsJsonArray().size() <= 0) {
                return UpdateResponseResult.error(
                        "Backend " + endpointName + " tra ve mang rong va khong xac nhan duoc ban ghi da cap nhat.",
                        rawResponse,
                        null
                );
            }
            return parseUpdateResponseElement(
                    element.getAsJsonArray().get(0),
                    rawResponse,
                    endpointName,
                    actionLabel
            );
        }
        if (element.isJsonPrimitive()) {
            return UpdateResponseResult.success(
                    buildMutationSuccessMessage(
                            endpointName,
                            actionLabel,
                            "Phan hoi: " + abbreviateForUser(element.getAsString())
                    ),
                    rawResponse,
                    null
            );
        }
        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        if (object.has("json") && object.get("json").isJsonObject()) {
            UpdateResponseResult nested = parseUpdateResponseElement(
                    object.get("json"),
                    rawResponse,
                    endpointName,
                    actionLabel
            );
            if (nested != null) {
                return nested;
            }
        }

        String message = firstString(object, "message", "msg", "detail");
        String error = firstString(object, "error", "errors");
        Boolean success = firstBoolean(object, "success");
        Integer affectedRows = firstInt(object, "affectedRows", "affected_rows", "updatedRows", "updated_rows");
        String status = firstString(object, "status", "state", "result");

        if (!error.isEmpty()) {
            return UpdateResponseResult.error(
                    message.isEmpty()
                            ? AssetErrorFormatter.buildBusinessMessage(endpointName, actionLabel, error)
                            : message + ": " + error,
                    rawResponse,
                    null
            );
        }
        if (success != null) {
            if (success) {
                return UpdateResponseResult.success(
                        message.isEmpty()
                                ? buildMutationSuccessMessage(endpointName, actionLabel, "Backend xac nhan thanh cong.")
                                : message,
                        rawResponse,
                        null
                );
            }
            return UpdateResponseResult.error(
                    message.isEmpty()
                            ? AssetErrorFormatter.buildBusinessMessage(endpointName, actionLabel, "Backend bao khong thanh cong.")
                            : message,
                    rawResponse,
                    null
            );
        }
        if (affectedRows != null) {
            if (affectedRows > 0) {
                return UpdateResponseResult.success(
                        message.isEmpty()
                                ? buildMutationSuccessMessage(endpointName, actionLabel, "So dong cap nhat: " + affectedRows)
                                : message,
                        rawResponse,
                        null
                );
            }
            return UpdateResponseResult.error(
                    message.isEmpty()
                            ? AssetErrorFormatter.buildBusinessMessage(endpointName, actionLabel, "Khong co dong nao duoc cap nhat.")
                            : message,
                    rawResponse,
                    null
            );
        }
        if (!status.isEmpty()) {
            String normalizedStatus = status.trim().toLowerCase(Locale.ROOT);
            if (normalizedStatus.contains("fail") || normalizedStatus.contains("error")) {
                return UpdateResponseResult.error(
                        message.isEmpty()
                                ? AssetErrorFormatter.buildBusinessMessage(endpointName, actionLabel, "Trang thai tra ve la loi.")
                                : message,
                        rawResponse,
                        null
                );
            }
            if (normalizedStatus.contains("ok") || normalizedStatus.contains("success")) {
                return UpdateResponseResult.success(
                        message.isEmpty()
                                ? buildMutationSuccessMessage(endpointName, actionLabel, "Backend tra ve trang thai " + status)
                                : message,
                        rawResponse,
                        null
                );
            }
        }
        if (!message.isEmpty()) {
            if (looksLikeFailureMessage(message)) {
                return UpdateResponseResult.error(
                        AssetErrorFormatter.buildBusinessMessage(endpointName, actionLabel, message),
                        rawResponse,
                        null
                );
            }
            return UpdateResponseResult.success(message, rawResponse, null);
        }
        return UpdateResponseResult.success(
                buildMutationSuccessMessage(endpointName, actionLabel, "Vui long kiem tra DB de xac nhan."),
                rawResponse,
                null
        );
    }

    private static UpdateResponseAssetMatch inspectUpdateResponseAssetData(String rawResponse, AssetUpdateRequestDto requestDto) {
        if (rawResponse == null || rawResponse.trim().isEmpty() || requestDto == null) {
            return UpdateResponseAssetMatch.empty();
        }
        try {
            List<Asset> assets = AssetApiResponseParser.parseAssets(rawResponse);
            if (assets.isEmpty()) {
                return UpdateResponseAssetMatch.empty();
            }
            for (Asset asset : assets) {
                if (requestDto.matchesIdentity(asset)) {
                    return UpdateResponseAssetMatch.withMatch(asset);
                }
            }
            return UpdateResponseAssetMatch.withAssetData();
        } catch (RuntimeException ignored) {
            return UpdateResponseAssetMatch.empty();
        }
    }

    private static boolean looksLikeFailureMessage(String message) {
        String normalizedMessage = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("khong")
                || normalizedMessage.contains("chua")
                || normalizedMessage.contains("loi")
                || normalizedMessage.contains("fail")
                || normalizedMessage.contains("error")
                || normalizedMessage.contains("not found")
                || normalizedMessage.contains("no row")
                || normalizedMessage.contains("no record");
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

    private static String buildMutationEndpointUrl(String endpointName) {
        return ApiClient.getResolvedBaseUrl() + endpointName;
    }

    private static String buildMutationSuccessMessage(String endpointName, String actionLabel, String suffix) {
        String safeSuffix = suffix == null ? "" : suffix.trim();
        if (safeSuffix.isEmpty()) {
            return "Da gui " + actionLabel + " len " + endpointName + ".";
        }
        return "Da gui " + actionLabel + " len " + endpointName + ". " + safeSuffix;
    }

    private static String combineMutationMessages(String firstMessage, String secondMessage) {
        String safeFirst = safeTrim(firstMessage);
        String safeSecond = safeTrim(secondMessage);
        if (safeFirst.isEmpty()) {
            return safeSecond;
        }
        if (safeSecond.isEmpty()) {
            return safeFirst;
        }
        if (safeFirst.equals(safeSecond)) {
            return safeFirst;
        }
        return safeFirst + " " + safeSecond;
    }

    private static String abbreviateForLog(String value) {
        String safe = value == null ? "" : value.trim();
        if (safe.length() <= 320) {
            return safe;
        }
        return safe.substring(0, 320) + "...";
    }

    private static String abbreviateForUser(String value) {
        String safe = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (safe.length() <= 120) {
            return safe;
        }
        return safe.substring(0, 120) + "...";
    }

    private static String firstString(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return "";
        }
        for (String currentKey : object.keySet()) {
            for (String key : keys) {
                if (normalizeResponseKey(currentKey).equals(normalizeResponseKey(key))) {
                    JsonElement element = object.get(currentKey);
                    if (element == null || element.isJsonNull()) {
                        continue;
                    }
                    try {
                        String value = element.getAsString();
                        if (value != null && !value.trim().isEmpty()) {
                            return value.trim();
                        }
                    } catch (ClassCastException | IllegalStateException ignored) {
                    }
                }
            }
        }
        return "";
    }

    private static Boolean firstBoolean(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return null;
        }
        for (String currentKey : object.keySet()) {
            for (String key : keys) {
                if (normalizeResponseKey(currentKey).equals(normalizeResponseKey(key))) {
                    JsonElement element = object.get(currentKey);
                    if (element == null || element.isJsonNull()) {
                        continue;
                    }
                    try {
                        return element.getAsBoolean();
                    } catch (ClassCastException | IllegalStateException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static Integer firstInt(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return null;
        }
        for (String currentKey : object.keySet()) {
            for (String key : keys) {
                if (normalizeResponseKey(currentKey).equals(normalizeResponseKey(key))) {
                    JsonElement element = object.get(currentKey);
                    if (element == null || element.isJsonNull()) {
                        continue;
                    }
                    try {
                        return element.getAsInt();
                    } catch (ClassCastException | IllegalStateException | NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private static String normalizeResponseKey(String value) {
        return AssetMapper.normalizeHeader(value);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void dispatchUpdateSuccess(AssetUpdateCallback callback, Asset asset, String message) {
        if (callback == null) {
            return;
        }
        if (mainHandler == null) {
            callback.onSuccess(asset, message);
            return;
        }
        mainHandler.post(() -> callback.onSuccess(asset, message));
    }

    private void dispatchUpdateError(AssetUpdateCallback callback, String message) {
        if (callback == null) {
            return;
        }
        if (mainHandler == null) {
            callback.onError(message);
            return;
        }
        mainHandler.post(() -> callback.onError(message));
    }

    private interface MutationCompletionCallback {
        void onSuccess(UpdateResponseResult result);

        void onError(String message);
    }

    static final class UpdateResponseResult {
        final boolean success;
        final String message;
        final String rawResponse;
        final Asset asset;

        private UpdateResponseResult(boolean success, String message, String rawResponse, Asset asset) {
            this.success = success;
            this.message = message == null || message.trim().isEmpty()
                    ? "Khong nhan duoc thong diep phan hoi tu backend."
                    : message.trim();
            this.rawResponse = rawResponse == null ? "" : rawResponse;
            this.asset = asset;
        }

        static UpdateResponseResult success(String message, String rawResponse, Asset asset) {
            return new UpdateResponseResult(true, message, rawResponse, asset);
        }

        static UpdateResponseResult error(String message, String rawResponse, Asset asset) {
            return new UpdateResponseResult(false, message, rawResponse, asset);
        }
    }

    private static final class UpdateResponseAssetMatch {
        final boolean hasAssetData;
        final Asset matchedAsset;

        private UpdateResponseAssetMatch(boolean hasAssetData, Asset matchedAsset) {
            this.hasAssetData = hasAssetData;
            this.matchedAsset = matchedAsset;
        }

        static UpdateResponseAssetMatch empty() {
            return new UpdateResponseAssetMatch(false, null);
        }

        static UpdateResponseAssetMatch withAssetData() {
            return new UpdateResponseAssetMatch(true, null);
        }

        static UpdateResponseAssetMatch withMatch(Asset asset) {
            return new UpdateResponseAssetMatch(true, asset);
        }
    }

    private void queueOfflineMutation(String actionType, String assetCode, String payloadJson) {
        Context appContext = AppRuntimeContext.get();
        if (appContext == null) return;

        offlineMutationExecutor.execute(() -> {
            try {
                PendingMutation mutation = new PendingMutation(
                        actionType,
                        assetCode,
                        payloadJson,
                        System.currentTimeMillis()
                );
                AppDatabase.getInstance(appContext).pendingMutationDao().insert(mutation);
                logRepository.logInfo(
                        "OFFLINE_QUEUE",
                        "Da xep hang thao tac offline (" + actionType + ")",
                        assetCode
                );

                enqueueOfflineSyncWork(appContext);
            } catch (Exception e) {
                logRepository.logError(
                        "OFFLINE_QUEUE",
                        "Loi khi xep hang thao tac offline",
                        e.getMessage()
                );
            }
        });
    }

    private void enqueueOfflineSyncWork(Context context) {
        try {
            androidx.work.Constraints constraints = new androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build();

            androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(SyncWorker.class)
                    .setConstraints(constraints)
                    .addTag("offline_sync_work")
                    .build();

            androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                            "offline_sync_work_unique",
                            androidx.work.ExistingWorkPolicy.KEEP,
                            syncRequest
                    );
        } catch (Exception e) {
            logRepository.logError("OFFLINE_WORK", "Khong the khoi chay WorkManager", e.getMessage());
        }
    }
}
