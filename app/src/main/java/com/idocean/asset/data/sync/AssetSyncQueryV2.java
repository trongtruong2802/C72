package com.idocean.asset.data.sync;

import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Immutable input model cho Sync V2.
 * Luon giu danh sach filter da duoc trim/sanitize/de-duplicate.
 */
public final class AssetSyncQueryV2 {
    public enum Mode {
        FULL,
        FILTERED,
        SESSION
    }

    private final Mode mode;
    private final List<String> departments;
    private final List<String> locations;
    private final List<String> assetTypes;
    private final String requestLocationValue;

    private AssetSyncQueryV2(
            Mode mode,
            Collection<String> departments,
            Collection<String> locations,
            Collection<String> assetTypes,
            String requestLocationValue
    ) {
        this.mode = mode == null ? Mode.FULL : mode;
        this.departments = Collections.unmodifiableList(sanitizeDepartments(departments));
        this.locations = Collections.unmodifiableList(sanitizeLocations(locations));
        this.assetTypes = Collections.unmodifiableList(sanitizeAssetTypes(assetTypes));
        this.requestLocationValue = sanitizeLocationKey(requestLocationValue);
        validate();
    }

    public static AssetSyncQueryV2 fullSync() {
        return new AssetSyncQueryV2(
                Mode.FULL,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                ""
        );
    }

    public static AssetSyncQueryV2 filtered(
            Collection<String> departments,
            Collection<String> locations,
            Collection<String> assetTypes
    ) {
        return new AssetSyncQueryV2(Mode.FILTERED, departments, locations, assetTypes, "");
    }

    public static AssetSyncQueryV2 session(
            String sessionDepartment,
            Collection<String> locations,
            Collection<String> assetTypes
    ) {
        return new AssetSyncQueryV2(
                Mode.SESSION,
                singletonOrEmpty(sessionDepartment),
                locations,
                assetTypes,
                ""
        );
    }

    static AssetSyncQueryV2 singleCombination(
            Mode mode,
            String department,
            String location,
            String assetType
    ) {
        return new AssetSyncQueryV2(
                mode,
                singletonOrEmpty(department),
                singletonOrEmpty(location),
                singletonOrEmpty(assetType),
                ""
        );
    }

    public Mode getMode() {
        return mode;
    }

    public List<String> getDepartments() {
        return new ArrayList<>(departments);
    }

    public List<String> getLocations() {
        return new ArrayList<>(locations);
    }

    public List<String> getAssetTypes() {
        return new ArrayList<>(assetTypes);
    }

    public String getSingleDepartment() {
        return departments.isEmpty() ? "" : departments.get(0);
    }

    public String getSingleLocation() {
        return locations.isEmpty() ? "" : locations.get(0);
    }

    public String getRequestLocationValue() {
        if (!requestLocationValue.isEmpty()) {
            return requestLocationValue;
        }
        return getSingleLocation();
    }

    public String getSingleAssetType() {
        return assetTypes.isEmpty() ? "" : assetTypes.get(0);
    }

    public boolean hasAnyFilter() {
        return !departments.isEmpty() || !locations.isEmpty() || !assetTypes.isEmpty();
    }

    AssetSyncQueryV2 withRequestLocationValue(String locationValue) {
        return new AssetSyncQueryV2(
                mode,
                departments,
                locations,
                assetTypes,
                locationValue
        );
    }

    AssetSyncQueryV2 withoutLocationFilter() {
        if (departments.isEmpty() && assetTypes.isEmpty()) {
            return fullSync();
        }
        return new AssetSyncQueryV2(
                mode,
                departments,
                Collections.<String>emptyList(),
                assetTypes,
                ""
        );
    }

    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append("mode=").append(mode.name().toLowerCase(Locale.ROOT));
        appendDimension(builder, "departments", departments);
        appendDimension(builder, "locations", locations);
        appendDimension(builder, "assetTypes", assetTypes);
        return builder.toString();
    }

    private void validate() {
        if (mode == Mode.FILTERED && !hasAnyFilter()) {
            throw new IllegalArgumentException("Sync V2 filtered yeu cau it nhat mot gia tri filter.");
        }
        if (mode == Mode.SESSION && departments.isEmpty()) {
            throw new IllegalArgumentException("Sync V2 session yeu cau session department hop le.");
        }
    }

    private static void appendDimension(StringBuilder builder, String key, List<String> values) {
        if (builder == null || values == null || values.isEmpty()) {
            return;
        }
        builder.append(" | ").append(key).append('=').append(join(values));
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static List<String> sanitizeDepartments(Collection<String> values) {
        List<String> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (String value : safeCollection(values)) {
            String normalized = cleanRaw(AssetFieldNormalizer.normalizeDepartmentForDisplay(value));
            if (!normalized.isEmpty() && unique.add(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static List<String> sanitizeLocations(Collection<String> values) {
        List<String> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (String value : safeCollection(values)) {
            String normalized = sanitizeLocationKey(value);
            if (!normalized.isEmpty() && unique.add(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static List<String> sanitizeAssetTypes(Collection<String> values) {
        List<String> result = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (String value : safeCollection(values)) {
            String normalized = cleanRaw(AssetFieldNormalizer.normalizeAssetTypeForDisplay(value));
            if (!normalized.isEmpty() && unique.add(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }

    private static Collection<String> safeCollection(Collection<String> values) {
        return values == null ? Collections.<String>emptyList() : values;
    }

    private static List<String> singletonOrEmpty(String value) {
        List<String> values = new ArrayList<>();
        if (value != null) {
            values.add(value);
        }
        return values;
    }

    private static String cleanRaw(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        String normalized = cleaned.toLowerCase(Locale.ROOT);
        if ("#n/a".equals(normalized)
                || "n/a".equals(normalized)
                || "null".equals(normalized)
                || "(null)".equals(normalized)
                || "undefined".equals(normalized)) {
            return "";
        }
        return cleaned;
    }

    private static String sanitizeLocationKey(String value) {
        String normalizedKey = AssetLocationUtils.resolveLocationKey(value);
        if (!normalizedKey.isEmpty()) {
            return normalizedKey;
        }
        return cleanRaw(value);
    }
}
