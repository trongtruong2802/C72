package com.idocean.asset.data.dto;

import com.google.gson.JsonObject;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

public final class AssetHandoverRequestDto {
    private static final String CHECKED_OUT_STATUS = "Checked Out";
    private final JsonObject payload;

    private AssetHandoverRequestDto(JsonObject payload) {
        this.payload = payload == null ? new JsonObject() : payload;
    }

    public static AssetHandoverRequestDto fromAssets(Asset originalAsset, Asset updatedAsset, String handoverDate) {
        Asset sourceAsset = originalAsset == null ? updatedAsset : originalAsset;
        Asset targetAsset = updatedAsset == null ? sourceAsset : updatedAsset;

        JsonObject payload = new JsonObject();
        String code = safe(sourceAsset == null ? "" : sourceAsset.getAssetCode());
        String tid = safe(sourceAsset == null ? "" : sourceAsset.getTid());
        String assignedUser = safe(sourceAsset == null ? "" : sourceAsset.getAssignedUser());
        String oldDepartment = AssetFieldNormalizer.normalizeDepartmentForDisplay(
                sourceAsset == null ? "" : sourceAsset.getDepartment()
        );
        String oldLocation = AssetLocationUtils.normalizeLocationForDisplay(
                sourceAsset == null ? "" : sourceAsset.getLocation()
        );
        String newUser = safe(targetAsset == null ? "" : targetAsset.getAssignedUser());
        String newDepartment = AssetFieldNormalizer.normalizeDepartmentForDisplay(
                targetAsset == null ? "" : targetAsset.getDepartment()
        );
        String newLocation = AssetLocationUtils.normalizeLocationForDisplay(
                targetAsset == null ? "" : targetAsset.getLocation()
        );
        String normalizedDate = safe(handoverDate);

        payload.addProperty("code", code);
        payload.addProperty("tid", tid);
        payload.addProperty("assignedUser", assignedUser);
        payload.addProperty("fromUser", assignedUser);
        payload.addProperty("oldDepartment", safe(oldDepartment));
        payload.addProperty("fromDepartment", safe(oldDepartment));
        payload.addProperty("oldLocation", safe(oldLocation));
        payload.addProperty("fromLocation", safe(oldLocation));
        payload.addProperty("newUser", newUser);
        payload.addProperty("toUser", newUser);
        payload.addProperty("newDepartment", safe(newDepartment));
        payload.addProperty("toDepartment", safe(newDepartment));
        payload.addProperty("newLocation", safe(newLocation));
        payload.addProperty("toLocation", safe(newLocation));
        payload.addProperty("handoverDate", normalizedDate);
        payload.addProperty("status", CHECKED_OUT_STATUS);
        payload.addProperty("action", CHECKED_OUT_STATUS);
        payload.addProperty("checkoutStatus", CHECKED_OUT_STATUS);
        payload.addProperty("transactionType", CHECKED_OUT_STATUS);
        payload.addProperty("type", CHECKED_OUT_STATUS);

        return new AssetHandoverRequestDto(payload);
    }

    public JsonObject getPayload() {
        return payload.deepCopy();
    }

    public boolean hasRequiredFields() {
        return hasValue("code")
                && hasValue("tid")
                && hasValue("newUser")
                && hasValue("newDepartment")
                && hasValue("newLocation")
                && hasValue("handoverDate");
    }

    private boolean hasValue(String key) {
        return payload.has(key)
                && !payload.get(key).isJsonNull()
                && !safe(payload.get(key).getAsString()).isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
