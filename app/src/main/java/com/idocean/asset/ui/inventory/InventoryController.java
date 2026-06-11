package com.idocean.asset.ui.inventory;

import com.idocean.asset.data.repository.DashboardMetricsRepository;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.InventoryItemStatus;
import com.idocean.asset.model.InventoryScanSource;
import com.idocean.asset.model.InventorySessionItem;
import com.idocean.asset.model.SessionConfig;
import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.scanner.rfid.UhfScanData;
import com.idocean.asset.utils.EpcUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InventoryController {
    public static final class SourceLoadResult {
        private final String sourceLabel;
        private final int sourceCount;

        SourceLoadResult(String sourceLabel, int sourceCount) {
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.sourceCount = Math.max(0, sourceCount);
        }

        public String getSourceLabel() {
            return sourceLabel;
        }

        public int getSourceCount() {
            return sourceCount;
        }
    }

    public static final class SourceSnapshot {
        private final LinkedHashMap<String, InventorySessionItem> sourceItems;
        private final LinkedHashMap<String, String> tidIndex;
        private final LinkedHashMap<String, String> codeIndex;
        private final String sourceLabel;
        private final int sourceCount;

        SourceSnapshot(LinkedHashMap<String, InventorySessionItem> sourceItems,
                       LinkedHashMap<String, String> tidIndex,
                       LinkedHashMap<String, String> codeIndex,
                       String sourceLabel) {
            this.sourceItems = sourceItems;
            this.tidIndex = tidIndex;
            this.codeIndex = codeIndex;
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.sourceCount = sourceItems == null ? 0 : sourceItems.size();
        }

        public LinkedHashMap<String, InventorySessionItem> getSourceItems() {
            return sourceItems;
        }

        public LinkedHashMap<String, String> getTidIndex() {
            return tidIndex;
        }

        public LinkedHashMap<String, String> getCodeIndex() {
            return codeIndex;
        }

        public String getSourceLabel() {
            return sourceLabel;
        }

        public int getSourceCount() {
            return sourceCount;
        }
    }

    public static final class ScanResult {
        private final InventoryScanSource scanSource;
        private final boolean handled;
        private final boolean matchedSourceItem;
        private final boolean outsideItem;
        private final String displayCode;
        private final String displayTid;
        private final String displayEpcHex;

        ScanResult(InventoryScanSource scanSource,
                   boolean handled,
                   boolean matchedSourceItem,
                   boolean outsideItem,
                   String displayCode,
                   String displayTid,
                   String displayEpcHex) {
            this.scanSource = scanSource == null ? InventoryScanSource.NONE : scanSource;
            this.handled = handled;
            this.matchedSourceItem = matchedSourceItem;
            this.outsideItem = outsideItem;
            this.displayCode = displayCode == null ? "" : displayCode;
            this.displayTid = displayTid == null ? "" : displayTid;
            this.displayEpcHex = displayEpcHex == null ? "" : displayEpcHex;
        }

        public static ScanResult empty() {
            return new ScanResult(InventoryScanSource.NONE, false, false, false, "", "", "");
        }

        public InventoryScanSource getScanSource() {
            return scanSource;
        }

        public boolean isHandled() {
            return handled;
        }

        public boolean isMatchedSourceItem() {
            return matchedSourceItem;
        }

        public boolean isOutsideItem() {
            return outsideItem;
        }

        public String getDisplayCode() {
            return displayCode;
        }

        public String getDisplayTid() {
            return displayTid;
        }

        public String getDisplayEpcHex() {
            return displayEpcHex;
        }
    }

    public static final class InventorySummary {
        private final int scannedCount;
        private final int datasetCount;
        private final int checkedCount;
        private final int missingCount;
        private final int outsideCount;
        private final int matchedCount;

        InventorySummary(int scannedCount, int datasetCount, int checkedCount, int missingCount, int outsideCount, int matchedCount) {
            this.scannedCount = Math.max(0, scannedCount);
            this.datasetCount = Math.max(0, datasetCount);
            this.checkedCount = Math.max(0, checkedCount);
            this.missingCount = Math.max(0, missingCount);
            this.outsideCount = Math.max(0, outsideCount);
            this.matchedCount = Math.max(0, matchedCount);
        }

        public int getScannedCount() {
            return scannedCount;
        }

        public int getDatasetCount() {
            return datasetCount;
        }

        public int getCheckedCount() {
            return checkedCount;
        }

        public int getMissingCount() {
            return missingCount;
        }

        public int getOutsideCount() {
            return outsideCount;
        }

        public int getMatchedCount() {
            return matchedCount;
        }
    }

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final LogRepository logRepository;
    private final InventoryState state = new InventoryState();

    public InventoryController(DashboardMetricsRepository dashboardMetricsRepository, LogRepository logRepository) {
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.logRepository = logRepository;
    }

    public InventoryState getState() {
        return state;
    }

    public void setCurrentSession(SessionConfig sessionConfig) {
        state.setCurrentSession(sessionConfig);
    }

    public SessionConfig getCurrentSession() {
        return state.getCurrentSession();
    }

    public void setCurrentSearchQuery(String searchQuery) {
        state.setCurrentSearchQuery(searchQuery);
    }

    public String getCurrentSearchQuery() {
        return state.getCurrentSearchQuery();
    }

    public String getCurrentDataSourceLabel() {
        return state.getCurrentDataSourceLabel();
    }

    public String getCurrentOperatorName() {
        SessionConfig currentSession = state.getCurrentSession();
        return currentSession == null || currentSession.getOperatorName() == null
                ? ""
                : currentSession.getOperatorName();
    }

    public String resolveInventoryNote(String screenNote) {
        String note = safe(screenNote).trim();
        if (!note.isEmpty()) {
            return note;
        }
        SessionConfig currentSession = state.getCurrentSession();
        return currentSession == null ? "" : safe(currentSession.getSessionNote());
    }

    public SourceSnapshot prepareSourceAssets(List<Asset> assets, String sourceLabel) {
        List<Asset> safeAssets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
        PreparedDataset preparedDataset = buildPreparedDataset(safeAssets, sourceLabel);
        return new SourceSnapshot(
                preparedDataset.sourceItems,
                preparedDataset.tidIndex,
                preparedDataset.codeIndex,
                preparedDataset.sourceLabel
        );
    }

    public SourceLoadResult applySourceAssets(SourceSnapshot snapshot) {
        if (snapshot == null) {
            state.replaceSourceData(new LinkedHashMap<String, InventorySessionItem>(), new LinkedHashMap<String, String>(), new LinkedHashMap<String, String>(), "");
            return new SourceLoadResult("", 0);
        }
        state.replaceSourceData(
                snapshot.getSourceItems(),
                snapshot.getTidIndex(),
                snapshot.getCodeIndex(),
                snapshot.getSourceLabel()
        );
        return new SourceLoadResult(snapshot.getSourceLabel(), snapshot.getSourceCount());
    }

    public SourceLoadResult applySourceAssets(List<Asset> assets, String sourceLabel) {
        return applySourceAssets(prepareSourceAssets(assets, sourceLabel));
    }

    public ScanResult handleRfidScan(UhfScanData scanData, String screenNote) {
        if (scanData == null) {
            return ScanResult.empty();
        }
        String rawTid = RfidTagNormalizer.sanitizeTid(scanData.getTid());
        String normalizedTid = normalize(rawTid);
        String epcHex = RfidTagNormalizer.normalizeHex(scanData.getEpcHex());
        String code = safe(scanData.getEpcAsciiCode());
        if (code.isEmpty()) {
            code = EpcUtils.hexToAscii(epcHex);
        }

        String itemKey = normalizedTid.isEmpty() ? null : state.getTidIndex().get(normalizedTid);
        boolean matchedByTid = itemKey != null;
        InventorySessionItem item;
        String operatorName = getCurrentOperatorName();
        String note = resolveInventoryNote(screenNote);

        if (matchedByTid) {
            item = state.getSourceItems().get(itemKey);
            if (item == null) {
                return ScanResult.empty();
            }
            item.markScanned(
                    InventoryScanSource.RFID,
                    operatorName,
                    note,
                    scanData.getScannedAt(),
                    code,
                    rawTid,
                    epcHex
            );
            logRepository.logInfo("SCAN_RFID", "Da quet RFID trung danh sach kiem ke", valueOrDash(rawTid));
        } else {
            String outsideKey = RfidTagNormalizer.buildOutsideRfidKey(rawTid, epcHex);
            item = state.getOutsideItems().get(outsideKey);
            if (item == null) {
                item = InventorySessionItem.createOutsideRfid(
                        outsideKey,
                        rawTid,
                        epcHex,
                        code,
                        scanData.getScannedAt(),
                        operatorName,
                        note
                );
                state.getOutsideItems().put(outsideKey, item);
            } else {
                item.markScanned(
                        InventoryScanSource.RFID,
                        operatorName,
                        note,
                        scanData.getScannedAt(),
                        code,
                        rawTid,
                        epcHex
                );
            }
            logRepository.logInfo("OUTSIDE_LIST", "RFID ngoai danh sach kiem ke", valueOrDash(rawTid));
        }

        return new ScanResult(
                InventoryScanSource.RFID,
                true,
                matchedByTid,
                !matchedByTid,
                code,
                rawTid,
                epcHex
        );
    }

    public ScanResult handleQrScan(String code, long timestamp, String screenNote) {
        String normalizedCode = normalize(code);
        String itemKey = normalizedCode.isEmpty() ? null : state.getCodeIndex().get(normalizedCode);
        boolean matchedByCode = itemKey != null;
        InventorySessionItem item;
        String operatorName = getCurrentOperatorName();
        String note = resolveInventoryNote(screenNote);

        if (matchedByCode) {
            item = state.getSourceItems().get(itemKey);
            if (item == null) {
                return ScanResult.empty();
            }
            item.markScanned(
                    InventoryScanSource.QR,
                    operatorName,
                    note,
                    timestamp,
                    safe(code),
                    item.getDisplayTid(),
                    ""
            );
            logRepository.logInfo("SCAN_QR", "Da quet QR trung danh sach kiem ke", valueOrDash(code));
        } else {
            String outsideKey = "OUTSIDE_QR:" + normalizedCode;
            item = state.getOutsideItems().get(outsideKey);
            if (item == null) {
                item = InventorySessionItem.createOutsideQr(
                        outsideKey,
                        safe(code),
                        timestamp,
                        operatorName,
                        note
                );
                state.getOutsideItems().put(outsideKey, item);
            } else {
                item.markScanned(
                        InventoryScanSource.QR,
                        operatorName,
                        note,
                        timestamp,
                        safe(code),
                        item.getDisplayTid(),
                        ""
                );
            }
            logRepository.logInfo("OUTSIDE_LIST", "QR ngoai danh sach kiem ke", valueOrDash(code));
        }

        return new ScanResult(
                InventoryScanSource.QR,
                true,
                matchedByCode,
                !matchedByCode,
                safe(code),
                "",
                ""
        );
    }

    public void clearSessionResults() {
        state.clearSessionResults();
        logRepository.logInfo("SESSION", "Da lam moi ket qua kiem ke hien tai");
    }

    public List<InventorySessionItem> buildOrderedItems() {
        return sortAndFilterItems(state.getCurrentSearchQuery());
    }

    public List<InventorySessionItem> buildExportItems() {
        return sortAndFilterItems("");
    }

    public List<InventorySessionItem> buildOrderedItems(String query) {
        return sortAndFilterItems(query);
    }

    private List<InventorySessionItem> sortAndFilterItems(String query) {
        List<InventorySessionItem> items = new ArrayList<>();
        items.addAll(state.getSourceItems().values());
        items.addAll(state.getOutsideItems().values());
        Collections.sort(items, new Comparator<InventorySessionItem>() {
            @Override
            public int compare(InventorySessionItem left, InventorySessionItem right) {
                int leftRank = statusRank(left.getStatus());
                int rightRank = statusRank(right.getStatus());
                if (leftRank != rightRank) {
                    return Integer.compare(leftRank, rightRank);
                }
                if (left.getScannedAt() != right.getScannedAt()) {
                    return Long.compare(right.getScannedAt(), left.getScannedAt());
                }
                return safe(left.getDisplayCode()).compareToIgnoreCase(safe(right.getDisplayCode()));
            }
        });

        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return items;
        }

        List<InventorySessionItem> filtered = new ArrayList<>();
        for (InventorySessionItem item : items) {
            if (item.matchesQuery(normalizedQuery)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public InventorySummary buildSummary() {
        int checked = 0;
        int missing = 0;
        int outside = state.getOutsideItems().size();
        int scanned = 0;
        int matched = 0;

        for (InventorySessionItem item : state.getSourceItems().values()) {
            if (item.getStatus() == InventoryItemStatus.CHECKED) {
                checked++;
                scanned++;
                matched++;
            } else if (item.getStatus() == InventoryItemStatus.MISSING) {
                missing++;
            }
        }

        for (InventorySessionItem item : state.getOutsideItems().values()) {
            if (item.getScanCount() > 0) {
                scanned++;
            }
        }

        dashboardMetricsRepository.updateInventorySummary(state.getSourceItems().size(), checked, missing, outside);
        return new InventorySummary(scanned, state.getSourceItems().size(), checked, missing, outside, matched);
    }

    public String resolveEmptyStateMessage() {
        if (state.getCurrentSearchQuery() != null && !state.getCurrentSearchQuery().trim().isEmpty()) {
            return "inventory_empty_state_search";
        }
        return "inventory_empty_placeholder";
    }

    public String getCurrentInventoryNote(String screenNote) {
        return resolveInventoryNote(screenNote);
    }

    public String getCurrentInventoryNote() {
        return resolveInventoryNote("");
    }

    private PreparedDataset buildPreparedDataset(List<Asset> assets, String sourceLabel) {
        LinkedHashMap<String, InventorySessionItem> preparedItems = new LinkedHashMap<>();
        LinkedHashMap<String, String> preparedTidIndex = new LinkedHashMap<>();
        LinkedHashMap<String, String> preparedCodeIndex = new LinkedHashMap<>();

        int index = 0;
        for (Asset asset : assets) {
            String itemKey = buildAssetKey(asset, index++);
            InventorySessionItem item = InventorySessionItem.fromAsset(itemKey, asset);
            preparedItems.put(itemKey, item);

            String normalizedTid = normalize(asset.getTid());
            if (!normalizedTid.isEmpty() && !preparedTidIndex.containsKey(normalizedTid)) {
                preparedTidIndex.put(normalizedTid, itemKey);
            }

            String normalizedCode = normalize(asset.getAssetCode());
            if (!normalizedCode.isEmpty() && !preparedCodeIndex.containsKey(normalizedCode)) {
                preparedCodeIndex.put(normalizedCode, itemKey);
            }
        }

        String resolvedSource = sourceLabel == null ? "" : sourceLabel;
        return new PreparedDataset(preparedItems, preparedTidIndex, preparedCodeIndex, resolvedSource);
    }

    private int statusRank(InventoryItemStatus status) {
        if (status == InventoryItemStatus.CHECKED) {
            return 0;
        }
        if (status == InventoryItemStatus.OUTSIDE) {
            return 1;
        }
        return 2;
    }

    private String buildAssetKey(Asset asset, int index) {
        String base = normalize(asset.getTid());
        if (base.isEmpty()) {
            base = normalize(asset.getAssetCode());
        }
        if (base.isEmpty()) {
            base = "ROW_" + index;
        }
        return "ASSET:" + base + ":" + index;
    }

    private String normalize(String value) {
        return safe(value).trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private static final class PreparedDataset {
        final LinkedHashMap<String, InventorySessionItem> sourceItems;
        final LinkedHashMap<String, String> tidIndex;
        final LinkedHashMap<String, String> codeIndex;
        final String sourceLabel;

        PreparedDataset(LinkedHashMap<String, InventorySessionItem> sourceItems,
                        LinkedHashMap<String, String> tidIndex,
                        LinkedHashMap<String, String> codeIndex,
                        String sourceLabel) {
            this.sourceItems = sourceItems;
            this.tidIndex = tidIndex;
            this.codeIndex = codeIndex;
            this.sourceLabel = sourceLabel;
        }
    }
}
