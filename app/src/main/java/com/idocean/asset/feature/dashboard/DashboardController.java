package com.idocean.asset.feature.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.idocean.asset.R;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.AssetSyncEntrypointMode;
import com.idocean.asset.data.repository.AssetSyncErrorType;
import com.idocean.asset.data.repository.AssetSyncProgressCallback;
import com.idocean.asset.data.repository.DashboardMetricsRepository;
import com.idocean.asset.data.repository.SessionRepository;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetSyncQuery;
import com.idocean.asset.model.SessionConfig;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DashboardController {
    public interface DashboardUi {
        boolean canRender();

        void renderDashboardState();

        void showToast(String message);
    }

    public enum SyncMode {
        ALL,
        FILTERED,
        SESSION
    }

    public enum SyncError {
        NONE,
        FILTER_SELECTION_REQUIRED,
        SESSION_DEPARTMENT_REQUIRED
    }

    public static final class SyncPlan {
        private final AssetSyncQuery query;
        private final SyncError error;

        private SyncPlan(AssetSyncQuery query, SyncError error) {
            this.query = query;
            this.error = error == null ? SyncError.NONE : error;
        }

        public static SyncPlan success(AssetSyncQuery query) {
            return new SyncPlan(query, SyncError.NONE);
        }

        public static SyncPlan error(SyncError error) {
            return new SyncPlan(null, error);
        }

        public AssetSyncQuery getQuery() {
            return query;
        }

        public SyncError getError() {
            return error;
        }

        public boolean isValid() {
            return error == SyncError.NONE && query != null;
        }
    }

    private static final String PREF_DASHBOARD = "ido_dashboard";
    private static final String KEY_LAST_SYNC_AT = "last_sync_at";
    private static final int DEFAULT_BATCH_SIZE = 300;
    private static final String CACHE_SOURCE = "CACHE";
    private static final String API_SOURCE = "API";

    private final Context appContext;
    private final AssetRepository assetRepository;
    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final SessionRepository sessionRepository;
    private final SharedPreferences dashboardPreferences;
    private Handler mainHandler;
    private ExecutorService dashboardExecutor;
    private final DashboardState state = new DashboardState();

    public DashboardController(
            Context appContext,
            AssetRepository assetRepository,
            DashboardMetricsRepository dashboardMetricsRepository,
            SessionRepository sessionRepository
    ) {
        this.appContext = appContext == null ? null : appContext.getApplicationContext();
        this.assetRepository = assetRepository;
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.sessionRepository = sessionRepository;
        this.dashboardPreferences = this.appContext == null
                ? null
                : this.appContext.getSharedPreferences(PREF_DASHBOARD, Context.MODE_PRIVATE);
        if (this.dashboardPreferences != null) {
            state.setLastSyncAt(this.dashboardPreferences.getLong(KEY_LAST_SYNC_AT, 0L));
        }
    }

    public DashboardState getState() {
        return state;
    }

    public void initializeDashboard() {
        state.setSyncUiState(DashboardState.SyncUiState.IDLE);
        state.setSyncErrorType(AssetSyncErrorType.NONE);
        state.setSyncing(false);
        state.setProgressIndeterminate(true);
        state.setPreviewTotalCount(0);
        state.setPreviewLoadedCount(0);
        state.setPreviewBatchSize(DEFAULT_BATCH_SIZE);
        state.setProgressBatchIndex(0);
        state.setProgressTotalBatches(0);
        state.setCachedAssetCount(0);
        state.setCachedAssetSource(CACHE_SOURCE);
        state.setPreviewText(getString(R.string.dashboard_preview_idle));
        state.setProgressText(getString(R.string.dashboard_progress_idle));
        refreshFilterOptions(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        state.setStatusText(buildStatusMessage(state.getCachedAssetCount(), state.getCachedAssetSource()));
        syncLastSyncFromPreferences();
    }

    public void loadDashboardCacheSnapshot(final DashboardUi ui, final String overrideMessage) {
        refreshDashboardSnapshotAsync(ui, overrideMessage);
    }

    public void syncWithMode(final SyncMode syncMode, final DashboardUi ui) {
        if (state.isSyncing()) {
            return;
        }

        SyncPlan plan = buildSyncPlan(syncMode);
        if (!plan.isValid()) {
            String message = resolveSyncErrorMessage(plan.getError());
            if (!message.isEmpty() && ui != null) {
                ui.showToast(message);
            }
            return;
        }

        state.setSyncUiState(DashboardState.SyncUiState.LOADING);
        state.setSyncErrorType(AssetSyncErrorType.NONE);
        state.setSyncing(true);
        state.setProgressIndeterminate(true);
        state.setPreviewTotalCount(0);
        state.setPreviewLoadedCount(0);
        state.setPreviewText(getString(R.string.dashboard_sync_stage_loading_data));
        state.setProgressText(getString(R.string.dashboard_progress_waiting));
        state.setStatusText(getString(
                R.string.dashboard_sync_in_progress_filtered,
                plan.getQuery().describe()
        ));

        if (canRender(ui)) {
            ui.renderDashboardState();
        }

        assetRepository.syncAssetsFromApi(plan.getQuery(), toEntrypointMode(syncMode), new AssetSyncProgressCallback() {
            @Override
            public void onCountReady(int totalCount, int batchSize, String description) {
                if (!canRender(ui)) {
                    return;
                }
                state.setSyncUiState(DashboardState.SyncUiState.LOADING);
                state.setSyncErrorType(AssetSyncErrorType.NONE);
                state.setPreviewTotalCount(totalCount);
                state.setPreviewLoadedCount(0);
                state.setPreviewBatchSize(batchSize);
                state.setProgressBatchIndex(0);
                state.setProgressTotalBatches(0);
                state.setProgressIndeterminate(totalCount < 0);
                state.setPreviewText(getString(R.string.dashboard_sync_stage_loading_data));
                if (totalCount < 0) {
                    state.setProgressText(getString(
                            R.string.dashboard_preview_result_unknown,
                            batchSize,
                            description
                    ));
                } else {
                    state.setProgressText(getString(
                            R.string.dashboard_preview_result,
                            totalCount,
                            batchSize,
                            description
                    ));
                }
                ui.renderDashboardState();
            }

            @Override
            public void onProgress(int loadedCount, int totalCount, int batchIndex, int totalBatches) {
                if (!canRender(ui)) {
                    return;
                }
                state.setSyncUiState(DashboardState.SyncUiState.LOADING);
                state.setSyncErrorType(AssetSyncErrorType.NONE);
                state.setPreviewLoadedCount(loadedCount);
                state.setPreviewTotalCount(totalCount);
                state.setProgressBatchIndex(batchIndex);
                state.setProgressTotalBatches(totalBatches);
                state.setProgressIndeterminate(totalCount < 0);
                state.setPreviewText(getString(R.string.dashboard_sync_stage_processing_data));
                if (totalCount < 0) {
                    state.setProgressText(getString(
                            R.string.dashboard_progress_value_unknown,
                            loadedCount,
                            batchIndex
                    ));
                } else {
                    state.setProgressText(getString(
                            R.string.dashboard_progress_value,
                            loadedCount,
                            totalCount,
                            batchIndex,
                            totalBatches
                    ));
                }
                ui.renderDashboardState();
            }

            @Override
            public void onSuccess(List<Asset> assets, String message) {
                if (!canRender(ui)) {
                    return;
                }
                state.setSyncUiState(DashboardState.SyncUiState.SUCCESS);
                state.setSyncErrorType(AssetSyncErrorType.NONE);
                state.setSyncing(false);
                state.setPreviewLoadedCount(assets == null ? 0 : assets.size());
                state.setCachedAssetCount(assets == null ? 0 : assets.size());
                state.setCachedAssetSource(API_SOURCE);
                state.setPreviewText(getString(R.string.dashboard_sync_stage_complete));
                if (state.getPreviewTotalCount() < 0) {
                    state.setProgressText(getString(
                            R.string.dashboard_progress_done_unknown,
                            state.getPreviewLoadedCount()
                    ));
                } else {
                    if (state.getPreviewTotalCount() <= 0) {
                        state.setPreviewTotalCount(state.getPreviewLoadedCount());
                    }
                    state.setProgressText(getString(
                            R.string.dashboard_progress_done,
                            state.getPreviewLoadedCount(),
                            state.getPreviewTotalCount()
                    ));
                }
                persistLastSync();
                refreshDashboardSnapshotAsync(
                        ui,
                        getString(
                                R.string.dashboard_sync_result_message,
                                message,
                                state.getCachedAssetSource()
                        )
                );
            }

            @Override
            public void onError(AssetSyncErrorType errorType, String message) {
                if (!canRender(ui)) {
                    return;
                }
                state.setSyncUiState(DashboardState.SyncUiState.ERROR);
                state.setSyncErrorType(errorType);
                state.setSyncing(false);
                state.setProgressIndeterminate(true);
                state.setPreviewText(getString(R.string.dashboard_sync_stage_failed));
                state.setProgressText(resolveSyncErrorProgressText(errorType, message));
                state.setStatusText(resolveSyncErrorMessage(errorType, message));
                ui.renderDashboardState();
            }
        });
    }

    public void clearFilters() {
        state.clearSelections();
    }

    public void replaceSelectedDepartmentOptions(Collection<String> selections) {
        state.replaceSelectedDepartmentOptions(selections);
    }

    public void replaceSelectedLocationOptions(Collection<String> selections) {
        state.replaceSelectedLocationOptions(selections);
    }

    public void replaceSelectedAssetTypeOptions(Collection<String> selections) {
        state.replaceSelectedAssetTypeOptions(normalizeAssetTypeSelections(selections));
    }

    public List<String> getDepartmentOptions() {
        return state.getDepartmentOptions();
    }

    public List<String> getLocationOptions() {
        return state.getLocationOptions();
    }

    public List<String> getAssetTypeOptions() {
        return state.getAssetTypeOptions();
    }

    public Set<String> getSelectedDepartmentOptions() {
        return state.getSelectedDepartmentOptions();
    }

    public Set<String> getSelectedLocationOptions() {
        return state.getSelectedLocationOptions();
    }

    public Set<String> getSelectedAssetTypeOptions() {
        return state.getSelectedAssetTypeOptions();
    }

    public String getDepartmentFilterText() {
        return buildSelectionSummary(
                state.getSelectedDepartmentOptions(),
                getString(R.string.dashboard_filter_any_department)
        );
    }

    public String getLocationFilterText() {
        return buildSelectionSummary(
                state.getSelectedLocationOptions(),
                getString(R.string.dashboard_filter_any_location)
        );
    }

    public String getAssetTypeFilterText() {
        return buildSelectionSummary(
                state.getSelectedAssetTypeOptions(),
                getString(R.string.dashboard_filter_any_asset_type)
        );
    }

    public String getFormattedLastSync() {
        long lastSyncAt = state.getLastSyncAt();
        if (lastSyncAt <= 0) {
            return getString(R.string.dashboard_sync_never);
        }
        return new java.text.SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
                .format(new Date(lastSyncAt));
    }

    public SyncPlan buildSyncPlan(SyncMode syncMode) {
        SessionConfig sessionConfig = sessionRepository == null ? null : sessionRepository.getSession();
        return createSyncPlan(
                syncMode,
                state.getSelectedDepartmentOptions(),
                state.getSelectedLocationOptions(),
                state.getSelectedAssetTypeOptions(),
                sessionConfig,
                DEFAULT_BATCH_SIZE
        );
    }

    public static SyncPlan createSyncPlan(
            SyncMode syncMode,
            Collection<String> departmentSelections,
            Collection<String> locationSelections,
            Collection<String> assetTypeSelections,
            SessionConfig sessionConfig,
            int batchSize
    ) {
        List<String> departments = normalizeDepartmentSelections(departmentSelections);
        List<String> locations = normalizeLocationSelections(locationSelections);
        List<String> assetTypes = normalizeAssetTypeSelections(assetTypeSelections);

        if (syncMode == SyncMode.FILTERED
                && departments.isEmpty()
                && locations.isEmpty()
                && assetTypes.isEmpty()) {
            return SyncPlan.error(SyncError.FILTER_SELECTION_REQUIRED);
        }

        if (syncMode == SyncMode.SESSION) {
            String sessionDepartment = sessionConfig == null
                    ? ""
                    : AssetFieldNormalizer.normalizeDepartmentForDisplay(sessionConfig.getDepartment());
            if (sessionDepartment.isEmpty()) {
                return SyncPlan.error(SyncError.SESSION_DEPARTMENT_REQUIRED);
            }
            departments = Collections.singletonList(sessionDepartment);
        }

        List<String> locationQueries = new ArrayList<>();
        for (String location : locations) {
            String locationKey = AssetLocationUtils.resolveLocationKey(location);
            if (!locationKey.isEmpty()) {
                locationQueries.add(locationKey);
            }
        }

        if (syncMode == SyncMode.FILTERED || syncMode == SyncMode.SESSION) {
            return SyncPlan.success(AssetSyncQuery.withFilters(
                    departments,
                    locations,
                    locationQueries,
                    assetTypes,
                    batchSize
            ));
        }

        return SyncPlan.success(AssetSyncQuery.withFilters(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                batchSize
        ));
    }

    private static AssetSyncEntrypointMode toEntrypointMode(SyncMode syncMode) {
        if (syncMode == SyncMode.SESSION) {
            return AssetSyncEntrypointMode.SESSION;
        }
        if (syncMode == SyncMode.FILTERED) {
            return AssetSyncEntrypointMode.FILTERED;
        }
        return AssetSyncEntrypointMode.FULL;
    }

    public static String buildSelectionSummary(Collection<String> selections, String emptyLabel) {
        if (selections == null || selections.isEmpty()) {
            return emptyLabel == null ? "" : emptyLabel;
        }
        List<String> values = new ArrayList<>();
        for (String selection : selections) {
            String value = selection == null ? "" : selection.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        if (values.isEmpty()) {
            return emptyLabel == null ? "" : emptyLabel;
        }
        if (values.size() <= 2) {
            return String.join(", ", values);
        }
        return values.get(0) + ", " + values.get(1) + " +" + (values.size() - 2);
    }

    private void refreshFilterOptions(
            Collection<String> runtimeDepartments,
            Collection<String> runtimeLocations,
            Collection<String> runtimeAssetTypes
    ) {
        Set<String> departmentSet = new LinkedHashSet<>();
        if (appContext != null) {
            String[] defaults = appContext.getResources().getStringArray(R.array.known_department_options);
            for (String option : defaults) {
                String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(option);
                if (!normalized.isEmpty()) {
                    departmentSet.add(normalized);
                }
            }
        }
        addNormalizedDepartments(departmentSet, runtimeDepartments);
        SessionConfig sessionConfig = sessionRepository == null ? null : sessionRepository.getSession();
        if (sessionConfig != null) {
            String sessionDepartment = AssetFieldNormalizer.normalizeDepartmentForDisplay(sessionConfig.getDepartment());
            if (!sessionDepartment.isEmpty()) {
                departmentSet.add(sessionDepartment);
            }
        }
        departmentSet.addAll(state.getSelectedDepartmentOptions());

        Set<String> locationSet = new LinkedHashSet<>();
        if (appContext != null) {
            String[] defaults = appContext.getResources().getStringArray(R.array.known_location_options);
            for (String option : defaults) {
                String normalized = AssetLocationUtils.normalizeLocationForDisplay(option);
                if (!normalized.isEmpty()) {
                    locationSet.add(normalized);
                }
            }
        }
        addNormalizedLocations(locationSet, runtimeLocations);
        locationSet.addAll(state.getSelectedLocationOptions());

        Set<String> assetTypeSet = new LinkedHashSet<>();
        if (appContext != null) {
            String[] defaults = appContext.getResources().getStringArray(R.array.known_asset_type_options);
            for (String option : defaults) {
                String normalized = AssetFieldNormalizer.normalizeAssetTypeForFilter(option);
                if (!normalized.isEmpty()) {
                    assetTypeSet.add(normalized);
                }
            }
        }
        addNormalizedAssetTypes(assetTypeSet, runtimeAssetTypes);
        addNormalizedAssetTypes(assetTypeSet, state.getSelectedAssetTypeOptions());

        state.replaceFilterOptions(
                new ArrayList<>(departmentSet),
                new ArrayList<>(locationSet),
                new ArrayList<>(assetTypeSet)
        );
    }

    private void refreshDashboardSnapshotAsync(DashboardUi ui, String overrideMessage) {
        getDashboardExecutor().execute(() -> {
            List<Asset> cachedAssets = assetRepository.getCachedAssets();
            String source = assetRepository.getLastSource();
            List<String> departments = assetRepository.collectDistinctValues("department");
            List<String> locations = assetRepository.collectDistinctValues("location");
            List<String> assetTypes = assetRepository.collectDistinctValues("assetType");
            int cachedCount = cachedAssets.size();
            long lastSyncAt = dashboardPreferences == null ? 0L : dashboardPreferences.getLong(KEY_LAST_SYNC_AT, 0L);

            getMainHandler().post(() -> {
                if (!canRender(ui)) {
                    return;
                }
                state.setCachedAssetCount(cachedCount);
                state.setCachedAssetSource(source == null ? CACHE_SOURCE : source);
                state.setLastSyncAt(lastSyncAt);
                refreshFilterOptions(departments, locations, assetTypes);
                if (overrideMessage != null || state.getSyncUiState() == DashboardState.SyncUiState.IDLE) {
                    state.setStatusText(overrideMessage != null
                            ? overrideMessage
                            : buildStatusMessage(state.getCachedAssetCount(), state.getCachedAssetSource()));
                }
                ui.renderDashboardState();
            });
        });
    }

    private String resolveSyncErrorMessage(AssetSyncErrorType errorType, String rawMessage) {
        if (errorType == null) {
            errorType = AssetSyncErrorType.UNKNOWN;
        }
        switch (errorType) {
            case NETWORK:
                return getString(R.string.dashboard_sync_error_network);
            case TIMEOUT:
                return getString(R.string.dashboard_sync_error_timeout);
            case API:
                return getString(R.string.dashboard_sync_error_api);
            case PARSE:
                return getString(R.string.dashboard_sync_error_parse);
            case STORAGE:
                return getString(R.string.dashboard_sync_error_storage);
            case UNKNOWN:
            case NONE:
            default:
                String message = rawMessage == null ? "" : rawMessage.trim();
                return message.isEmpty()
                        ? getString(R.string.dashboard_progress_failed)
                        : message;
        }
    }

    private String resolveSyncErrorProgressText(AssetSyncErrorType errorType, String rawMessage) {
        if (errorType != null && errorType != AssetSyncErrorType.UNKNOWN && errorType != AssetSyncErrorType.NONE) {
            return getString(R.string.dashboard_progress_failed);
        }
        String message = rawMessage == null ? "" : rawMessage.trim();
        return message.isEmpty() ? getString(R.string.dashboard_progress_failed) : message;
    }

    private void syncLastSyncFromPreferences() {
        if (dashboardPreferences != null) {
            state.setLastSyncAt(dashboardPreferences.getLong(KEY_LAST_SYNC_AT, 0L));
        }
    }

    private void persistLastSync() {
        if (dashboardPreferences == null) {
            return;
        }
        long now = System.currentTimeMillis();
        dashboardPreferences.edit().putLong(KEY_LAST_SYNC_AT, now).apply();
        state.setLastSyncAt(now);
    }

    private String buildStatusMessage(int assetCount, String lastSource) {
        if (assetCount <= 0) {
            return getString(R.string.dashboard_status_message_empty);
        }
        if (dashboardMetricsRepository != null && dashboardMetricsRepository.hasInventorySummary()) {
            return getString(
                    R.string.dashboard_status_message_inventory_summary,
                    assetCount,
                    dashboardMetricsRepository.getCheckedCount(),
                    dashboardMetricsRepository.getMissingCount(),
                    dashboardMetricsRepository.getOutsideCount()
            );
        }
        return getString(R.string.dashboard_status_message_cache, assetCount, lastSource);
    }

    private String resolveSyncErrorMessage(SyncError error) {
        if (error == SyncError.FILTER_SELECTION_REQUIRED) {
            return getString(R.string.dashboard_sync_filter_missing);
        }
        if (error == SyncError.SESSION_DEPARTMENT_REQUIRED) {
            return getString(R.string.dashboard_sync_session_missing_department);
        }
        return "";
    }

    private boolean canRender(DashboardUi ui) {
        return ui != null && ui.canRender();
    }

    public void shutdown() {
        if (dashboardExecutor != null) {
            dashboardExecutor.shutdownNow();
            dashboardExecutor = null;
        }
    }

    private Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

    private ExecutorService getDashboardExecutor() {
        if (dashboardExecutor == null) {
            dashboardExecutor = Executors.newSingleThreadExecutor();
        }
        return dashboardExecutor;
    }

    private String getString(int resId, Object... formatArgs) {
        if (appContext == null) {
            return "";
        }
        return formatArgs == null || formatArgs.length == 0
                ? appContext.getString(resId)
                : appContext.getString(resId, formatArgs);
    }

    private static void addNormalizedDepartments(Set<String> target, Collection<String> values) {
        if (target == null || values == null) {
            return;
        }
        for (String value : values) {
            String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(value);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private static void addNormalizedLocations(Set<String> target, Collection<String> values) {
        if (target == null || values == null) {
            return;
        }
        for (String value : values) {
            String normalized = AssetLocationUtils.normalizeLocationForDisplay(value);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private static void addNormalizedAssetTypes(Set<String> target, Collection<String> values) {
        if (target == null || values == null) {
            return;
        }
        for (String value : values) {
            String normalized = AssetFieldNormalizer.normalizeAssetTypeForFilter(value);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private static List<String> normalizeDepartmentSelections(Collection<String> selections) {
        List<String> normalized = new ArrayList<>();
        if (selections == null) {
            return normalized;
        }
        for (String selection : selections) {
            String value = AssetFieldNormalizer.normalizeDepartmentForDisplay(selection);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static List<String> normalizeLocationSelections(Collection<String> selections) {
        List<String> normalized = new ArrayList<>();
        if (selections == null) {
            return normalized;
        }
        for (String selection : selections) {
            String value = AssetLocationUtils.normalizeLocationForDisplay(selection);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static List<String> normalizeAssetTypeSelections(Collection<String> selections) {
        List<String> normalized = new ArrayList<>();
        if (selections == null) {
            return normalized;
        }
        for (String selection : selections) {
            String value = AssetFieldNormalizer.normalizeAssetTypeForFilter(selection);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

}
