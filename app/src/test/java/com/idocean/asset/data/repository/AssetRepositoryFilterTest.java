package com.idocean.asset.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetFilterCriteria;
import com.idocean.asset.model.AssetSyncQuery;
import com.idocean.asset.utils.AssetLocationUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AssetRepositoryFilterTest {
    private final AssetFilterService filterService = new AssetFilterService();

    @Test
    public void matchesFilter_matchesNormalizedDepartmentAndLocationValues() {
        Asset asset = asset(
                "TS-001",
                "E280001",
                "Finance & Accountant",
                "Alice Nguyen",
                "Idoplex-5",
                "LAPTOP",
                "Dang su dung",
                "Dell Latitude",
                "SN-001"
        );

        AssetFilterCriteria criteria = new AssetFilterCriteria(
                "",
                "",
                "",
                "Finance and Accountant",
                "",
                "L\u1ea7u 5 - TT16"
        );

        assertTrue(filterService.matchesFilter(asset, criteria));
    }

    @Test
    public void matchesFilter_matchesQueryCaseInsensitivelyAcrossSupportedFields() {
        Asset asset = asset(
                "TS-001",
                "E280ABC",
                "IT",
                "Truong Vu",
                "Warehouse",
                "LAPTOP",
                "Dang su dung",
                "Laptop Dell",
                "SN-LOOKUP-01"
        );

        AssetFilterCriteria criteria = new AssetFilterCriteria(
                "sn-lookup-01",
                "",
                "",
                "",
                "",
                ""
        );

        assertTrue(filterService.matchesFilter(asset, criteria));
    }

    @Test
    public void matchesSyncQuery_acceptsCanonicalLocationAgainstLegacyStoredValue() {
        Asset asset = asset(
                "TS-002",
                "E280XYZ",
                "IT",
                "Thang Nguyen",
                "Idoplex-5",
                "LAPTOP",
                "Dang su dung",
                "HP EliteBook",
                "SN-002"
        );
        AssetSyncQuery query = AssetSyncQuery.localFilterOnly(
                Arrays.asList("IT"),
                Arrays.asList("L\u1ea7u 5 - TT16"),
                AssetLocationUtils.resolveLocationQueryAliases("L\u1ea7u 5 - TT16"),
                Arrays.asList("LAPTOP"),
                300
        );

        assertTrue(filterService.matchesSyncQuery(asset, query));
    }

    @Test
    public void matchesSyncQuery_supportsMultipleSelectionsAcrossAllGroups() {
        List<Asset> assets = Arrays.asList(
                asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "LAPTOP", "Dang su dung", "Dell", "SN-001"),
                asset("TS-002", "E280002", "HR", "Bob", "L\u1ea7u 2 - TT16", "MONITOR", "Dang su dung", "HP", "SN-002"),
                asset("TS-003", "E280003", "QA", "Charlie", "Warehouse", "MONITOR", "Dang su dung", "Lenovo", "SN-003")
        );

        AssetSyncQuery query = AssetSyncQuery.localFilterOnly(
                Arrays.asList("IT", "HR"),
                Arrays.asList("Warehouse", "L\u1ea7u 2 - TT16"),
                AssetLocationUtils.resolveLocationQueryAliases("L\u1ea7u 2 - TT16"),
                Arrays.asList("LAPTOP", "MONITOR"),
                300
        );

        List<Asset> filtered = filterService.filterAssetsBySyncQuery(assets, query);

        assertEquals(2, filtered.size());
        assertEquals("TS-001", filtered.get(0).getAssetCode());
        assertEquals("TS-002", filtered.get(1).getAssetCode());
        assertFalse(filtered.stream().anyMatch(asset -> "TS-003".equals(asset.getAssetCode())));
    }

    @Test
    public void matchesSyncQuery_supportsMultipleLocationsInSameGroup() {
        List<Asset> assets = Arrays.asList(
                asset("TS-010", "E280010", "IT", "Alice", "L\u1ea7u 5 - TT16", "LAPTOP", "Dang su dung", "Dell", "SN-010"),
                asset("TS-011", "E280011", "IT", "Bob", "L\u1ea7u 6 - TT16", "LAPTOP", "Dang su dung", "HP", "SN-011"),
                asset("TS-012", "E280012", "IT", "Charlie", "Warehouse", "LAPTOP", "Dang su dung", "Lenovo", "SN-012")
        );

        List<String> locationQueries = new java.util.ArrayList<>();
        locationQueries.addAll(AssetLocationUtils.resolveLocationQueryAliases("L\u1ea7u 5 - TT16"));
        locationQueries.addAll(AssetLocationUtils.resolveLocationQueryAliases("L\u1ea7u 6 - TT16"));

        AssetSyncQuery query = AssetSyncQuery.localFilterOnly(
                Arrays.asList("IT"),
                Arrays.asList("L\u1ea7u 5 - TT16", "L\u1ea7u 6 - TT16"),
                locationQueries,
                Arrays.asList("LAPTOP"),
                300
        );

        List<Asset> filtered = filterService.filterAssetsBySyncQuery(assets, query);

        assertEquals(2, filtered.size());
        assertEquals("TS-010", filtered.get(0).getAssetCode());
        assertEquals("TS-011", filtered.get(1).getAssetCode());
    }

    @Test
    public void collectDistinctValues_keepsEncounterOrderAndSkipsBlankValues() {
        List<Asset> assets = Arrays.asList(
                asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "LAPTOP", "Dang su dung", "Dell", "SN-001"),
                asset("TS-002", "E280002", "IT", "Bob", "Warehouse", "MONITOR", "Dang su dung", "LG", "SN-002"),
                asset("TS-003", "E280003", "HR", "Alice", "L\u1ea7u 5 - TT16", "LAPTOP", "Cho phan bo", "HP", "SN-003"),
                asset("TS-004", "E280004", " ", " ", " ", "", "", "Blank", "SN-004")
        );

        List<String> departments = filterService.collectDistinctValues(assets, "department");
        List<String> assignedUsers = filterService.collectDistinctValues(assets, "assignedUser");

        assertEquals(Arrays.asList("IT", "HR"), departments);
        assertEquals(Arrays.asList("Alice", "Bob"), assignedUsers);
    }

    @Test
    public void collectDistinctValues_normalizesAssetTypeCaseVariants() {
        List<Asset> assets = Arrays.asList(
                asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "CHUỘT", "Dang su dung", "Dell", "SN-001"),
                asset("TS-002", "E280002", "IT", "Bob", "Warehouse", "Chuột", "Dang su dung", "LG", "SN-002"),
                asset("TS-003", "E280003", "IT", "Charlie", "Warehouse", " Laptop ", "Dang su dung", "HP", "SN-003")
        );

        List<String> assetTypes = filterService.collectDistinctValues(assets, "assetType");

        assertEquals(Arrays.asList("CHUỘT", "LAPTOP"), assetTypes);
    }

    @Test
    public void buildDistinctValueMap_includesSupportedFieldsOnly() {
        List<Asset> assets = Arrays.asList(
                asset("TS-001", "E280001", "IT", "Alice", "Warehouse", "LAPTOP", "Dang su dung", "Dell", "SN-001"),
                asset("TS-002", "E280002", "HR", "Bob", "L\u1ea7u 5 - TT16", "MONITOR", "Cho phan bo", "LG", "SN-002")
        );

        Map<String, List<String>> distinctValueMap = filterService.buildDistinctValueMap(assets);

        assertEquals(Arrays.asList("Dang su dung", "Cho phan bo"), distinctValueMap.get("inventoryStatus"));
        assertEquals(Arrays.asList("LAPTOP", "MONITOR"), distinctValueMap.get("assetType"));
        assertEquals(Arrays.asList("IT", "HR"), distinctValueMap.get("department"));
        assertEquals(Arrays.asList("Alice", "Bob"), distinctValueMap.get("assignedUser"));
        assertEquals(Arrays.asList("Warehouse", "L\u1ea7u 5 - TT16"), distinctValueMap.get("location"));
    }

    private static Asset asset(
            String code,
            String tid,
            String department,
            String assignedUser,
            String location,
            String assetType,
            String inventoryStatus,
            String assetName,
            String serial
    ) {
        return new Asset(
                1,
                code,
                tid,
                "",
                "",
                assetName,
                assetType,
                serial,
                department,
                assignedUser,
                location,
                inventoryStatus,
                "",
                "",
                "",
                "",
                "API"
        );
    }
}
