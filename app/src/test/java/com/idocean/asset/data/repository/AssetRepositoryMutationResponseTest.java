package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.idocean.asset.data.dto.AssetUpdateRequestDto;
import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

public class AssetRepositoryMutationResponseTest {

    @Test
    public void parseUpdateResponse_treatsAffectedRowsAsSuccess() {
        AssetMutationService.UpdateResponseResult result = invokeParseUpdateResponse(
                "{\"affectedRows\":1,\"message\":\"Cap nhat thanh cong\"}",
                null,
                "update-asset",
                "cap nhat tai san"
        );

        assertTrue(result.success);
        assertEquals("Cap nhat thanh cong", result.message);
    }

    @Test
    public void parseUpdateResponse_treatsExplicitFailureAsError() {
        AssetMutationService.UpdateResponseResult result = invokeParseUpdateResponse(
                "{\"success\":false,\"message\":\"Khong tim thay tai san\"}",
                null,
                "update-asset",
                "cap nhat tai san"
        );

        assertFalse(result.success);
        assertEquals("Khong tim thay tai san", result.message);
    }

    @Test
    public void parseUpdateResponse_treatsCheckoutEndpointAsSuccessWhenBackendConfirmsRows() {
        AssetMutationService.UpdateResponseResult result = invokeParseUpdateResponse(
                "{\"affectedRows\":1,\"message\":\"Da ghi nhan ban giao\"}",
                null,
                "checkout-asset",
                "ghi nhan ban giao tai san"
        );

        assertTrue(result.success);
        assertEquals("Da ghi nhan ban giao", result.message);
    }

    @Test
    public void parseUpdateResponse_returnsMatchedAssetWhenBackendEchoesAppliedChanges() {
        Asset originalAsset = asset("AREXAT", "E280001", "BOD", "Truong Vu", "Idoplex-5");
        Asset updatedAsset = asset("AREXAT", "E280001", "IT", "Thang Nguyen", "L\u1ea7u 2 - TT16");
        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);

        AssetMutationService.UpdateResponseResult result = invokeParseUpdateResponse(
                "{"
                        + "\"data\":[{"
                        + "\"code\":\"AREXAT\","
                        + "\"tid\":\"E280001\","
                        + "\"department\":\"IT\","
                        + "\"assignedUser\":\"Thang Nguyen\","
                        + "\"location\":\"Idoplex-2\","
                        + "\"asset_name\":\"Laptop Dell\""
                        + "}]"
                        + "}",
                requestDto,
                "update-asset",
                "cap nhat tai san"
        );

        assertTrue(result.success);
        Asset matchedAsset = result.asset;
        assertNotNull(matchedAsset);
        assertEquals("AREXAT", matchedAsset.getAssetCode());
    }

    @Test
    public void parseUpdateResponse_returnsErrorWhenBackendAssetDoesNotReflectChanges() {
        Asset originalAsset = asset("AREXAT", "E280001", "BOD", "Truong Vu", "Idoplex-5");
        Asset updatedAsset = asset("AREXAT", "E280001", "IT", "Thang Nguyen", "L\u1ea7u 2 - TT16");
        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);

        AssetMutationService.UpdateResponseResult result = invokeParseUpdateResponse(
                "{"
                        + "\"data\":[{"
                        + "\"code\":\"AREXAT\","
                        + "\"tid\":\"E280001\","
                        + "\"department\":\"IT\","
                        + "\"assignedUser\":\"Truong Vu\","
                        + "\"location\":\"Idoplex-2\","
                        + "\"asset_name\":\"Laptop Dell\""
                        + "}]"
                        + "}",
                requestDto,
                "update-asset",
                "cap nhat tai san"
        );

        assertFalse(result.success);
        assertTrue(result.message.contains("chua duoc ap dung"));
    }

    @Test
    public void parseUpdateResponse_matchesAssetWhenBackendReturnsUpdatedCode() {
        Asset originalAsset = asset("", "E280001", "BOD", "Truong Vu", "Idoplex-5");
        Asset updatedAsset = asset("AREXAT-NEW", "E280001", "BOD", "Truong Vu", "Idoplex-5");
        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);

        AssetMutationService.UpdateResponseResult result = invokeParseUpdateResponse(
                "{"
                        + "\"data\":[{"
                        + "\"code\":\"AREXAT-NEW\","
                        + "\"tid\":\"E280001\","
                        + "\"department\":\"BOD\","
                        + "\"assignedUser\":\"Truong Vu\","
                        + "\"location\":\"Idoplex-5\","
                        + "\"asset_name\":\"Laptop Dell\""
                        + "}]"
                        + "}",
                requestDto,
                "update-asset",
                "cap nhat tai san"
        );

        assertTrue(result.success);
        assertNotNull(result.asset);
        assertEquals("AREXAT-NEW", result.asset.getAssetCode());
    }

    private AssetMutationService.UpdateResponseResult invokeParseUpdateResponse(
            String rawBody,
            AssetUpdateRequestDto requestDto,
            String endpointName,
            String actionLabel
    ) {
        return AssetMutationService.parseUpdateResponse(
                rawBody == null ? null : responseBody(rawBody),
                requestDto,
                endpointName,
                actionLabel
        );
    }

    private static Asset asset(String code, String tid, String department, String assignedUser, String location) {
        return new Asset(
                1,
                code,
                tid,
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                department,
                assignedUser,
                location,
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
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
}
