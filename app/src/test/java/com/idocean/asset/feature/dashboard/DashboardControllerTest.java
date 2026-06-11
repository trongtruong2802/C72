package com.idocean.asset.feature.dashboard;

import com.idocean.asset.model.AssetSyncQuery;
import com.idocean.asset.model.SessionConfig;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DashboardControllerTest {
    @Test
    public void buildSelectionSummary_returnsEmptyLabelWhenNoSelection() {
        assertEquals(
                "Tat ca phong ban",
                DashboardController.buildSelectionSummary(Collections.emptyList(), "Tat ca phong ban")
        );
    }

    @Test
    public void buildSelectionSummary_collapsesMoreThanTwoValues() {
        assertEquals(
                "IT, HR +1",
                DashboardController.buildSelectionSummary(Arrays.asList("IT", "HR", "QA"), "Tat ca phong ban")
        );
    }

    @Test
    public void clearFilters_resetsAllSelections() {
        DashboardController controller = new DashboardController(null, null, null, null);
        controller.replaceSelectedDepartmentOptions(Arrays.asList(" IT ", "HR"));
        controller.replaceSelectedLocationOptions(Arrays.asList(" Warehouse "));
        controller.replaceSelectedAssetTypeOptions(Arrays.asList(" LAPTOP "));

        assertFalse(controller.getSelectedDepartmentOptions().isEmpty());
        assertFalse(controller.getSelectedLocationOptions().isEmpty());
        assertFalse(controller.getSelectedAssetTypeOptions().isEmpty());

        controller.clearFilters();

        assertTrue(controller.getSelectedDepartmentOptions().isEmpty());
        assertTrue(controller.getSelectedLocationOptions().isEmpty());
        assertTrue(controller.getSelectedAssetTypeOptions().isEmpty());
    }

    @Test
    public void initializeDashboard_setsIdleSyncState() {
        DashboardController controller = new DashboardController(null, null, null, null);

        controller.initializeDashboard();

        assertEquals(DashboardState.SyncUiState.IDLE, controller.getState().getSyncUiState());
        assertEquals(
                com.idocean.asset.data.repository.AssetSyncErrorType.NONE,
                controller.getState().getSyncErrorType()
        );
    }

    @Test
    public void createSyncPlan_filteredWithoutSelectionsReturnsError() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.FILTERED,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                300
        );

        assertFalse(plan.isValid());
        assertEquals(DashboardController.SyncError.FILTER_SELECTION_REQUIRED, plan.getError());
    }

    @Test
    public void createSyncPlan_sessionWithoutDepartmentReturnsError() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.SESSION,
                Collections.singletonList("IT"),
                Collections.emptyList(),
                Collections.emptyList(),
                new SessionConfig("Nguoi dung", "  ", "", false),
                300
        );

        assertFalse(plan.isValid());
        assertEquals(DashboardController.SyncError.SESSION_DEPARTMENT_REQUIRED, plan.getError());
    }

    @Test
    public void createSyncPlan_sessionBuildsFilteredQuery() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.SESSION,
                Collections.emptyList(),
                Collections.singletonList("Idoplex - 2"),
                Collections.singletonList("LAPTOP"),
                new SessionConfig("Nguoi dung", "it", "", false),
                300
        );

        assertTrue(plan.isValid());
        AssetSyncQuery query = plan.getQuery();
        assertFalse(query.isLocalFilterOnly());
        assertEquals(Collections.singletonList("IT"), query.getDepartments());
        assertTrue(query.getLocations().contains("Lầu 2 - TT16"));
        assertTrue(query.getLocationQueries().contains("TT16_F2"));
        assertEquals(Collections.singletonList("LAPTOP"), query.getAssetTypes());
    }

    @Test
    public void createSyncPlan_allBuildsFullQueryWithoutFilters() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.ALL,
                Collections.singletonList("IT"),
                Collections.singletonList("Lầu 2 - TT16"),
                Collections.singletonList("LAPTOP"),
                null,
                300
        );

        assertTrue(plan.isValid());
        AssetSyncQuery query = plan.getQuery();
        assertFalse(query.isLocalFilterOnly());
        assertTrue(query.getDepartments().isEmpty());
        assertTrue(query.getLocations().isEmpty());
        assertTrue(query.getAssetTypes().isEmpty());
    }

    @Test
    public void createSyncPlan_filteredKeepsMultipleLocationSelections() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.FILTERED,
                Collections.singletonList("IT"),
                Arrays.asList("Lầu 2 - TT16", "Lầu 5 - TT16"),
                Collections.singletonList("LAPTOP"),
                null,
                300
        );

        assertTrue(plan.isValid());
        AssetSyncQuery query = plan.getQuery();
        assertEquals(2, query.getLocations().size());
        assertTrue(query.getLocations().contains("Lầu 2 - TT16"));
        assertTrue(query.getLocations().contains("Lầu 5 - TT16"));
    }

    @Test
    public void createSyncPlan_filteredKeepsMultipleDepartmentSelections() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.FILTERED,
                Arrays.asList("IT", "HR"),
                Collections.singletonList("Lầu 2 - TT16"),
                Collections.singletonList("LAPTOP"),
                null,
                300
        );

        assertTrue(plan.isValid());
        AssetSyncQuery query = plan.getQuery();
        assertEquals(2, query.getDepartments().size());
        assertTrue(query.getDepartments().contains("IT"));
        assertTrue(query.getDepartments().contains("HR"));
    }

    @Test
    public void createSyncPlan_filteredKeepsMultipleAssetTypeSelections() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.FILTERED,
                Collections.singletonList("IT"),
                Collections.singletonList("Lầu 2 - TT16"),
                Arrays.asList("LAPTOP", "MONITOR"),
                null,
                300
        );

        assertTrue(plan.isValid());
        AssetSyncQuery query = plan.getQuery();
        assertEquals(2, query.getAssetTypes().size());
        assertTrue(query.getAssetTypes().contains("LAPTOP"));
        assertTrue(query.getAssetTypes().contains("MONITOR"));
    }
    @Test
    public void createSyncPlan_normalizesAssetTypeSelectionCase() {
        DashboardController.SyncPlan plan = DashboardController.createSyncPlan(
                DashboardController.SyncMode.FILTERED,
                Collections.singletonList("IT"),
                Collections.singletonList("Láº§u 2 - TT16"),
                Collections.singletonList("Chuột"),
                null,
                300
        );

        assertTrue(plan.isValid());
        AssetSyncQuery query = plan.getQuery();
        assertEquals(Collections.singletonList("CHUỘT"), query.getAssetTypes());
    }
}
