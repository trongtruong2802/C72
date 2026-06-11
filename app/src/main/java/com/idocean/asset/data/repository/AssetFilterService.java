package com.idocean.asset.data.repository;

import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetFilterCriteria;
import com.idocean.asset.model.AssetSyncQuery;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AssetFilterService {

    List<Asset> filterAssets(List<Asset> assets, AssetFilterCriteria criteria) {
        AssetFilterCriteria safeCriteria = criteria == null
                ? new AssetFilterCriteria("", "", "", "", "", "")
                : criteria;
        List<Asset> filtered = new ArrayList<>();
        if (assets == null) {
            return filtered;
        }
        for (Asset asset : assets) {
            if (matchesFilter(asset, safeCriteria)) {
                filtered.add(asset);
            }
        }
        return filtered;
    }

    List<Asset> filterAssetsBySyncQuery(List<Asset> assets, AssetSyncQuery query) {
        List<Asset> filteredAssets = new ArrayList<>();
        if (assets == null || assets.isEmpty() || query == null) {
            return filteredAssets;
        }
        for (Asset asset : assets) {
            if (matchesSyncQuery(asset, query)) {
                filteredAssets.add(asset);
            }
        }
        return filteredAssets;
    }

    List<Asset> filterAssetsBySyncQuery(List<Asset> assets, AssetSyncQueryV2 query) {
        List<Asset> filteredAssets = new ArrayList<>();
        if (assets == null || assets.isEmpty() || query == null) {
            return filteredAssets;
        }
        for (Asset asset : assets) {
            if (matchesSyncQuery(asset, query)) {
                filteredAssets.add(asset);
            }
        }
        return filteredAssets;
    }

    Map<String, List<String>> buildDistinctValueMap(List<Asset> assets) {
        LinkedHashMap<String, Set<String>> groupedValues = new LinkedHashMap<>();
        groupedValues.put("inventoryStatus", new LinkedHashSet<>());
        groupedValues.put("assetType", new LinkedHashSet<>());
        groupedValues.put("department", new LinkedHashSet<>());
        groupedValues.put("assignedUser", new LinkedHashSet<>());
        groupedValues.put("location", new LinkedHashSet<>());

        if (assets != null) {
            for (Asset asset : assets) {
                for (Map.Entry<String, Set<String>> entry : groupedValues.entrySet()) {
                    String value = valueForField(asset, entry.getKey());
                    if (!value.isEmpty()) {
                        entry.getValue().add(value);
                    }
                }
            }
        }

        LinkedHashMap<String, List<String>> distinctValues = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : groupedValues.entrySet()) {
            distinctValues.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return distinctValues;
    }

    List<String> collectDistinctValues(List<Asset> assets, String fieldName) {
        Set<String> values = new LinkedHashSet<>();
        if (assets != null) {
            for (Asset asset : assets) {
                String value = valueForField(asset, fieldName);
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return new ArrayList<>(values);
    }

    boolean matchesFilter(Asset asset, AssetFilterCriteria criteria) {
        if (asset == null) {
            return false;
        }
        if (!matchesOption(asset.getInventoryStatus(), criteria.getInventoryStatus())) {
            return false;
        }
        if (!matchesOption(asset.getAssetType(), criteria.getAssetType())) {
            return false;
        }
        if (!matchesDepartmentOption(asset.getDepartment(), criteria.getDepartment())) {
            return false;
        }
        if (!matchesOption(asset.getAssignedUser(), criteria.getAssignedUser())) {
            return false;
        }
        if (!matchesLocationOption(asset.getLocation(), criteria.getLocation())) {
            return false;
        }

        String query = normalizeSearch(criteria.getQuery());
        if (query.isEmpty()) {
            return true;
        }

        return contains(asset.getAssetCode(), query)
                || contains(asset.getTid(), query)
                || contains(asset.getAssetName(), query)
                || contains(asset.getAssignedUser(), query)
                || contains(asset.getDepartment(), query)
                || contains(asset.getSerialNumber(), query);
    }

    boolean matchesSyncQuery(Asset asset, AssetSyncQuery query) {
        if (asset == null || query == null) {
            return false;
        }
        if (!matchesDepartmentOptions(asset.getDepartment(), query.getDepartments())) {
            return false;
        }
        if (!matchesOptions(asset.getAssetType(), query.getAssetTypes())) {
            return false;
        }
        return matchesLocationOptions(asset.getLocation(), query.getLocations());
    }

    boolean matchesSyncQuery(Asset asset, AssetSyncQueryV2 query) {
        if (asset == null || query == null) {
            return false;
        }
        if (!matchesDepartmentOptions(asset.getDepartment(), query.getDepartments())) {
            return false;
        }
        if (!matchesOptions(asset.getAssetType(), query.getAssetTypes())) {
            return false;
        }
        return matchesLocationOptions(asset.getLocation(), query.getLocations());
    }

    String valueForField(Asset asset, String fieldName) {
        if (asset == null || fieldName == null) {
            return "";
        }
        String value;
        if ("inventoryStatus".equals(fieldName)) {
            value = asset.getInventoryStatus();
        } else if ("assetType".equals(fieldName)) {
            value = AssetFieldNormalizer.normalizeAssetTypeForFilter(asset.getAssetType());
        } else if ("department".equals(fieldName)) {
            value = asset.getDepartment();
        } else if ("assignedUser".equals(fieldName)) {
            value = asset.getAssignedUser();
        } else if ("location".equals(fieldName)) {
            value = asset.getLocation();
        } else {
            value = "";
        }
        return value == null ? "" : value.trim();
    }

    private boolean matchesOption(String value, String selectedOption) {
        String normalizedOption = normalizeSearch(selectedOption);
        if (normalizedOption.isEmpty()) {
            return true;
        }
        return normalizedOption.equals(normalizeSearch(value));
    }

    private boolean matchesDepartmentOption(String value, String selectedOption) {
        String normalizedOption = normalizeSearch(
                AssetFieldNormalizer.normalizeDepartmentForDisplay(selectedOption)
        );
        if (normalizedOption.isEmpty()) {
            return true;
        }
        return normalizedOption.equals(normalizeSearch(
                AssetFieldNormalizer.normalizeDepartmentForDisplay(value)
        ));
    }

    private boolean matchesLocationOption(String value, String selectedOption) {
        String selectedLocationKey = normalizeSearch(
                AssetLocationUtils.resolveLocationKey(selectedOption)
        );
        if (selectedLocationKey.isEmpty()) {
            return true;
        }
        return selectedLocationKey.equals(normalizeSearch(
                AssetLocationUtils.resolveLocationKey(value)
        ));
    }

    private boolean matchesOptions(String value, List<String> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return true;
        }
        for (String selectedOption : selectedOptions) {
            if (matchesOption(value, selectedOption)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesDepartmentOptions(String value, List<String> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return true;
        }
        for (String selectedOption : selectedOptions) {
            if (matchesDepartmentOption(value, selectedOption)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesLocationOptions(String value, List<String> selectedOptions) {
        if (selectedOptions == null || selectedOptions.isEmpty()) {
            return true;
        }
        for (String selectedOption : selectedOptions) {
            if (matchesLocationOption(value, selectedOption)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String normalizedQuery) {
        return normalizeSearch(value).contains(normalizedQuery);
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
