package com.idocean.asset.data.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AssetUpdateRequestDto {
    private static final String FIELD_TID = "tid";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_OLD_CODE = "oldCode";
    private static final String FIELD_OLD_SERIAL = "oldSerial";
    private static final String FIELD_ASSET_NAME = "assetName";
    private static final String FIELD_ASSET_TYPE = "ãysetType";
    private static final String FIELD_SERIAL_NUMBER = "serialNumber";
    private static final String FIELD_DEPARTMENT = "department";
    private static final String FIELD_ASSIGNED_USER = "assignedUser";
    private static final String FIELD_LOCATION = "location";
    private static final String FIELD_INVENTORY_STATUS = "inventoryStatus";
    private static final String FIELD_ASSET_CONDITION = "assetCondition";
    private static final String FIELD_TAG_DATE = "tagDate";
    private static final String FIELD_TAG_BY = "tagBy";
    private static final String FIELD_NOTE = "note";
    private static final String FIELD_SOURCE = "source";

    private final JsonObject payload;
    private final Asset originalAsset;
    private final Asset updatedAsset;
    private final List<String> changedFields;

    private AssetUpdateRequestDto(
            JsonObject payload,
            Asset originalAsset,
            Asset updatedAsset,
            List<String> changedFields
    ) {
        this.payload = payload == null ? new JsonObject() : payload;
        this.originalAsset = originalAsset;
        this.updatedAsset = updatedAsset;
        this.changedFields = changedFields == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(changedFields));
    }

    public static AssetUpdateRequestDto fromAssets(Asset originalAsset, Asset updatedAsset) {
        Asset safeOriginalAsset = originalAsset == null ? emptyAsset() : originalAsset;
        Asset safeUpdatedAsset = updatedAsset == null ? safeOriginalAsset : updatedAsset;

        JsonObject payload = new JsonObject();
        List<String> changedFields = new ArrayList<>();
        JsonArray updatedFields = new JsonArray();
        JsonArray clearedFields = new JsonArray();

        appendIdentifiers(payload, safeOriginalAsset, safeUpdatedAsset);
        appendWebhookCompatFields(payload, safeUpdatedAsset);

        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_TID,
                safeOriginalAsset.getTid(), safeUpdatedAsset.getTid(),
                "tid", "tag_id");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_CODE,
                safeOriginalAsset.getAssetCode(), safeUpdatedAsset.getAssetCode(),
                "code", "asset_code", "new_code", "updated_code");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_OLD_CODE,
                safeOriginalAsset.getOldCode(), safeUpdatedAsset.getOldCode(),
                "old_code", "code_old", "oldCode");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_OLD_SERIAL,
                safeOriginalAsset.getOldSerial(), safeUpdatedAsset.getOldSerial(),
                "old_serial", "serial_old", "oldSerial");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_ASSET_NAME,
                safeOriginalAsset.getAssetName(), safeUpdatedAsset.getAssetName(),
                "asset_name", "name", "assetName");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_ASSET_TYPE,
                safeOriginalAsset.getAssetType(), safeUpdatedAsset.getAssetType(),
                "asset_type", "type", "assetType");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_SERIAL_NUMBER,
                safeOriginalAsset.getSerialNumber(), safeUpdatedAsset.getSerialNumber(),
                "serial_number", "serial", "serialNumber");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_DEPARTMENT,
                AssetFieldNormalizer.normalizeDepartmentForDisplay(safeOriginalAsset.getDepartment()),
                safeUpdatedAsset.getDepartment(),
                "department", "department_name", "dept");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_ASSIGNED_USER,
                safeOriginalAsset.getAssignedUser(), safeUpdatedAsset.getAssignedUser(),
                "assignedUser", "user", "assigned_user", "username");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_LOCATION,
                AssetLocationUtils.normalizeLocationForDisplay(safeOriginalAsset.getLocation()),
                safeUpdatedAsset.getLocation(),
                "location", "location_name", "area");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_INVENTORY_STATUS,
                safeOriginalAsset.getInventoryStatus(), safeUpdatedAsset.getInventoryStatus(),
                "inventory_status", "status", "check_status");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_ASSET_CONDITION,
                safeOriginalAsset.getAssetCondition(), safeUpdatedAsset.getAssetCondition(),
                "condition", "asset_condition");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_TAG_DATE,
                safeOriginalAsset.getTagDate(), safeUpdatedAsset.getTagDate(),
                "tag_date", "tagged_at");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_TAG_BY,
                safeOriginalAsset.getTagBy(), safeUpdatedAsset.getTagBy(),
                "tag_by", "tagged_by", "user_name");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_NOTE,
                safeOriginalAsset.getNote(), safeUpdatedAsset.getNote(),
                "note", "notes", "remark");
        addChangedField(payload, changedFields, updatedFields, clearedFields, FIELD_SOURCE,
                safeOriginalAsset.getSource(), safeUpdatedAsset.getSource(),
                "source");

        if (updatedFields.size() > 0) {
            payload.add("updated_fields", updatedFields);
        }
        if (clearedFields.size() > 0) {
            payload.add("cleared_fields", clearedFields);
        }

        return new AssetUpdateRequestDto(payload, safeOriginalAsset, safeUpdatedAsset, changedFields);
    }

    public JsonObject getPayload() {
        return payload.deepCopy();
    }

    public Asset getOriginalAsset() {
        return originalAsset;
    }

    public Asset getUpdatedAsset() {
        return updatedAsset;
    }

    public boolean hasChanges() {
        return !changedFields.isEmpty();
    }

    public List<String> getChangedFields() {
        return new ArrayList<>(changedFields);
    }

    public boolean matchesIdentity(Asset asset) {
        if (asset == null) {
            return false;
        }

        Integer originalRowNumber = originalAsset == null ? null : originalAsset.getRowNumber();
        if (originalRowNumber != null && asset.getRowNumber() != null && originalRowNumber.equals(asset.getRowNumber())) {
            return true;
        }

        String originalCode = normalizeIdentity(originalAsset == null ? "" : originalAsset.getAssetCode());
        String updatedCode = normalizeIdentity(updatedAsset == null ? "" : updatedAsset.getAssetCode());
        String responseCode = normalizeIdentity(asset.getAssetCode());
        if (!originalCode.isEmpty() && originalCode.equals(responseCode)) {
            return true;
        }
        if (!updatedCode.isEmpty() && updatedCode.equals(responseCode)) {
            return true;
        }

        String originalTid = normalizeIdentity(originalAsset == null ? "" : originalAsset.getTid());
        String updatedTid = normalizeIdentity(updatedAsset == null ? "" : updatedAsset.getTid());
        String responseTid = normalizeIdentity(asset.getTid());
        return (!originalTid.isEmpty() && originalTid.equals(responseTid))
                || (!updatedTid.isEmpty() && updatedTid.equals(responseTid));
    }

    public boolean matchesReturnedAsset(Asset asset) {
        if (asset == null || !matchesIdentity(asset)) {
            return false;
        }
        for (String changedField : changedFields) {
            if (!normalizedFieldValue(updatedAsset, changedField).equals(normalizedFieldValue(asset, changedField))) {
                return false;
            }
        }
        return true;
    }

    private static void appendIdentifiers(JsonObject payload, Asset originalAsset, Asset updatedAsset) {
        Integer rowNumber = originalAsset != null && originalAsset.getRowNumber() != null
                ? originalAsset.getRowNumber()
                : updatedAsset == null ? null : updatedAsset.getRowNumber();
        if (rowNumber != null) {
            payload.addProperty("row_number", rowNumber);
            payload.addProperty("rowNumber", rowNumber);
            payload.addProperty("stt", rowNumber);
            payload.addProperty("id", rowNumber);
        }

        String originalAssetCode = safe(originalAsset == null ? "" : originalAsset.getAssetCode());
        if (!originalAssetCode.isEmpty()) {
            payload.addProperty("match_code", originalAssetCode);
            payload.addProperty("original_code", originalAssetCode);
        }

        String updatedAssetCode = safe(updatedAsset == null ? "" : updatedAsset.getAssetCode());
        if (!updatedAssetCode.isEmpty()) {
            payload.addProperty("code", updatedAssetCode);
            payload.addProperty("asset_code", updatedAssetCode);
        }

        String originalTid = safe(originalAsset == null ? "" : originalAsset.getTid());
        if (!originalTid.isEmpty()) {
            payload.addProperty("match_tid", originalTid);
            payload.addProperty("original_tid", originalTid);
        }

        String updatedTid = safe(updatedAsset == null ? "" : updatedAsset.getTid());
        if (!updatedTid.isEmpty()) {
            payload.addProperty("tid", updatedTid);
            payload.addProperty("tag_id", updatedTid);
        }
    }

    private static void appendWebhookCompatFields(JsonObject payload, Asset updatedAsset) {
        if (payload == null || updatedAsset == null) {
            return;
        }

        String webhookStatus = safe(updatedAsset.getInventoryStatus());
        if (webhookStatus.isEmpty()) {
            webhookStatus = safe(updatedAsset.getAssetCondition());
        }

        payload.addProperty("code", safe(updatedAsset.getAssetCode()));
        String tid = safe(updatedAsset.getTid());
        if (!tid.isEmpty()) {
            payload.addProperty("tid", tid);
        }
        payload.addProperty("oldCode", safe(updatedAsset.getOldCode()));
        payload.addProperty("oldSerial", safe(updatedAsset.getOldSerial()));
        payload.addProperty("assetName", safe(updatedAsset.getAssetName()));
        payload.addProperty("assetType", safe(updatedAsset.getAssetType()));
        payload.addProperty("serialNumber", safe(updatedAsset.getSerialNumber()));
        payload.addProperty("department", safe(updatedAsset.getDepartment()));
        payload.addProperty("assignedUser", safe(updatedAsset.getAssignedUser()));
        payload.addProperty("location", safe(updatedAsset.getLocation()));
        payload.addProperty("inventoryStatus", webhookStatus);
        // n8n hien dang doc "assetCondition" nhu truong tinh trang/usage status.
        payload.addProperty("assetCondition", webhookStatus);
        payload.addProperty("note", safe(updatedAsset.getNote()));
    }

    private static void addChangedField(
            JsonObject payload,
            List<String> changedFields,
            JsonArray updatedFields,
            JsonArray clearedFields,
            String fieldName,
            String originalValue,
            String updatedValue,
            String... payloadKeys
    ) {
        if (sameFieldValue(fieldName, originalValue, updatedValue)) {
            return;
        }

        String safeUpdatedValue = safe(updatedValue);
        changedFields.add(fieldName);
        updatedFields.add(fieldName);
        if (safeUpdatedValue.isEmpty()) {
            clearedFields.add(fieldName);
        }
        for (String payloadKey : payloadKeys) {
            payload.addProperty(payloadKey, safeUpdatedValue);
        }
    }

    private static boolean sameFieldValue(String fieldName, String left, String right) {
        return normalizedFieldValue(fieldName, left).equals(normalizedFieldValue(fieldName, right));
    }

    private static String normalizedFieldValue(Asset asset, String fieldName) {
        if (asset == null) {
            return normalizedFieldValue(fieldName, "");
        }
        switch (fieldName) {
            case FIELD_TID:
                return normalizedFieldValue(fieldName, asset.getTid());
            case FIELD_CODE:
                return normalizedFieldValue(fieldName, asset.getAssetCode());
            case FIELD_OLD_CODE:
                return normalizedFieldValue(fieldName, asset.getOldCode());
            case FIELD_OLD_SERIAL:
                return normalizedFieldValue(fieldName, asset.getOldSerial());
            case FIELD_ASSET_NAME:
                return normalizedFieldValue(fieldName, asset.getAssetName());
            case FIELD_ASSET_TYPE:
                return normalizedFieldValue(fieldName, asset.getAssetType());
            case FIELD_SERIAL_NUMBER:
                return normalizedFieldValue(fieldName, asset.getSerialNumber());
            case FIELD_DEPARTMENT:
                return normalizedFieldValue(fieldName, asset.getDepartment());
            case FIELD_ASSIGNED_USER:
                return normalizedFieldValue(fieldName, asset.getAssignedUser());
            case FIELD_LOCATION:
                return normalizedFieldValue(fieldName, asset.getLocation());
            case FIELD_INVENTORY_STATUS:
                return normalizedFieldValue(fieldName, asset.getInventoryStatus());
            case FIELD_ASSET_CONDITION:
                return normalizedFieldValue(fieldName, asset.getAssetCondition());
            case FIELD_TAG_DATE:
                return normalizedFieldValue(fieldName, asset.getTagDate());
            case FIELD_TAG_BY:
                return normalizedFieldValue(fieldName, asset.getTagBy());
            case FIELD_NOTE:
                return normalizedFieldValue(fieldName, asset.getNote());
            case FIELD_SOURCE:
                return normalizedFieldValue(fieldName, asset.getSource());
            default:
                return "";
        }
    }

    private static String normalizedFieldValue(String fieldName, String value) {
        String safeValue = safe(value);
        switch (fieldName) {
            case FIELD_DEPARTMENT:
                return safe(AssetFieldNormalizer.normalizeDepartmentForDisplay(safeValue)).toLowerCase(Locale.ROOT);
            case FIELD_LOCATION:
                return safe(AssetLocationUtils.normalizeLocationForDisplay(safeValue)).toLowerCase(Locale.ROOT);
            default:
                return safeValue;
        }
    }

    private static String normalizeIdentity(String value) {
        return safe(value).toUpperCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static Asset emptyAsset() {
        return new Asset(null, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "");
    }
}
