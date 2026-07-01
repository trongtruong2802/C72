package com.idocean.asset.data.mapper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.idocean.asset.model.Asset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssetMapperTest {

    @Test
    public void normalizeHeader_handlesVietnameseColumnNames() {
        assertEquals("so_seri", AssetMapper.normalizeHeader("Số seri"));
        assertEquals("bo_phan_su_dung", AssetMapper.normalizeHeader("Bộ phận sử dụng"));
        assertEquals("vi_tri_dia_diem", AssetMapper.normalizeHeader("Vị trí/ Địa điểm"));
    }

    @Test
    public void fromApiJson_mapsAliasesAndFallbackRowNumber() {
        JsonObject object = JsonParser.parseString("{" +
                "\"Số seri\":\"AFITEW\"," +
                "\"Tên nhãn hiệu, quy cách vật tư, dụng cụ\":\"Adapter máy Laptop HP Pavilion Gaming 15\"," +
                "\"Phân loại cấp 2\":\"Adapter\"," +
                "\"Bộ phận sử dụng\":\"IT\"," +
                "\"Người sử dụng\":\"Thang Nguyen\"," +
                "\"Vị trí/ Địa điểm\":\"Lầu 5 - TT16\"," +
                "\"Tình trạng\":\"Đang sử dụng\"," +
                "\"Old_Serial\":\"\"," +
                "\"TID\":\"E2801190200093F73CC803CA\"" +
                "}").getAsJsonObject();

        Asset asset = AssetMapper.fromApiJson(object, 7);

        assertEquals(Integer.valueOf(7), asset.getRowNumber());
        assertEquals("AFITEW", asset.getAssetCode());
        assertEquals("Adapter máy Laptop HP Pavilion Gaming 15", asset.getAssetName());
        assertEquals("Adapter", asset.getAssetType());
        assertEquals("IT", asset.getDepartment());
        assertEquals("Thang Nguyen", asset.getAssignedUser());
        assertEquals("Lầu 5 - TT16", asset.getLocation());
        assertEquals("Đang sử dụng", asset.getAssetCondition());
        assertEquals("E2801190200093F73CC803CA", asset.getTid());
        assertTrue(AssetMapper.hasMeaningfulData(asset));
    }

    @Test
    public void fromApiJson_mapsInventoryStatusAliases() {
        JsonObject object = JsonParser.parseString("{" +
                "\"Code\":\"TEST_CODE\"," +
                "\"Trạng thái sử dụng\":\"Đang sử dụng\"" +
                "}").getAsJsonObject();

        Asset asset = AssetMapper.fromApiJson(object, 1);

        assertEquals("TEST_CODE", asset.getAssetCode());
        assertEquals("Đang sử dụng", asset.getInventoryStatus());
    }
}
