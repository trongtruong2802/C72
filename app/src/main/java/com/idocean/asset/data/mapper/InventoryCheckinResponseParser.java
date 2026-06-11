package com.idocean.asset.data.mapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.idocean.asset.data.dto.InventoryCheckinInsertedRowDto;
import com.idocean.asset.data.dto.InventoryCheckinResponseDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser response tu webhook check-in.
 */
public final class InventoryCheckinResponseParser {
    public InventoryCheckinResponseDto parse(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            throw new JsonParseException("Backend checkin-assets khong tra ve du lieu.");
        }

        JsonElement root = JsonParser.parseString(rawBody);
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException("Backend checkin-assets tra ve dinh dang khong hop le.");
        }

        JsonObject object = root.getAsJsonObject();
        JsonObject summary = readObject(object, "summary");
        boolean success = readBoolean(object, "success");
        return new InventoryCheckinResponseDto(
                success,
                readString(object, "message"),
                firstString(object, "session_id", summary, "session_id"),
                firstInt(object, "total_received", summary, "total_received"),
                firstInt(object, "total_scanned_valid", summary, "total_scanned_valid"),
                firstInt(object, "total_skipped", summary, "total_skipped"),
                firstInt(object, "total_inserted", summary, "total_inserted"),
                readInsertedRows(firstElement(object, "inserted_rows", summary, "inserted_rows"))
        );
    }

    private List<InventoryCheckinInsertedRowDto> readInsertedRows(JsonElement element) {
        List<InventoryCheckinInsertedRowDto> rows = new ArrayList<>();
        if (element == null || element.isJsonNull() || !element.isJsonArray()) {
            return rows;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement item : array) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject object = item.getAsJsonObject();
            rows.add(new InventoryCheckinInsertedRowDto(
                    readString(object, "code"),
                    readString(object, "tid"),
                    readString(object, "checkin")
            ));
        }
        return rows;
    }

    private JsonObject readObject(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return null;
        }
        return element.getAsJsonObject();
    }

    private JsonElement firstElement(JsonObject primary, String primaryKey, JsonObject fallback, String fallbackKey) {
        JsonElement primaryElement = readElement(primary, primaryKey);
        if (primaryElement != null) {
            return primaryElement;
        }
        return readElement(fallback, fallbackKey);
    }

    private String firstString(JsonObject primary, String primaryKey, JsonObject fallback, String fallbackKey) {
        String primaryValue = readString(primary, primaryKey);
        if (!primaryValue.isEmpty()) {
            return primaryValue;
        }
        return readString(fallback, fallbackKey);
    }

    private int firstInt(JsonObject primary, String primaryKey, JsonObject fallback, String fallbackKey) {
        Integer primaryValue = readIntOrNull(primary, primaryKey);
        if (primaryValue != null) {
            return primaryValue;
        }
        Integer fallbackValue = readIntOrNull(fallback, fallbackKey);
        return fallbackValue == null ? 0 : fallbackValue;
    }

    private static JsonElement readElement(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element;
    }

    private static boolean readBoolean(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return false;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return false;
        }
        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int readInt(JsonObject object, String key) {
        Integer value = readIntOrNull(object, key);
        return value == null ? 0 : value;
    }

    private static Integer readIntOrNull(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return null;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = sanitizeString(element.getAsString());
                if (value.isEmpty()) {
                    return null;
                }
                return Math.max(0, Integer.parseInt(value));
            }
            return Math.max(0, element.getAsInt());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key)) {
            return "";
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        try {
            return sanitizeString(element.getAsString());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String sanitizeString(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = value.trim();
        if (sanitized.startsWith("={{") && sanitized.endsWith("}}")) {
            return "";
        }
        return sanitized;
    }
}
