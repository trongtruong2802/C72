package com.idocean.asset.feature.dashboard;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.idocean.asset.data.repository.SessionRepository;
import com.idocean.asset.feature.dashboard.DashboardController;
import com.idocean.asset.feature.dashboard.DashboardState;
import com.idocean.asset.ui.assets.AssetsActivity;
import com.idocean.asset.ui.checkout.CheckoutActivity;
import com.idocean.asset.ui.inventory.InventoryActivity;
import com.idocean.asset.ui.logs.LogsActivity;
import com.idocean.asset.ui.lookup.LookupActivity;
import com.idocean.asset.ui.settings.SettingsActivity;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity implements DashboardController.DashboardUi {
    private DashboardController dashboardController;

    private MaterialToolbar toolbarDashboard;
    private MaterialCardView cardDashboardHero;
    private TextView tvDataStatus;
    private TextView tvAssetCount;
    private TextView tvLastSync;
    private TextView tvStateChip;
    private TextView tvPreviewCount;
    private TextView tvSyncProgress;
    private MaterialAutoCompleteTextView actDepartmentFilter;
    private MaterialAutoCompleteTextView etLocationFilter;
    private MaterialAutoCompleteTextView etAssetTypeFilter;
    private MaterialButton buttonClearFilters;
    private MaterialButton buttonLoadAll;
    private MaterialButton buttonSyncFiltered;
    private MaterialButton buttonSyncSession;
    private LinearProgressIndicator progressDashboardSync;
    private ViewStub stubDashboardQuickActions;
    private View[] introViews;
    private boolean introAnimated;
    private boolean quickActionsInflated;
    private boolean reduceIntroMotion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        setContentView(R.layout.activity_dashboard);

        dashboardController = new DashboardController(
                getApplicationContext(),
                com.idocean.asset.data.repository.AssetRepository.getInstance(),
                com.idocean.asset.data.repository.DashboardMetricsRepository.getInstance(),
                new SessionRepository(getApplicationContext())
        );
        tintSystemBars();
        reduceIntroMotion = shouldReduceIntroMotion();
        bindViews();
        bindActions();
        dashboardController.initializeDashboard();
        renderDashboardState();
        dashboardController.loadDashboardCacheSnapshot(this, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dashboardController.loadDashboardCacheSnapshot(this, null);
        if (!introAnimated) {
            cardDashboardHero.post(() -> {
                inflateQuickActionsIfNeeded();
                runIntroAnimation();
            });
        } else {
            inflateQuickActionsIfNeeded();
        }
    }

    @Override
    protected void onDestroy() {
        if (dashboardController != null) {
            dashboardController.shutdown();
        }
        super.onDestroy();
    }

    private void bindViews() {
        toolbarDashboard = findViewById(R.id.toolbarDashboard);
        toolbarDashboard.setTitle(R.string.dashboard_toolbar_title);
        toolbarDashboard.setSubtitle(null);

        cardDashboardHero = findViewById(R.id.cardDashboardHero);
        tvDataStatus = findViewById(R.id.tvDashboardMasterStatus);
        tvAssetCount = findViewById(R.id.tvDashboardAssetCount);
        tvLastSync = findViewById(R.id.tvDashboardLastSync);
        tvStateChip = findViewById(R.id.tvDashboardStateChip);
        tvPreviewCount = findViewById(R.id.tvDashboardPreviewCount);
        tvSyncProgress = findViewById(R.id.tvDashboardSyncProgress);
        actDepartmentFilter = findViewById(R.id.actDashboardDepartmentFilter);
        etLocationFilter = findViewById(R.id.etDashboardLocationFilter);
        etAssetTypeFilter = findViewById(R.id.etDashboardAssetTypeFilter);
        buttonClearFilters = findViewById(R.id.buttonDashboardClearFilters);
        buttonLoadAll = findViewById(R.id.buttonDashboardLoadAll);
        buttonSyncFiltered = findViewById(R.id.buttonDashboardSyncFiltered);
        buttonSyncSession = findViewById(R.id.buttonDashboardSyncSession);
        progressDashboardSync = findViewById(R.id.progressDashboardSync);
        stubDashboardQuickActions = findViewById(R.id.stubDashboardQuickActions);

        introViews = new View[]{
                cardDashboardHero
        };
    }

    private void bindActions() {
        buttonLoadAll.setOnClickListener(v -> dashboardController.syncWithMode(DashboardController.SyncMode.ALL, this));
        buttonSyncFiltered.setOnClickListener(v -> dashboardController.syncWithMode(DashboardController.SyncMode.FILTERED, this));
        buttonSyncSession.setOnClickListener(v -> dashboardController.syncWithMode(DashboardController.SyncMode.SESSION, this));
        buttonClearFilters.setOnClickListener(v -> {
            dashboardController.clearFilters();
            renderDashboardState();
        });
        actDepartmentFilter.setOnClickListener(v -> openDepartmentFilterDialog());
        actDepartmentFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
                openDepartmentFilterDialog();
            }
        });
        etLocationFilter.setOnClickListener(v -> openLocationFilterDialog());
        etLocationFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
                openLocationFilterDialog();
            }
        });
        etAssetTypeFilter.setOnClickListener(v -> openAssetTypeFilterDialog());
        etAssetTypeFilter.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.clearFocus();
                openAssetTypeFilterDialog();
            }
        });
    }

    private void bindNavigation(int viewId, final Class<?> activityClass) {
        findViewById(viewId).setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, activityClass)));
    }

    private void tintSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.dashboard_primary_dark));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.dashboard_bg_top));
    }

    private void renderDashboardFromState() {
        if (dashboardController == null) {
            return;
        }

        DashboardState state = dashboardController.getState();
        tvDataStatus.setText(state.getStatusText());
        tvAssetCount.setText(getString(R.string.dashboard_asset_count_value, state.getCachedAssetCount()));
        tvLastSync.setText(dashboardController.getFormattedLastSync());
        progressDashboardSync.setVisibility(state.isSyncing() ? View.VISIBLE : View.GONE);
        if (state.isSyncing()) {
            if (state.isProgressIndeterminate()) {
                progressDashboardSync.setIndeterminate(true);
                progressDashboardSync.setProgressCompat(0, false);
            } else {
                progressDashboardSync.setIndeterminate(false);
                int max = Math.max(state.getPreviewTotalCount(), 1);
                progressDashboardSync.setMax(max);
                progressDashboardSync.setProgressCompat(Math.min(state.getPreviewLoadedCount(), max), true);
            }
        }
        setSyncButtonsEnabled(!state.isSyncing());
        bindStatusChip(state);
        updateSyncInfoViews();
        updateFilterTexts();
    }

    @Override
    public boolean canRender() {
        return !isFinishing() && !isDestroyed();
    }

    @Override
    public void renderDashboardState() {
        renderDashboardFromState();
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateSyncInfoViews() {
        if (dashboardController == null) {
            return;
        }
        DashboardState state = dashboardController.getState();
        tvPreviewCount.setText(state.getPreviewText());
        tvSyncProgress.setText(state.getProgressText());
    }

    private void setSyncButtonsEnabled(boolean enabled) {
        buttonClearFilters.setEnabled(enabled);
        buttonLoadAll.setEnabled(enabled);
        buttonSyncFiltered.setEnabled(enabled);
        buttonSyncSession.setEnabled(enabled);
        actDepartmentFilter.setEnabled(enabled);
        etLocationFilter.setEnabled(enabled);
        etAssetTypeFilter.setEnabled(enabled);
    }

    private void openDepartmentFilterDialog() {
        if (dashboardController == null) {
            return;
        }
        showMultiSelectDialog(
                getString(R.string.dashboard_filter_department),
                dashboardController.getDepartmentOptions(),
                dashboardController.getSelectedDepartmentOptions(),
                selections -> {
                    dashboardController.replaceSelectedDepartmentOptions(selections);
                    renderDashboardState();
                }
        );
    }

    private void openLocationFilterDialog() {
        if (dashboardController == null) {
            return;
        }
        showMultiSelectDialog(
                getString(R.string.dashboard_filter_location),
                dashboardController.getLocationOptions(),
                dashboardController.getSelectedLocationOptions(),
                selections -> {
                    dashboardController.replaceSelectedLocationOptions(selections);
                    renderDashboardState();
                }
        );
    }

    private void openAssetTypeFilterDialog() {
        if (dashboardController == null) {
            return;
        }
        showMultiSelectDialog(
                getString(R.string.dashboard_filter_asset_type),
                dashboardController.getAssetTypeOptions(),
                dashboardController.getSelectedAssetTypeOptions(),
                selections -> {
                    dashboardController.replaceSelectedAssetTypeOptions(selections);
                    renderDashboardState();
                }
        );
    }

    private void showMultiSelectDialog(
            String title,
            List<String> options,
            Set<String> selectedValues,
            SelectionAppliedCallback onApplied
    ) {
        if (options == null || options.isEmpty()) {
            return;
        }
        String[] items = options.toArray(new String[0]);
        boolean[] checkedItems = new boolean[items.length];
        final Set<String> workingSelection = new LinkedHashSet<>(selectedValues);
        for (int i = 0; i < items.length; i++) {
            checkedItems[i] = workingSelection.contains(items[i]);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    String item = items[which];
                    if (isChecked) {
                        workingSelection.add(item);
                    } else {
                        workingSelection.remove(item);
                    }
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    selectedValues.clear();
                    for (String option : options) {
                        if (workingSelection.contains(option)) {
                            selectedValues.add(option);
                        }
                    }
                    if (onApplied != null) {
                        onApplied.onApplied(new LinkedHashSet<>(selectedValues));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private interface SelectionAppliedCallback {
        void onApplied(Set<String> selections);
    }

    private void updateFilterTexts() {
        if (dashboardController == null) {
            return;
        }
        actDepartmentFilter.setText(dashboardController.getDepartmentFilterText(), false);
        etLocationFilter.setText(dashboardController.getLocationFilterText(), false);
        etAssetTypeFilter.setText(dashboardController.getAssetTypeFilterText(), false);
    }

    private void bindStatusChip(DashboardState state) {
        boolean loading = state != null && state.getSyncUiState() == DashboardState.SyncUiState.LOADING;
        boolean success = state != null && state.getSyncUiState() == DashboardState.SyncUiState.SUCCESS;
        boolean error = state != null && state.getSyncUiState() == DashboardState.SyncUiState.ERROR;
        int assetCount = state == null ? 0 : state.getCachedAssetCount();
        String lastSource = state == null ? "" : state.getCachedAssetSource();
        int backgroundRes;
        int textColorRes;
        int labelRes;

        if (loading) {
            backgroundRes = R.drawable.bg_dashboard_chip_loading;
            textColorRes = R.color.dashboard_chip_loading_text;
            labelRes = R.string.dashboard_status_loading;
        } else if (error) {
            backgroundRes = R.drawable.bg_dashboard_chip_error;
            textColorRes = R.color.dashboard_chip_error_text;
            labelRes = R.string.dashboard_status_error;
        } else if (success) {
            backgroundRes = R.drawable.bg_dashboard_chip_ready;
            textColorRes = R.color.dashboard_chip_ready_text;
            labelRes = R.string.dashboard_status_success;
        } else if (assetCount <= 0) {
            backgroundRes = R.drawable.bg_dashboard_chip_empty;
            textColorRes = R.color.dashboard_chip_empty_text;
            labelRes = R.string.dashboard_status_empty;
        } else if ("API".equalsIgnoreCase(lastSource)) {
            backgroundRes = R.drawable.bg_dashboard_chip_ready;
            textColorRes = R.color.dashboard_chip_ready_text;
            labelRes = R.string.dashboard_status_ready;
        } else {
            backgroundRes = R.drawable.bg_dashboard_chip_cache;
            textColorRes = R.color.dashboard_chip_cache_text;
            labelRes = R.string.dashboard_status_cache;
        }

        tvStateChip.setBackgroundResource(backgroundRes);
        tvStateChip.setText(labelRes);
        tvStateChip.setTextColor(ContextCompat.getColor(this, textColorRes));
    }

    private void runIntroAnimation() {
        if (introAnimated) {
            return;
        }
        introAnimated = true;

        if (reduceIntroMotion) {
            for (View view : introViews) {
                if (view == null) {
                    continue;
                }
                view.setAlpha(1f);
                view.setTranslationY(0f);
            }
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float startOffset = 20f * density;
        long duration = 280L;
        long stagger = 26L;
        for (int i = 0; i < introViews.length; i++) {
            View view = introViews[i];
            if (view == null) {
                continue;
            }
            view.setAlpha(0f);
            view.setTranslationY(startOffset);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * stagger)
                    .setDuration(duration)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void inflateQuickActionsIfNeeded() {
        if (quickActionsInflated || stubDashboardQuickActions == null) {
            return;
        }
        View inflated = stubDashboardQuickActions.inflate();
        quickActionsInflated = true;
        bindNavigation(R.id.buttonOpenInventory, InventoryActivity.class);
        bindNavigation(R.id.buttonOpenCheckOut, CheckoutActivity.class);
        bindNavigation(R.id.buttonOpenCheckIn, AssetsActivity.class);
        bindNavigation(R.id.buttonOpenHandover, LogsActivity.class);
        bindNavigation(R.id.buttonOpenLookup, LookupActivity.class);
        bindNavigation(R.id.buttonOpenHistory, SettingsActivity.class);

        View[] quickActionViews = new View[]{
                inflated.findViewById(R.id.tvDashboardQuickActionsTitle),
                inflated.findViewById(R.id.buttonOpenInventory),
                inflated.findViewById(R.id.buttonOpenCheckOut),
                inflated.findViewById(R.id.buttonOpenCheckIn),
                inflated.findViewById(R.id.buttonOpenHandover),
                inflated.findViewById(R.id.buttonOpenLookup),
                inflated.findViewById(R.id.buttonOpenHistory)
        };

        if (introAnimated) {
            for (View view : quickActionViews) {
                if (view == null) {
                    continue;
                }
                view.setAlpha(0f);
                view.setTranslationY(10f * getResources().getDisplayMetrics().density);
                view.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(180L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
            return;
        }

        introViews = new View[]{
                cardDashboardHero,
                quickActionViews[0],
                quickActionViews[1],
                quickActionViews[2],
                quickActionViews[3],
                quickActionViews[4],
                quickActionViews[5],
                quickActionViews[6]
        };
    }

    private boolean shouldReduceIntroMotion() {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        return activityManager != null && activityManager.isLowRamDevice();
    }
}
