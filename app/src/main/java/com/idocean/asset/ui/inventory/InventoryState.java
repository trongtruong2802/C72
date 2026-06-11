package com.idocean.asset.ui.inventory;

import com.idocean.asset.model.InventorySessionItem;
import com.idocean.asset.model.SessionConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trang thai chay cua man kiem ke.
 */
public final class InventoryState {
    private final LinkedHashMap<String, InventorySessionItem> sourceItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, InventorySessionItem> outsideItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> tidIndex = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> codeIndex = new LinkedHashMap<>();

    private SessionConfig currentSession;
    private String currentDataSourceLabel = "";
    private String currentSearchQuery = "";

    public Map<String, InventorySessionItem> getSourceItems() {
        return sourceItems;
    }

    public Map<String, InventorySessionItem> getOutsideItems() {
        return outsideItems;
    }

    public Map<String, String> getTidIndex() {
        return tidIndex;
    }

    public Map<String, String> getCodeIndex() {
        return codeIndex;
    }

    public SessionConfig getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(SessionConfig currentSession) {
        this.currentSession = currentSession;
    }

    public String getCurrentDataSourceLabel() {
        return currentDataSourceLabel;
    }

    public void setCurrentDataSourceLabel(String currentDataSourceLabel) {
        this.currentDataSourceLabel = currentDataSourceLabel == null ? "" : currentDataSourceLabel;
    }

    public String getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    public void setCurrentSearchQuery(String currentSearchQuery) {
        this.currentSearchQuery = currentSearchQuery == null ? "" : currentSearchQuery;
    }

    public void replaceSourceData(Map<String, InventorySessionItem> newSourceItems,
                                  Map<String, String> newTidIndex,
                                  Map<String, String> newCodeIndex,
                                  String sourceLabel) {
        sourceItems.clear();
        if (newSourceItems != null) {
            sourceItems.putAll(newSourceItems);
        }
        outsideItems.clear();
        tidIndex.clear();
        if (newTidIndex != null) {
            tidIndex.putAll(newTidIndex);
        }
        codeIndex.clear();
        if (newCodeIndex != null) {
            codeIndex.putAll(newCodeIndex);
        }
        setCurrentDataSourceLabel(sourceLabel);
    }

    public void clearSessionResults() {
        outsideItems.clear();
        for (InventorySessionItem item : sourceItems.values()) {
            item.resetScan();
        }
    }

    public void resetAll() {
        sourceItems.clear();
        outsideItems.clear();
        tidIndex.clear();
        codeIndex.clear();
        currentSession = null;
        currentDataSourceLabel = "";
        currentSearchQuery = "";
    }
}
