package com.idocean.asset.data.mapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.idocean.asset.model.Asset;

import java.util.ArrayList;
import java.util.List;

public final class AssetApiResponseParser {
    private static final int MAX_DEPTH = 8;
    private static final String[] ARRAY_CONTAINER_KEYS = {
            "data", "assets", "rows", "items", "result", "results", "records", "json"
    };
    private static final String[] TOTAL_KEYS = {
            "total", "totalCount", "total_count", "count", "recordCount", "record_count"
    };

    private AssetApiResponseParser() {
    }

    public static List<Asset> parseAssets(String rawBody) {
        return mapAssets(parseAssetArray(rawBody));
    }

    public static JsonArray parseAssetArray(String rawBody) {
        return parsePageResult(rawBody).assetArray;
    }

    public static AssetPageResult parsePageResult(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            throw new JsonParseException("May chu khong tra ve du lieu tai san.");
        }

        JsonElement root = JsonParser.parseString(rawBody);
        String errorMessage = findErrorMessage(root, 0);
        if (!errorMessage.isEmpty()) {
            throw new JsonParseException(errorMessage);
        }

        JsonArray assetArray = findAssetArray(root, 0);
        if (assetArray == null) {
            throw new JsonParseException("Khong tim thay danh sach tai san trong phan hoi get-db.");
        }
        Integer explicitTotalCount = findTotalCount(root, 0);
        boolean hasExplicitTotalCount = explicitTotalCount != null && explicitTotalCount >= 0;
        int totalCount = hasExplicitTotalCount ? explicitTotalCount : assetArray.size();
        return new AssetPageResult(assetArray, totalCount, hasExplicitTotalCount);
    }

    public static List<Asset> mapAssets(JsonArray assetArray) {
        List<Asset> assets = mapAssetsAllowEmpty(assetArray);
        if (assets.isEmpty()) {
            throw new JsonParseException("Phan hoi get-db khong co ban ghi tai san hop le.");
        }
        return assets;
    }

    public static List<Asset> mapAssetsAllowEmpty(JsonArray assetArray) {
        List<Asset> assets = new ArrayList<>();
        int fallbackRowNumber = 1;
        for (JsonElement item : assetArray) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }

            JsonObject object = item.getAsJsonObject();
            Asset asset = AssetMapper.fromApiJson(object, fallbackRowNumber);
            if (!AssetMapper.hasMeaningfulData(asset)) {
                continue;
            }
            assets.add(asset);
            fallbackRowNumber++;
        }
        return assets;
    }

    private static Integer findTotalCount(JsonElement element, int depth) {
        if (element == null || element.isJsonNull() || depth > MAX_DEPTH) {
            return null;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (String key : TOTAL_KEYS) {
                Integer value = readInt(object, key);
                if (value != null && value >= 0) {
                    return value;
                }
            }
            for (String key : ARRAY_CONTAINER_KEYS) {
                Integer nested = findTotalCount(findByNormalizedKey(object, key), depth + 1);
                if (nested != null && nested >= 0) {
                    return nested;
                }
            }
            for (String key : object.keySet()) {
                Integer nested = findTotalCount(object.get(key), depth + 1);
                if (nested != null && nested >= 0) {
                    return nested;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                Integer nested = findTotalCount(child, depth + 1);
                if (nested != null && nested >= 0) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static JsonArray findAssetArray(JsonElement element, int depth) {
        if (element == null || element.isJsonNull() || depth > MAX_DEPTH) {
            return null;
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (looksLikeAssetArray(array)) {
                return array;
            }
            for (JsonElement child : array) {
                JsonArray nested = findAssetArray(child, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
            return null;
        }

        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        for (String preferredKey : ARRAY_CONTAINER_KEYS) {
            JsonElement nested = findByNormalizedKey(object, preferredKey);
            JsonArray nestedArray = findAssetArray(nested, depth + 1);
            if (nestedArray != null) {
                return nestedArray;
            }
        }

        for (String key : object.keySet()) {
            JsonArray nestedArray = findAssetArray(object.get(key), depth + 1);
            if (nestedArray != null) {
                return nestedArray;
            }
        }
        return null;
    }

    private static boolean looksLikeAssetArray(JsonArray array) {
        if (array == null) {
            return false;
        }
        if (array.size() == 0) {
            return true;
        }
        for (JsonElement element : array) {
            if (element != null && element.isJsonObject()) {
                return AssetMapper.looksLikeAssetObject(element.getAsJsonObject());
            }
        }
        return false;
    }

    private static String findErrorMessage(JsonElement element, int depth) {
        if (element == null || element.isJsonNull() || depth > MAX_DEPTH) {
            return "";
        }

        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String nested = findErrorMessage(child, depth + 1);
                if (!nested.isEmpty()) {
                    return nested;
                }
            }
            return "";
        }

        if (!element.isJsonObject()) {
            return "";
        }

        JsonObject object = element.getAsJsonObject();
        Boolean success = readBoolean(object, "success");
        String message = readString(object, "message", "msg", "detail");
        String error = readString(object, "error", "errors");

        if (success != null && !success) {
            if (!message.isEmpty()) {
                return message;
            }
            if (!error.isEmpty()) {
                return error;
            }
            return "Backend get-db tra ve trang thai khong thanh cong.";
        }

        if (!error.isEmpty() && !AssetMapper.looksLikeAssetObject(object)) {
            return message.isEmpty() ? error : message + ": " + error;
        }

        for (String preferredKey : ARRAY_CONTAINER_KEYS) {
            String nested = findErrorMessage(findByNormalizedKey(object, preferredKey), depth + 1);
            if (!nested.isEmpty()) {
                return nested;
            }
        }

        return "";
    }

    private static JsonElement findByNormalizedKey(JsonObject object, String targetKey) {
        if (object == null) {
            return null;
        }
        String normalizedTarget = AssetMapper.normalizeHeader(targetKey);
        for (String key : object.keySet()) {
            if (AssetMapper.normalizeHeader(key).equals(normalizedTarget)) {
                return object.get(key);
            }
        }
        return null;
    }

    private static String readString(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = findByNormalizedKey(object, key);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            try {
                String value = element.getAsString();
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            } catch (RuntimeException ignored) {
            }
        }
        return "";
    }

    private static Boolean readBoolean(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = findByNormalizedKey(object, key);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            try {
                return element.getAsBoolean();
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Integer readInt(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = findByNormalizedKey(object, key);
            if (element == null || element.isJsonNull()) {
                continue;
            }
            try {
                return element.getAsInt();
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    public static final class AssetPageResult {
        public final JsonArray assetArray;
        public final int totalCount;
        public final boolean hasExplicitTotalCount;

        public AssetPageResult(JsonArray assetArray, int totalCount, boolean hasExplicitTotalCount) {
            this.assetArray = assetArray;
            this.totalCount = totalCount;
            this.hasExplicitTotalCount = hasExplicitTotalCount;
        }
    }
}
