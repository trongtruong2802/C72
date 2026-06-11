package com.idocean.asset.data.mapper;

import com.google.gson.JsonParseException;
import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class AssetApiResponseParserTest {

    @Test
    public void parseAssets_supportsEnvelopeArrayWithVietnameseColumns() {
        String rawResponse = "[{" +
                "\"success\":true," +
                "\"total\":1," +
                "\"data\":[{" +
                "\"stt\":1," +
                "\"Số seri\":\"AFEFTO\"," +
                "\"Tên nhãn hiệu, quy cách vật tư, dụng cụ\":\"Switch Netgear GS324T 24 Port\"," +
                "\"Phân loại cấp 2\":\"Switch\"," +
                "\"SerialNumber\":\"5LE1935A00397\"," +
                "\"Bộ phận sử dụng\":\"IT\"," +
                "\"Người sử dụng\":\"Phi Nguyen\"," +
                "\"Vị trí/ Địa điểm\":\"Lầu 3 - TT16\"," +
                "\"Tình trạng\":\"Đang sử dụng\"," +
                "\"Ghi chú\":\"New 2025\"," +
                "\"Old_Serial\":\"\"," +
                "\"TID\":\"#N/A\"" +
                "}]" +
                "}]";

        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawResponse);
        List<Asset> assets = AssetApiResponseParser.parseAssets(rawResponse);

        assertTrue(pageResult.hasExplicitTotalCount);
        assertEquals(1, pageResult.totalCount);
        assertEquals(1, assets.size());
        Asset asset = assets.get(0);
        assertEquals(Integer.valueOf(1), asset.getRowNumber());
        assertEquals("AFEFTO", asset.getAssetCode());
        assertEquals("", asset.getTid());
        assertEquals("Switch Netgear GS324T 24 Port", asset.getAssetName());
        assertEquals("Switch", asset.getAssetType());
        assertEquals("5LE1935A00397", asset.getSerialNumber());
        assertEquals("IT", asset.getDepartment());
        assertEquals("Phi Nguyen", asset.getAssignedUser());
        assertEquals("Lầu 3 - TT16", asset.getLocation());
        assertEquals("Đang sử dụng", asset.getAssetCondition());
        assertEquals("New 2025", asset.getNote());
    }

    @Test
    public void parseAssets_throwsWhenBackendReturnsFailureEnvelope() {
        String rawResponse = "{\"success\":false,\"message\":\"Khong doc duoc du lieu\"}";

        try {
            AssetApiResponseParser.parseAssets(rawResponse);
        } catch (JsonParseException exception) {
            assertTrue(exception.getMessage().contains("Khong doc duoc du lieu"));
            return;
        }

        throw new AssertionError("Expected JsonParseException");
    }

    @Test
    public void parsePageResult_marksUnknownTotalWhenBackendDoesNotReturnCount() {
        String rawResponse = "{\"data\":[{\"code\":\"TS-001\",\"tid\":\"E280\"}]}";

        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawResponse);

        assertFalse(pageResult.hasExplicitTotalCount);
        assertEquals(1, pageResult.totalCount);
    }

    @Test
    public void parsePageResult_supportsNestedJsonEnvelopeAndTotalCountAlias() {
        String rawResponse = "{"
                + "\"json\":{"
                + "\"items\":[{"
                + "\"code\":\"TS-001\","
                + "\"tid\":\"E280ABC\","
                + "\"name\":\"Laptop Dell\""
                + "}],"
                + "\"record_count\":5"
                + "}"
                + "}";

        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawResponse);
        List<Asset> assets = AssetApiResponseParser.parseAssets(rawResponse);

        assertTrue(pageResult.hasExplicitTotalCount);
        assertEquals(5, pageResult.totalCount);
        assertEquals(1, assets.size());
        assertEquals("TS-001", assets.get(0).getAssetCode());
    }

    @Test
    public void parsePageResult_supportsTotalCountAliasFromDirectEnvelope() {
        String rawResponse = "{"
                + "\"data\":[{"
                + "\"code\":\"TS-002\","
                + "\"tid\":\"E280XYZ\""
                + "}],"
                + "\"total_count\":12"
                + "}";

        AssetApiResponseParser.AssetPageResult pageResult = AssetApiResponseParser.parsePageResult(rawResponse);
        List<Asset> assets = AssetApiResponseParser.parseAssets(rawResponse);

        assertTrue(pageResult.hasExplicitTotalCount);
        assertEquals(12, pageResult.totalCount);
        assertEquals(1, assets.size());
        assertEquals("TS-002", assets.get(0).getAssetCode());
    }

    @Test
    public void mapAssetsAllowEmpty_skipsRowsWithoutMeaningfulAssetData() {
        String rawArray = "["
                + "{\"note\":\"header\"},"
                + "{\"code\":\"TS-001\",\"tid\":\"E280ABC\",\"name\":\"Laptop Dell\"}"
                + "]";

        List<Asset> assets = AssetApiResponseParser.mapAssetsAllowEmpty(
                com.google.gson.JsonParser.parseString(rawArray).getAsJsonArray()
        );

        assertEquals(1, assets.size());
        assertEquals("TS-001", assets.get(0).getAssetCode());
        assertEquals(Integer.valueOf(1), assets.get(0).getRowNumber());
    }
}
