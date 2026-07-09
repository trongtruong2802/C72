package com.idocean.asset.ui.dashboard;

import com.idocean.asset.data.sync.AssetSyncErrorType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Trang thai runtime cua dashboard.
 */
public final class DashboardState {
    public enum SyncUiState {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private SyncUiState syncUiState = SyncUiState.IDLE;
    private AssetSyncErrorType syncErrorType = AssetSyncErrorType.NONE;
    private boolean syncing;
    private int previewTotalCount;
    private int previewLoadedCount;
    private int previewBatchSize;
    private int progressBatchIndex;
    private int progressTotalBatches;
    private boolean progressIndeterminate = true;
    private int cachedAssetCount;
    private String cachedAssetSource = "CACHE";
    private String statusText = "";
    private String previewText = "";
    private String progressText = "";
    private String activeSyncDescription = "";
    private long lastSyncAt;

    private final List<String> departmentOptions = new ArrayList<>();
    private final List<String> locationOptions = new ArrayList<>();
    private final List<String> assetTypeOptions = new ArrayList<>();
    private final LinkedHashSet<String> selectedDepartmentOptions = new LinkedHashSet<>();
    private final LinkedHashSet<String> selectedLocationOptions = new LinkedHashSet<>();
    private final LinkedHashSet<String> selectedAssetTypeOptions = new LinkedHashSet<>();

    public boolean isSyncing() {
        return syncing;
    }

    public void setSyncing(boolean syncing) {
        this.syncing = syncing;
    }

    public SyncUiState getSyncUiState() {
        return syncUiState;
    }

    public void setSyncUiState(SyncUiState syncUiState) {
        this.syncUiState = syncUiState == null ? SyncUiState.IDLE : syncUiState;
    }

    public AssetSyncErrorType getSyncErrorType() {
        return syncErrorType;
    }

    public void setSyncErrorType(AssetSyncErrorType syncErrorType) {
        this.syncErrorType = syncErrorType == null ? AssetSyncErrorType.NONE : syncErrorType;
    }

    public int getPreviewTotalCount() {
        return previewTotalCount;
    }

    public void setPreviewTotalCount(int previewTotalCount) {
        this.previewTotalCount = previewTotalCount;
    }

    public int getPreviewLoadedCount() {
        return previewLoadedCount;
    }

    public void setPreviewLoadedCount(int previewLoadedCount) {
        this.previewLoadedCount = previewLoadedCount;
    }

    public int getPreviewBatchSize() {
        return previewBatchSize;
    }

    public void setPreviewBatchSize(int previewBatchSize) {
        this.previewBatchSize = previewBatchSize;
    }

    public int getProgressBatchIndex() {
        return progressBatchIndex;
    }

    public void setProgressBatchIndex(int progressBatchIndex) {
        this.progressBatchIndex = progressBatchIndex;
    }

    public int getProgressTotalBatches() {
        return progressTotalBatches;
    }

    public void setProgressTotalBatches(int progressTotalBatches) {
        this.progressTotalBatches = progressTotalBatches;
    }

    public boolean isProgressIndeterminate() {
        return progressIndeterminate;
    }

    public void setProgressIndeterminate(boolean progressIndeterminate) {
        this.progressIndeterminate = progressIndeterminate;
    }

    public int getCachedAssetCount() {
        return cachedAssetCount;
    }

    public void setCachedAssetCount(int cachedAssetCount) {
        this.cachedAssetCount = cachedAssetCount;
    }

    public String getCachedAssetSource() {
        return cachedAssetSource;
    }

    public void setCachedAssetSource(String cachedAssetSource) {
        this.cachedAssetSource = cachedAssetSource == null ? "CACHE" : cachedAssetSource;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText == null ? "" : statusText;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText == null ? "" : previewText;
    }

    public String getProgressText() {
        return progressText;
    }

    public void setProgressText(String progressText) {
        this.progressText = progressText == null ? "" : progressText;
    }

    public String getActiveSyncDescription() {
        return activeSyncDescription;
    }

    public void setActiveSyncDescription(String activeSyncDescription) {
        this.activeSyncDescription = activeSyncDescription == null ? "" : activeSyncDescription;
    }

    public long getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(long lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public List<String> getDepartmentOptions() {
        return new ArrayList<>(departmentOptions);
    }

    public List<String> getLocationOptions() {
        return new ArrayList<>(locationOptions);
    }

    public List<String> getAssetTypeOptions() {
        return new ArrayList<>(assetTypeOptions);
    }

    public Set<String> getSelectedDepartmentOptions() {
        return new LinkedHashSet<>(selectedDepartmentOptions);
    }

    public Set<String> getSelectedLocationOptions() {
        return new LinkedHashSet<>(selectedLocationOptions);
    }

    public Set<String> getSelectedAssetTypeOptions() {
        return new LinkedHashSet<>(selectedAssetTypeOptions);
    }

    public void replaceFilterOptions(List<String> departments, List<String> locations, List<String> assetTypes) {
        departmentOptions.clear();
        if (departments != null) {
            departmentOptions.addAll(departments);
        }
        locationOptions.clear();
        if (locations != null) {
            locationOptions.addAll(locations);
        }
        assetTypeOptions.clear();
        if (assetTypes != null) {
            assetTypeOptions.addAll(assetTypes);
        }
    }

    public void replaceSelectedDepartmentOptions(Collection<String> selections) {
        selectedDepartmentOptions.clear();
        addSelectedValues(selectedDepartmentOptions, selections);
    }

    public void replaceSelectedLocationOptions(Collection<String> selections) {
        selectedLocationOptions.clear();
        addSelectedValues(selectedLocationOptions, selections);
    }

    public void replaceSelectedAssetTypeOptions(Collection<String> selections) {
        selectedAssetTypeOptions.clear();
        addSelectedValues(selectedAssetTypeOptions, selections);
    }

    public void clearSelections() {
        selectedDepartmentOptions.clear();
        selectedLocationOptions.clear();
        selectedAssetTypeOptions.clear();
    }

    private void addSelectedValues(Set<String> target, Collection<String> selections) {
        if (selections == null) {
            return;
        }
        for (String value : selections) {
            if (value != null && !value.trim().isEmpty()) {
                target.add(value.trim());
            }
        }
    }
}
