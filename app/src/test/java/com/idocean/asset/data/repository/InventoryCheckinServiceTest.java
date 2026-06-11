package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.idocean.asset.data.api.AssetApiService;
import com.idocean.asset.data.mapper.InventoryCheckinPayloadMapper;
import com.idocean.asset.data.mapper.InventoryCheckinResponseParser;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.InventoryScanSource;
import com.idocean.asset.model.InventorySessionItem;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InventoryCheckinServiceTest {
    @Test
    public void buildCheckinEndpointUrl_reusesWebhookBaseUrlWhenAlreadyPresent() {
        String endpointUrl = InventoryCheckinService.buildCheckinEndpointUrl(
                "https://n8n.idocean.info:8443/webhook/"
        );

        assertEquals("https://n8n.idocean.info:8443/webhook/checkin-assets", endpointUrl);
    }

    @Test
    public void buildCheckinEndpointUrl_appendsWebhookWhenBaseUrlDoesNotContainIt() {
        String endpointUrl = InventoryCheckinService.buildCheckinEndpointUrl(
                "https://n8n.idocean.info:8443/"
        );

        assertEquals("https://n8n.idocean.info:8443/webhook/checkin-assets", endpointUrl);
    }

    @Test
    public void uploadRequest_returnsSuccessOutcomeWhenInsertedRowsExist() throws Exception {
        InventoryCheckinService service = new InventoryCheckinService(
                LogRepository.getInstance(),
                new FakeAssetApiService(successResponseBody(
                        "{\"success\":true,\"message\":\"Xu ly batch kiem ke thanh cong\",\"total_received\":2,\"total_scanned_valid\":1,\"total_skipped\":1,\"total_inserted\":1}"
                )),
                new InventoryCheckinPayloadMapper(),
                new InventoryCheckinResponseParser()
        );

        InventoryCheckinUploadResult result = service.uploadSnapshot(
                Collections.singletonList(scannedItem())
        );

        assertTrue(result.isSuccess());
        assertEquals(1, result.getResponse().getTotalInserted());
    }

    @Test
    public void uploadRequest_returnsWarningOutcomeWhenBackendInsertedNothing() throws Exception {
        InventoryCheckinService service = new InventoryCheckinService(
                LogRepository.getInstance(),
                new FakeAssetApiService(successResponseBody(
                        "{\"success\":true,\"message\":\"Khong co tai san hop le\",\"total_received\":2,\"total_inserted\":0}"
                )),
                new InventoryCheckinPayloadMapper(),
                new InventoryCheckinResponseParser()
        );

        InventoryCheckinUploadResult result = service.uploadSnapshot(
                Collections.singletonList(scannedItem())
        );

        assertTrue(result.isWarning());
        assertEquals(0, result.getResponse().getTotalInserted());
    }

    @Test
    public void uploadRequest_throwsWhenBackendReturnsBusinessFailure() {
        InventoryCheckinService service = new InventoryCheckinService(
                LogRepository.getInstance(),
                new FakeAssetApiService(successResponseBody(
                        "{\"success\":false,\"message\":\"Invalid input\"}"
                )),
                new InventoryCheckinPayloadMapper(),
                new InventoryCheckinResponseParser()
        );

        try {
            service.uploadSnapshot(Collections.singletonList(scannedItem()));
        } catch (InventoryCheckinService.UploadFailureException exception) {
            assertTrue(exception.getMessage().contains("khong thanh cong"));
            return;
        }

        throw new AssertionError("Expected upload failure");
    }

    @Test
    public void uploadRequest_throwsWhenBackendReturnsHttpFailure() {
        InventoryCheckinService service = new InventoryCheckinService(
                LogRepository.getInstance(),
                new FakeAssetApiService(errorResponseBody(500, "{\"message\":\"Server error\"}")),
                new InventoryCheckinPayloadMapper(),
                new InventoryCheckinResponseParser()
        );

        try {
            service.uploadSnapshot(Collections.singletonList(scannedItem()));
        } catch (InventoryCheckinService.UploadFailureException exception) {
            assertTrue(exception.getMessage().contains("500"));
            return;
        }

        throw new AssertionError("Expected HTTP failure");
    }

    @Test
    public void uploadRequest_throwsConnectivityMessageWhenExecuteFails() {
        InventoryCheckinService service = new InventoryCheckinService(
                LogRepository.getInstance(),
                new FailingAssetApiService(new IOException("timeout")),
                new InventoryCheckinPayloadMapper(),
                new InventoryCheckinResponseParser()
        );

        try {
            service.uploadSnapshot(Collections.singletonList(scannedItem()));
        } catch (InventoryCheckinService.UploadFailureException exception) {
            assertTrue(exception.getMessage().toLowerCase().contains("qua thoi gian")
                    || exception.getMessage().toLowerCase().contains("khong ket noi"));
            return;
        }

        throw new AssertionError("Expected connectivity failure");
    }

    private static InventorySessionItem scannedItem() {
        InventorySessionItem item = InventorySessionItem.fromAsset(
                "ASSET:TID-01:0",
                new Asset(
                        1,
                        "CODE-01",
                        "TID-01",
                        "",
                        "",
                        "Laptop A",
                        "Laptop",
                        "SN-01",
                        "IT",
                        "User A",
                        "Lau 5 - TT16",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "API"
                )
        );
        item.markScanned(
                InventoryScanSource.RFID,
                "operator-a",
                "note-a",
                1712318071000L,
                "CODE-01",
                "TID-01",
                "E200001122"
        );
        return item;
    }

    private static Response<ResponseBody> successResponseBody(String rawBody) {
        return Response.success(responseBody(rawBody));
    }

    private static Response<ResponseBody> errorResponseBody(int httpCode, String rawBody) {
        return Response.error(httpCode, responseBody(rawBody));
    }

    private static ResponseBody responseBody(String rawBody) {
        return new ResponseBody() {
            @Override
            public okhttp3.MediaType contentType() {
                return null;
            }

            @Override
            public long contentLength() {
                return rawBody.getBytes(StandardCharsets.UTF_8).length;
            }

            @Override
            public BufferedSource source() {
                return new Buffer().writeString(rawBody, StandardCharsets.UTF_8);
            }
        };
    }

    private static final class FakeAssetApiService implements AssetApiService {
        private final Response<ResponseBody> response;

        FakeAssetApiService(Response<ResponseBody> response) {
            this.response = response;
        }

        @Override
        public Call<ResponseBody> getAssets() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> getAssets(java.util.Map<String, String> queryMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> getAssets(String url) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> updateAsset(JsonObject requestDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> checkoutAsset(JsonObject requestDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> checkinAssets(String url, JsonObject requestDto) {
            return new FakeCall(response);
        }
    }

    private static final class FailingAssetApiService implements AssetApiService {
        private final IOException exception;

        FailingAssetApiService(IOException exception) {
            this.exception = exception;
        }

        @Override
        public Call<ResponseBody> getAssets() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> getAssets(java.util.Map<String, String> queryMap) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> getAssets(String url) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> updateAsset(JsonObject requestDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> checkoutAsset(JsonObject requestDto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Call<ResponseBody> checkinAssets(String url, JsonObject requestDto) {
            return new FailingCall(exception);
        }
    }

    private static final class FakeCall implements Call<ResponseBody> {
        private final Response<ResponseBody> response;

        FakeCall(Response<ResponseBody> response) {
            this.response = response;
        }

        @Override
        public Response<ResponseBody> execute() throws IOException {
            return response;
        }

        @Override
        public void enqueue(Callback<ResponseBody> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public Call<ResponseBody> clone() {
            return new FakeCall(response);
        }

        @Override
        public Request request() {
            return new Request.Builder().url("https://n8n.idocean.info:8443/webhook/checkin-assets").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }

    private static final class FailingCall implements Call<ResponseBody> {
        private final IOException exception;

        FailingCall(IOException exception) {
            this.exception = exception;
        }

        @Override
        public Response<ResponseBody> execute() throws IOException {
            throw exception;
        }

        @Override
        public void enqueue(Callback<ResponseBody> callback) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isExecuted() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public Call<ResponseBody> clone() {
            return new FailingCall(exception);
        }

        @Override
        public Request request() {
            return new Request.Builder().url("https://n8n.idocean.info:8443/webhook/checkin-assets").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
