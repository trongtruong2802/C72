package com.idocean.asset.ui.inventory;

import com.idocean.asset.data.repository.DashboardMetricsRepository;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.InventoryItemStatus;
import com.idocean.asset.model.InventoryScanSource;
import com.idocean.asset.model.InventorySessionItem;
import com.idocean.asset.model.SessionConfig;
import com.idocean.asset.scanner.rfid.UhfScanData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryControllerTest {
    private final DashboardMetricsRepository dashboardMetricsRepository = DashboardMetricsRepository.getInstance();
    private InventoryController controller;

    @Before
    public void setUp() {
        dashboardMetricsRepository.clear();
        controller = new InventoryController(dashboardMetricsRepository, LogRepository.getInstance());
        controller.setCurrentSession(new SessionConfig("Truong Vu", "IT", "Session note", false));
    }

    @After
    public void tearDown() {
        dashboardMetricsRepository.clear();
    }

    @Test
    public void applySourceAssets_keepsFirstIdentityAndBuildsIndices() {
        List<Asset> assets = new ArrayList<>();
        assets.add(asset(1, "CODE-A", "TID-A", "Asset A"));
        assets.add(asset(2, "CODE-B", "TID-A", "Asset B"));
        assets.add(asset(3, "CODE-A", "TID-C", "Asset C"));

        InventoryController.SourceLoadResult result = controller.applySourceAssets(assets, "API");

        assertEquals("API", result.getSourceLabel());
        assertEquals(3, result.getSourceCount());

        Map<String, String> tidIndex = controller.getState().getTidIndex();
        Map<String, String> codeIndex = controller.getState().getCodeIndex();
        assertEquals("ASSET:TID-A:0", tidIndex.get("TID-A"));
        assertEquals("ASSET:TID-C:2", tidIndex.get("TID-C"));
        assertEquals("ASSET:TID-A:0", codeIndex.get("CODE-A"));
        assertEquals("ASSET:TID-A:1", codeIndex.get("CODE-B"));
    }

    @Test
    public void handleQrScan_marksMatchedItemAndCountsDuplicateScans() {
        controller.applySourceAssets(singleAssetSource(), "API");

        InventoryController.ScanResult first = controller.handleQrScan("QR-1", 1000L, "");
        InventoryController.ScanResult second = controller.handleQrScan("QR-1", 2000L, "");

        assertTrue(first.isHandled());
        assertTrue(first.isMatchedSourceItem());
        assertFalse(first.isOutsideItem());
        assertTrue(second.isHandled());

        InventorySessionItem item = controller.getState().getSourceItems().values().iterator().next();
        assertEquals(InventoryItemStatus.CHECKED, item.getStatus());
        assertEquals(2, item.getScanCount());
        assertEquals(InventoryScanSource.QR, item.getScanSource());
        assertEquals("Truong Vu", item.getOperatorName());
        assertEquals("Session note", item.getInventoryNote());
    }

    @Test
    public void handleRfidScan_createsOutsideItemWhenTidNotFound() {
        controller.applySourceAssets(singleAssetSource(), "API");

        InventoryController.ScanResult result = controller.handleRfidScan(
                new UhfScanData("TID-OUT", "E2801190200089A73CC203CA", "CODE-OUT", "", 0, 1234L, null),
                ""
        );

        assertTrue(result.isHandled());
        assertFalse(result.isMatchedSourceItem());
        assertTrue(result.isOutsideItem());

        Map<String, InventorySessionItem> outsideItems = controller.getState().getOutsideItems();
        assertEquals(1, outsideItems.size());
        InventorySessionItem item = outsideItems.values().iterator().next();
        assertEquals(InventoryItemStatus.OUTSIDE, item.getStatus());
        assertEquals("CODE-OUT", item.getDisplayCode());
        assertEquals("TID-OUT", item.getDisplayTid());
        assertEquals("E2801190200089A73CC203CA", item.getDisplayEpcHex());
    }

    @Test
    public void clearSessionResults_resetsSourceItemsAndClearsOutsideItems() {
        controller.applySourceAssets(singleAssetSource(), "API");
        controller.handleQrScan("QR-1", 1000L, "");
        controller.handleRfidScan(
                new UhfScanData("TID-OUT", "E2801190200089A73CC203CA", "CODE-OUT", "", 0, 1234L, null),
                ""
        );

        controller.clearSessionResults();

        InventorySessionItem sourceItem = controller.getState().getSourceItems().values().iterator().next();
        assertEquals(InventoryItemStatus.MISSING, sourceItem.getStatus());
        assertEquals(0, sourceItem.getScanCount());
        assertEquals(0, controller.getState().getOutsideItems().size());
    }

    @Test
    public void buildSummary_updatesDashboardMetricsRepository() {
        controller.applySourceAssets(twoAssetSource(), "API");
        controller.handleQrScan("QR-1", 1000L, "");
        controller.handleRfidScan(
                new UhfScanData("TID-OUT", "E2801190200089A73CC203CA", "CODE-OUT", "", 0, 1234L, null),
                ""
        );

        InventoryController.InventorySummary summary = controller.buildSummary();

        assertEquals(2, summary.getScannedCount());
        assertEquals(2, summary.getDatasetCount());
        assertEquals(1, summary.getCheckedCount());
        assertEquals(1, summary.getMissingCount());
        assertEquals(1, summary.getOutsideCount());
        assertEquals(1, summary.getMatchedCount());

        assertTrue(dashboardMetricsRepository.hasInventorySummary());
        assertEquals(2, dashboardMetricsRepository.getExpectedCount());
        assertEquals(1, dashboardMetricsRepository.getCheckedCount());
        assertEquals(1, dashboardMetricsRepository.getMissingCount());
        assertEquals(1, dashboardMetricsRepository.getOutsideCount());
    }

    @Test
    public void buildOrderedItems_filtersByQuery() {
        List<Asset> assets = new ArrayList<>();
        assets.add(new Asset(1, "CODE-A", "TID-A", "", "", "Laptop A", "Laptop", "SER-1", "IT", "User A", "Lầu 5 - TT16", "Đang sử dụng", "", "", "", "", "api"));
        assets.add(new Asset(2, "CODE-B", "TID-B", "", "", "Monitor B", "Monitor", "SER-2", "HR", "User B", "Warehouse", "Đang sử dụng", "", "", "", "", "api"));
        controller.applySourceAssets(assets, "API");

        controller.setCurrentSearchQuery("warehouse");
        List<InventorySessionItem> filtered = controller.buildOrderedItems();

        assertEquals(1, filtered.size());
        assertEquals("CODE-B", filtered.get(0).getDisplayCode());
    }

    @Test
    public void buildExportItems_doesNotClearSearchQuery() {
        controller.applySourceAssets(twoAssetSource(), "API");
        controller.setCurrentSearchQuery("warehouse");

        List<InventorySessionItem> exportItems = controller.buildExportItems();

        assertEquals(2, exportItems.size());
        assertEquals("warehouse", controller.getCurrentSearchQuery());
    }

    private List<Asset> singleAssetSource() {
        List<Asset> assets = new ArrayList<>();
        assets.add(asset(1, "QR-1", "TID-1", "Asset A"));
        return assets;
    }

    private List<Asset> twoAssetSource() {
        List<Asset> assets = new ArrayList<>();
        assets.add(asset(1, "QR-1", "TID-1", "Asset A"));
        assets.add(asset(2, "QR-2", "TID-2", "Asset B"));
        return assets;
    }

    private Asset asset(int rowNumber, String assetCode, String tid, String assetName) {
        return new Asset(
                rowNumber,
                assetCode,
                tid,
                "",
                "",
                assetName,
                "Laptop",
                "SER-" + rowNumber,
                "IT",
                "User " + rowNumber,
                "Lầu 5 - TT16",
                "Đang sử dụng",
                "",
                "",
                "",
                "",
                "api"
        );
    }
}
