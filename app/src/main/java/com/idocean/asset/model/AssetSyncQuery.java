package com.idocean.asset.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;

/**
 * Query dong bo asset gui len get-db theo query string.
 */
public class AssetSyncQuery {
    private final int batchSize;
    private final List<String> departments;
    private final List<String> locations;
    private final List<String> assetTypes;
    private final List<String> locationQueries;
    private final boolean localFilterOnly;

    public AssetSyncQuery(String department, String location, String assetType, int batchSize) {
        this(
                asSingleValueList(department),
                asSingleValueList(location),
                null,
                asSingleValueList(assetType),
                batchSize,
                false
        );
    }

    private AssetSyncQuery(
            List<String> departments,
            List<String> locations,
            List<String> locationQueries,
            List<String> assetTypes,
            int batchSize,
            boolean localFilterOnly
    ) {
        this.departments = sanitizeValues(departments);
        this.locations = sanitizeValues(locations);
        this.assetTypes = sanitizeValues(assetTypes);
        this.batchSize = batchSize;
        this.locationQueries = sanitizeLocationQueries(this.locations, locationQueries);
        this.localFilterOnly = localFilterOnly;
    }

    public static AssetSyncQuery withLocationQueries(
            String department,
            String locationDisplay,
            List<String> locationQueries,
            String assetType,
            int batchSize
    ) {
        return new AssetSyncQuery(
                asSingleValueList(department),
                asSingleValueList(locationDisplay),
                locationQueries,
                asSingleValueList(assetType),
                batchSize,
                false
        );
    }

    public static AssetSyncQuery localFilterOnly(
            String department,
            String locationDisplay,
            List<String> locationQueries,
            String assetType,
            int batchSize
    ) {
        return new AssetSyncQuery(
                asSingleValueList(department),
                asSingleValueList(locationDisplay),
                locationQueries,
                asSingleValueList(assetType),
                batchSize,
                true
        );
    }

    public static AssetSyncQuery localFilterOnly(
            List<String> departments,
            List<String> locations,
            List<String> locationQueries,
            List<String> assetTypes,
            int batchSize
    ) {
        return new AssetSyncQuery(departments, locations, locationQueries, assetTypes, batchSize, true);
    }

    public static AssetSyncQuery withFilters(
            List<String> departments,
            List<String> locations,
            List<String> locationQueries,
            List<String> assetTypes,
            int batchSize
    ) {
        return new AssetSyncQuery(departments, locations, locationQueries, assetTypes, batchSize, false);
    }

    public String getDepartment() {
        return departments.isEmpty() ? "" : departments.get(0);
    }

    public List<String> getDepartments() {
        return new ArrayList<>(departments);
    }

    public String getLocation() {
        return locations.isEmpty() ? "" : locations.get(0);
    }

    public List<String> getLocations() {
        return new ArrayList<>(locations);
    }

    public List<String> getLocationQueries() {
        return new ArrayList<>(locationQueries);
    }

    public List<String> getRequestLocationValues() {
        if (!locationQueries.isEmpty()) {
            return new ArrayList<>(locationQueries);
        }
        return new ArrayList<>(locations);
    }

    public String getAssetType() {
        return assetTypes.isEmpty() ? "" : assetTypes.get(0);
    }

    public List<String> getAssetTypes() {
        return new ArrayList<>(assetTypes);
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isLocalFilterOnly() {
        return localFilterOnly;
    }

    public boolean hasAnyFilter() {
        return !departments.isEmpty() || !locations.isEmpty() || !assetTypes.isEmpty();
    }

    public Map<String, String> toQueryMap(Integer limit, Integer offset) {
        return toQueryMap(getPrimaryLocationQuery(), limit, offset);
    }

    public Map<String, String> toQueryMap(String locationOverride, Integer limit, Integer offset) {
        LinkedHashMap<String, String> queryMap = new LinkedHashMap<>();
        String resolvedLocation = locationOverride == null ? getPrimaryLocationQuery() : safe(locationOverride);
        putIfNotBlank(queryMap, "department", getDepartment());
        putIfNotBlank(queryMap, "location", resolvedLocation);
        putIfNotBlank(queryMap, "assetType", getAssetType());
        if (limit != null && limit > 0) {
            queryMap.put("limit", String.valueOf(limit));
        }
        if (offset != null && offset >= 0) {
            queryMap.put("offset", String.valueOf(offset));
        }
        return queryMap;
    }

    public String describe() {
        if (!hasAnyFilter()) {
            return "toan bo du lieu";
        }
        StringBuilder builder = new StringBuilder();
        appendPart(builder, "phong ban", departments);
        appendPart(builder, "vi tri", locations);
        appendPart(builder, "loai", assetTypes);
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private void appendPart(StringBuilder builder, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" | ");
        }
        builder.append(label).append(": ").append(join(values));
    }

    private String getPrimaryLocationQuery() {
        return locationQueries.isEmpty() ? getLocation() : locationQueries.get(0);
    }

    private void putIfNotBlank(Map<String, String> queryMap, String key, String value) {
        if (!value.isEmpty()) {
            queryMap.put(key, value);
        }
    }

    private static List<String> sanitizeLocationQueries(List<String> locations, List<String> locationQueries) {
        Set<String> values = new LinkedHashSet<>();
        if (locationQueries != null) {
            for (String locationQuery : locationQueries) {
                String safeValue = safe(locationQuery);
                if (!safeValue.isEmpty()) {
                    values.add(safeValue);
                }
            }
        }
        if (values.isEmpty()) {
            List<String> safeLocations = sanitizeValues(locations);
            for (String safeLocation : safeLocations) {
                if (!safeLocation.isEmpty()) {
                    values.add(safeLocation);
                }
            }
        }
        return new ArrayList<>(values);
    }

    private static List<String> sanitizeValues(List<String> values) {
        Set<String> sanitized = new LinkedHashSet<>();
        if (values == null) {
            return new ArrayList<>();
        }
        for (String value : values) {
            String safeValue = safe(value);
            if (!safeValue.isEmpty()) {
                sanitized.add(safeValue);
            }
        }
        return new ArrayList<>(sanitized);
    }

    private static List<String> asSingleValueList(String value) {
        List<String> values = new ArrayList<>();
        String safeValue = safe(value);
        if (!safeValue.isEmpty()) {
            values.add(safeValue);
        }
        return values;
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
