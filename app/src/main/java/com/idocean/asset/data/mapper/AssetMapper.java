package com.idocean.asset.data.mapper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.idocean.asset.data.dto.ApiAssetDto;
import com.idocean.asset.model.Asset;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AssetMapper {
    private static final String SOURCE_API = "API";
    private static final String SOURCE_CSV = "CSV";

    private static final String[] ROW_NUMBER_KEYS = {
            "stt", "id", "row_number", "row_no", "index", "indexid"
    };
    private static final String[] CODE_KEYS = {
            "code", "asset_code", "ma", "assetcode", "item_code", "so_seri", "ma_qr_code"
    };
    private static final String[] TID_KEYS = {
            "tid", "epc", "rfid", "rfid_tid", "tag_id", "tagid"
    };
    private static final String[] OLD_CODE_KEYS = {
            "old_code", "code_old", "ma_cu", "oldcode"
    };
    private static final String[] OLD_SERIAL_KEYS = {
            "old_serial", "serial_old", "serial_cu", "oldserial"
    };
    private static final String[] ASSET_NAME_KEYS = {
            "asset_name", "name", "assetname", "ten_tai_san", "asset", "ten_nhan_hieu_quy_cach_vat_tu_dung_cu"
    };
    private static final String[] ASSET_TYPE_KEYS = {
            "asset_type", "type", "category", "assettype", "loai_tai_san",
            "phan_loai_cap_2", "phan_loai_cap_1", "item_type", "nhom"
    };
    private static final String[] SERIAL_NUMBER_KEYS = {
            "serial_number", "serial", "serial_no", "serialnumber", "productnumber"
    };
    private static final String[] DEPARTMENT_KEYS = {
            "department", "dept", "department_name", "bo_phan", "phong_ban", "bo_phan_su_dung"
    };
    private static final String[] USER_KEYS = {
            "user", "assigned_user", "assigneduser", "owner", "employee", "nguoi_su_dung", "username"
    };
    private static final String[] LOCATION_KEYS = {
            "location", "location_name", "room", "area", "vi_tri", "vi_tri_dia_diem"
    };
    private static final String[] INVENTORY_STATUS_KEYS = {
            "inventory_status", "status", "check_status", "trang_thai_kiem_ke", "trang_thai_su_dung", "trang_thai", "usage_status", "inventorystatus"
    };
    private static final String[] CONDITION_KEYS = {
            "condition", "asset_condition", "tinh_trang"
    };
    private static final String[] TAG_DATE_KEYS = {
            "tag_date", "tagged_at", "created_at", "ngay_dan_tag", "ngay_kiem_ke", "created"
    };
    private static final String[] TAG_BY_KEYS = {
            "tag_by", "tagged_by", "created_by", "nguoi_dan_tag", "nguoi_tao", "user_name"
    };
    private static final String[] NOTE_KEYS = {
            "note", "notes", "remark", "description", "ghi_chu", "ghi_chu_kiem_ke"
    };

    private AssetMapper() {
    }

    public static Asset fromApi(ApiAssetDto dto) {
        if (dto == null) {
            return emptyAsset(null, SOURCE_API);
        }
        return new Asset(
                dto.stt,
                clean(dto.code),
                clean(dto.tid),
                clean(dto.oldCode),
                clean(dto.oldSerial),
                clean(dto.assetName),
                clean(dto.assetType),
                clean(dto.serialNumber),
                clean(dto.department),
                clean(dto.user),
                clean(dto.location),
                clean(dto.inventoryStatus),
                clean(dto.condition),
                clean(dto.tagDate),
                clean(dto.tagBy),
                clean(dto.note),
                SOURCE_API
        );
    }

    public static Asset fromApiJson(JsonObject object) {
        return fromApiJson(object, null);
    }

    public static Asset fromApiJson(JsonObject object, Integer fallbackRowNumber) {
        Map<String, JsonElement> values = normalizeJsonObject(object);
        return new Asset(
                readIntegerFromJson(values, fallbackRowNumber, ROW_NUMBER_KEYS),
                readStringFromJson(values, CODE_KEYS),
                readStringFromJson(values, TID_KEYS),
                readStringFromJson(values, OLD_CODE_KEYS),
                readStringFromJson(values, OLD_SERIAL_KEYS),
                readStringFromJson(values, ASSET_NAME_KEYS),
                readStringFromJson(values, ASSET_TYPE_KEYS),
                readStringFromJson(values, SERIAL_NUMBER_KEYS),
                readStringFromJson(values, DEPARTMENT_KEYS),
                readStringFromJson(values, USER_KEYS),
                readStringFromJson(values, LOCATION_KEYS),
                readStringFromJson(values, INVENTORY_STATUS_KEYS),
                readStringFromJson(values, CONDITION_KEYS),
                readStringFromJson(values, TAG_DATE_KEYS),
                readStringFromJson(values, TAG_BY_KEYS),
                readStringFromJson(values, NOTE_KEYS),
                SOURCE_API
        );
    }

    public static Asset fromCsv(Map<String, String> row, int fallbackRowNumber) {
        Map<String, String> values = normalizeStringMap(row);
        return new Asset(
                readIntegerFromStrings(values, fallbackRowNumber, ROW_NUMBER_KEYS),
                readStringFromStrings(values, CODE_KEYS),
                readStringFromStrings(values, TID_KEYS),
                readStringFromStrings(values, OLD_CODE_KEYS),
                readStringFromStrings(values, OLD_SERIAL_KEYS),
                readStringFromStrings(values, ASSET_NAME_KEYS),
                readStringFromStrings(values, ASSET_TYPE_KEYS),
                readStringFromStrings(values, SERIAL_NUMBER_KEYS),
                readStringFromStrings(values, DEPARTMENT_KEYS),
                readStringFromStrings(values, USER_KEYS),
                readStringFromStrings(values, LOCATION_KEYS),
                readStringFromStrings(values, INVENTORY_STATUS_KEYS),
                readStringFromStrings(values, CONDITION_KEYS),
                readStringFromStrings(values, TAG_DATE_KEYS),
                readStringFromStrings(values, TAG_BY_KEYS),
                readStringFromStrings(values, NOTE_KEYS),
                SOURCE_CSV
        );
    }

    public static boolean looksLikeAssetObject(JsonObject object) {
        Map<String, JsonElement> values = normalizeJsonObject(object);
        return hasAny(values, CODE_KEYS)
                || hasAny(values, TID_KEYS)
                || hasAny(values, ASSET_NAME_KEYS)
                || hasAny(values, SERIAL_NUMBER_KEYS);
    }

    public static boolean hasMeaningfulData(Asset asset) {
        return asset != null && (
                !asset.getAssetCode().isEmpty()
                        || !asset.getTid().isEmpty()
                        || !asset.getAssetName().isEmpty()
                        || !asset.getSerialNumber().isEmpty()
        );
    }

    public static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .replace("Ä‘", "d")
                .replace("Ã„â€˜", "d")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized == null ? "" : normalized;
    }

    private static Asset emptyAsset(Integer rowNumber, String source) {
        return new Asset(
                rowNumber,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                source
        );
    }

    private static Map<String, JsonElement> normalizeJsonObject(JsonObject object) {
        Map<String, JsonElement> normalized = new LinkedHashMap<>();
        if (object == null) {
            return normalized;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            normalized.put(normalizeHeader(entry.getKey()), entry.getValue());
        }
        return normalized;
    }

    private static Map<String, String> normalizeStringMap(Map<String, String> values) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (values == null) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            normalized.put(normalizeHeader(entry.getKey()), clean(entry.getValue()));
        }
        return normalized;
    }

    private static boolean hasAny(Map<String, JsonElement> values, String[] aliases) {
        for (String alias : aliases) {
            JsonElement element = values.get(normalizeHeader(alias));
            if (element != null && !element.isJsonNull()) {
                String candidate = readStringFromJson(values, new String[]{alias});
                if (!candidate.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String readStringFromJson(Map<String, JsonElement> values, String[] aliases) {
        for (String alias : aliases) {
            JsonElement element = values.get(normalizeHeader(alias));
            if (element == null || element.isJsonNull()) {
                continue;
            }
            try {
                String value = element.getAsString();
                String cleaned = clean(value);
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            } catch (RuntimeException ignored) {
                String cleaned = clean(element.toString());
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            }
        }
        return "";
    }

    private static String readStringFromStrings(Map<String, String> values, String[] aliases) {
        for (String alias : aliases) {
            String value = clean(values.get(normalizeHeader(alias)));
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static Integer readIntegerFromJson(Map<String, JsonElement> values, Integer fallbackValue, String[] aliases) {
        for (String alias : aliases) {
            JsonElement element = values.get(normalizeHeader(alias));
            if (element == null || element.isJsonNull()) {
                continue;
            }
            try {
                return element.getAsInt();
            } catch (RuntimeException ignored) {
                String cleaned = clean(element.toString());
                if (!cleaned.isEmpty()) {
                    try {
                        return Integer.parseInt(cleaned);
                    } catch (NumberFormatException ignoredToo) {
                    }
                }
            }
        }
        return fallbackValue;
    }

    private static Integer readIntegerFromStrings(Map<String, String> values, Integer fallbackValue, String[] aliases) {
        for (String alias : aliases) {
            String cleaned = clean(values.get(normalizeHeader(alias)));
            if (cleaned.isEmpty()) {
                continue;
            }
            try {
                return Integer.parseInt(cleaned);
            } catch (NumberFormatException ignored) {
            }
        }
        return fallbackValue;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        String normalized = cleaned.toLowerCase(Locale.ROOT);
        if (normalized.equals("#n/a")
                || normalized.equals("n/a")
                || normalized.equals("null")
                || normalized.equals("(null)")
                || normalized.equals("undefined")) {
            return "";
        }
        return cleaned;
    }
}
