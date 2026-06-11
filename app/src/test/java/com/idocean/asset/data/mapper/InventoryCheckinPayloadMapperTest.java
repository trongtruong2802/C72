package com.idocean.asset.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.idocean.asset.data.dto.InventoryCheckinBatchRequestDto;
import com.idocean.asset.data.dto.InventoryCheckinRequestItemDto;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.InventoryScanSource;
import com.idocean.asset.model.InventorySessionItem;

import org.junit.Test;

import java.util.Arrays;

public class InventoryCheckinPayloadMapperTest {
    @Test
    public void buildRequest_onlyKeepsItemsEligibleForWorkflowInsert() {
        InventoryCheckinPayloadMapper mapper = new InventoryCheckinPayloadMapper();

        InventorySessionItem checkedItem = InventorySessionItem.fromAsset(
                "ASSET:TID-01:0",
                asset("CODE-01", "TID-01", "Laptop A")
        );
        checkedItem.markScanned(
                InventoryScanSource.RFID,
                "operator-a",
                "note-a",
                1712318071000L,
                "CODE-01",
                "TID-01",
                "E200001122"
        );

        InventorySessionItem missingItem = InventorySessionItem.fromAsset(
                "ASSET:TID-02:1",
                asset("CODE-02", "TID-02", "Laptop B")
        );

        InventorySessionItem outsideItem = InventorySessionItem.createOutsideQr(
                "OUTSIDE_QR:CODE-03",
                "CODE-03",
                1712318099000L,
                "operator-b",
                "note-b"
        );

        InventoryCheckinBatchRequestDto request = mapper.buildRequest(Arrays.asList(checkedItem, missingItem));

        request = mapper.buildRequest(Arrays.asList(checkedItem, missingItem, outsideItem));

        assertEquals(2, request.size());

        InventoryCheckinRequestItemDto firstItem = request.getItems().get(0);
        assertEquals("CODE-01", firstItem.getCode());
        assertEquals("TID-01", firstItem.getTid());
        assertEquals("E200001122", firstItem.getEpcHex());
        assertEquals("RFID", firstItem.getScanSource());
        assertEquals(InventoryCheckinPayloadMapper.formatApiTimestamp(1712318071000L), firstItem.getScannedAt());
        assertEquals("\u0110\u00e3 ki\u1ec3m k\u00ea", firstItem.getInventoryStatus());
        assertEquals("operator-a", firstItem.getOperator());
        assertEquals("note-a", firstItem.getNote());

        InventoryCheckinRequestItemDto secondItem = request.getItems().get(1);
        assertEquals("CODE-03", secondItem.getCode());
        assertNull(secondItem.getTid());
        assertNull(secondItem.getEpcHex());
        assertEquals("QR", secondItem.getScanSource());
        assertEquals("Ngo\u00e0i danh s\u00e1ch", secondItem.getInventoryStatus());
        assertEquals(InventoryCheckinPayloadMapper.formatApiTimestamp(1712318099000L), secondItem.getScannedAt());
    }

    @Test
    public void isEligibleForUpload_requiresScannedAtAndAllowedStatus() {
        InventorySessionItem missingItem = InventorySessionItem.fromAsset(
                "ASSET:TID-02:1",
                asset("CODE-02", "TID-02", "Laptop B")
        );
        assertTrue(!InventoryCheckinPayloadMapper.isEligibleForUpload(missingItem));

        InventorySessionItem checkedItem = InventorySessionItem.fromAsset(
                "ASSET:TID-01:0",
                asset("CODE-01", "TID-01", "Laptop A")
        );
        checkedItem.markScanned(
                InventoryScanSource.RFID,
                "operator-a",
                "note-a",
                1712318071000L,
                "CODE-01",
                "TID-01",
                "E200001122"
        );
        assertTrue(InventoryCheckinPayloadMapper.isEligibleForUpload(checkedItem));
    }

    private Asset asset(String code, String tid, String name) {
        return new Asset(
                1,
                code,
                tid,
                "",
                "",
                name,
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
        );
    }
}
