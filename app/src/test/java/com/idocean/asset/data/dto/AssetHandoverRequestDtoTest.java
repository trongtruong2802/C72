package com.idocean.asset.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.idocean.asset.model.Asset;

import org.junit.Test;

public class AssetHandoverRequestDtoTest {

    @Test
    public void fromAssets_buildsExpectedCheckoutPayload() {
        Asset originalAsset = new Asset(
                12,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "HR",
                "Thang Nguyen",
                "Idoplex-5",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                12,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "IT",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "Ban giao",
                "API"
        );

        AssetHandoverRequestDto requestDto = AssetHandoverRequestDto.fromAssets(
                originalAsset,
                updatedAsset,
                "2026-04-08"
        );

        JsonObject payload = requestDto.getPayload();

        assertTrue(requestDto.hasRequiredFields());
        assertEquals("AREXAT", payload.get("code").getAsString());
        assertEquals("E2801190200089A73CC203CA", payload.get("tid").getAsString());
        assertEquals("Thang Nguyen", payload.get("assignedUser").getAsString());
        assertEquals("Thang Nguyen", payload.get("fromUser").getAsString());
        assertEquals("HR", payload.get("oldDepartment").getAsString());
        assertEquals("HR", payload.get("fromDepartment").getAsString());
        assertEquals("L\u1ea7u 5 - TT16", payload.get("oldLocation").getAsString());
        assertEquals("L\u1ea7u 5 - TT16", payload.get("fromLocation").getAsString());
        assertEquals("Truong Vu", payload.get("newUser").getAsString());
        assertEquals("Truong Vu", payload.get("toUser").getAsString());
        assertEquals("IT", payload.get("newDepartment").getAsString());
        assertEquals("IT", payload.get("toDepartment").getAsString());
        assertEquals("L\u1ea7u 5 - TT16", payload.get("newLocation").getAsString());
        assertEquals("L\u1ea7u 5 - TT16", payload.get("toLocation").getAsString());
        assertEquals("2026-04-08", payload.get("handoverDate").getAsString());
        assertEquals("Checked Out", payload.get("status").getAsString());
        assertEquals("Checked Out", payload.get("action").getAsString());
        assertEquals("Checked Out", payload.get("checkoutStatus").getAsString());
        assertEquals("Checked Out", payload.get("transactionType").getAsString());
        assertEquals("Checked Out", payload.get("type").getAsString());
    }

    @Test
    public void hasRequiredFields_returnsFalseWhenAnyTargetFieldMissing() {
        Asset originalAsset = new Asset(
                12,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "HR",
                "Thang Nguyen",
                "Idoplex-5",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                12,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "IT",
                "",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        AssetHandoverRequestDto requestDto = AssetHandoverRequestDto.fromAssets(
                originalAsset,
                updatedAsset,
                "2026-04-08"
        );

        assertFalse(requestDto.hasRequiredFields());
    }

    @Test
    public void hasRequiredFields_returnsTrueWhenTidIsMissingButOthersPresent() {
        Asset originalAsset = new Asset(
                12,
                "AREXAT",
                "",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "HR",
                "Thang Nguyen",
                "Idoplex-5",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );
        Asset updatedAsset = new Asset(
                12,
                "AREXAT",
                "",
                "",
                "",
                "Laptop Dell",
                "LAPTOP",
                "SN-01",
                "IT",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "",
                "",
                "",
                "API"
        );

        AssetHandoverRequestDto requestDto = AssetHandoverRequestDto.fromAssets(
                originalAsset,
                updatedAsset,
                "2026-04-08"
        );

        assertTrue(requestDto.hasRequiredFields());
    }
}
