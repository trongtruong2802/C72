package com.idocean.asset.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.idocean.asset.data.api.ApiClient;
import com.idocean.asset.data.repository.LogRepository;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        LogRepository logRepository = LogRepository.getInstance();

        List<PendingMutation> pendingMutations = db.pendingMutationDao().getAllPending();
        if (pendingMutations == null || pendingMutations.isEmpty()) {
            return Result.success();
        }

        logRepository.logInfo("SYNC_WORKER", "Bat dau dong bo online", pendingMutations.size() + " request(s)");

        for (PendingMutation mutation : pendingMutations) {
            try {
                JsonObject payloadJson = JsonParser.parseString(mutation.getPayload()).getAsJsonObject();
                Response<ResponseBody> response;
                
                if ("HANDOVER".equals(mutation.getActionType())) {
                    response = ApiClient.getAssetApiService().checkoutAsset(payloadJson).execute();
                } else {
                    response = ApiClient.getAssetApiService().updateAsset(payloadJson).execute();
                }

                if (response.isSuccessful()) {
                    db.pendingMutationDao().delete(mutation);
                    logRepository.logInfo("SYNC_WORKER", "Dong bo thanh cong offline " + mutation.getActionType(), mutation.getAssetCode());
                } else if (response.code() >= 400 && response.code() < 500) {
                    db.pendingMutationDao().delete(mutation);
                    logRepository.logError("SYNC_WORKER", "Bo qua loi client: " + response.code(), mutation.getAssetCode());
                } else {
                    logRepository.logError("SYNC_WORKER", "Loi server: " + response.code(), mutation.getAssetCode());
                    return Result.retry();
                }
            } catch (IOException e) {
                logRepository.logError("SYNC_WORKER", "Loi ket noi khi dong bo", e.getMessage());
                return Result.retry();
            } catch (Exception e) {
                db.pendingMutationDao().delete(mutation);
                logRepository.logError("SYNC_WORKER", "Bo qua loi phan tich payload", e.getMessage());
            }
        }

        return Result.success();
    }
}
