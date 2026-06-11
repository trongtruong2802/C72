package com.idocean.asset.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.idocean.asset.model.Asset;

import org.junit.Test;

import java.util.List;

public class AssetUpdateRequestDtoTest {

    @Test
    public void fromAssets_includesAssignedUserCamelCaseKey() {
        Asset originalAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "BOD",
                "Truong Vu",
                "Lầu 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "IT",
                "Thang Nguyen",
                "Lầu 2 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);
        JsonObject payload = requestDto.getPayload();

        assertTrue(requestDto.hasChanges());
        assertTrue(payload.has("assignedUser"));
        assertEquals("Thang Nguyen", payload.get("assignedUser").getAsString());
    }

    @Test
    public void fromAssets_treatsNormalizedDepartmentAndLocationAsUnchanged() {
        Asset originalAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "Finance & Accountant",
                "Truong Vu",
                "Idoplex-5",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "Finance and Accountant",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);

        assertFalse(requestDto.hasChanges());
        assertTrue(requestDto.getChangedFields().isEmpty());
    }

    @Test
    public void matchesReturnedAsset_acceptsCanonicalAndLegacyEquivalentLocationValues() {
        Asset originalAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "BOD",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "IT",
                "Thang Nguyen",
                "L\u1ea7u 2 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset returnedAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "IT",
                "Thang Nguyen",
                "Idoplex-2",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);

        assertTrue(requestDto.matchesReturnedAsset(returnedAsset));
    }

    @Test
    public void fromAssets_keepsOriginalCodeForMatchingWhenCodeChanges() {
        Asset originalAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "BOD",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                1,
                "AREXAT-NEW",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "BOD",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);
        JsonObject payload = requestDto.getPayload();
        List<String> changedFields = requestDto.getChangedFields();

        assertTrue(requestDto.hasChanges());
        assertTrue(changedFields.contains("code"));
        assertEquals("AREXAT", payload.get("original_code").getAsString());
        assertEquals("AREXAT", payload.get("match_code").getAsString());
        assertEquals("AREXAT-NEW", payload.get("code").getAsString());
        assertEquals("AREXAT-NEW", payload.get("asset_code").getAsString());
        assertEquals("AREXAT-NEW", payload.get("new_code").getAsString());
    }

    @Test
    public void fromAssets_includesWebhookCompatCamelCaseBody() {
        Asset originalAsset = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "R6WK5L",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "BOD",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                1,
                "AREXAT-NEW",
                "E2801190200089A73CC203CA",
                "R6WK5L",
                "",
                "Laptop Dell Inspiron 15 7000 Gaming",
                "Laptop",
                "7R1BJ22",
                "IT",
                "truongvt@idocean.com",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "Test",
                "API"
        );

        AssetUpdateRequestDto requestDto = AssetUpdateRequestDto.fromAssets(originalAsset, updatedAsset);
        JsonObject payload = requestDto.getPayload();

        assertEquals("AREXAT-NEW", payload.get("code").getAsString());
        assertEquals("E2801190200089A73CC203CA", payload.get("tid").getAsString());
        assertEquals("R6WK5L", payload.get("oldCode").getAsString());
        assertEquals("", payload.get("oldSerial").getAsString());
        assertEquals("Laptop Dell Inspiron 15 7000 Gaming", payload.get("assetName").getAsString());
        assertEquals("Laptop", payload.get("assetType").getAsString());
        assertEquals("7R1BJ22", payload.get("serialNumber").getAsString());
        assertEquals("IT", payload.get("department").getAsString());
        assertEquals("truongvt@idocean.com", payload.get("assignedUser").getAsString());
        assertEquals("L\u1ea7u 5 - TT16", payload.get("location").getAsString());
        assertEquals("Dang su dung", payload.get("inventoryStatus").getAsString());
        assertEquals("Dang su dung", payload.get("assetCondition").getAsString());
        assertEquals("Test", payload.get("note").getAsString());
        assertEquals("AREXAT", payload.get("match_code").getAsString());
        assertEquals("AREXAT-NEW", payload.get("new_code").getAsString());
    }
}
