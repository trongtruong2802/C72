package com.idocean.asset.ui.assets;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.AssetRepositoryCallback;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetFilterCriteria;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.ui.lookup.LookupActivity;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssetsActivity extends AppCompatActivity {
    private static final String ALL_OPTION = "Tat ca";
    private static final String SCREEN = "Assets";
    private static final String FLOW_FILTER = "filter_search";
    private static final long FILTER_DEBOUNCE_MS = 180L;

    private final AssetRepository repository = AssetRepository.getInstance();
    private final LogRepository logRepository = LogRepository.getInstance();
    private final AssetListAdapter assetListAdapter = new AssetListAdapter(this::openAssetDetail);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService filterExecutor = Executors.newSingleThreadExecutor();
    private final Runnable applyFiltersRunnable = this::applyFilters;

    private final ActivityResultLauncher<String[]> csvPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onCsvSelected);

    private TextView tvDataStatus;
    private TextView tvAssetResultCount;
    private TextView tvAssetsSummaryTotal;
    private TextView tvAssetsSummaryVisible;
    private TextView tvAssetsSummaryFilters;
    private TextView tvAssetsEmpty;
    private TextInputEditText etAssetSearch;
    private MaterialAutoCompleteTextView actAssetTypeFilter;
    private MaterialAutoCompleteTextView actAssetDepartmentFilter;
    private MaterialAutoCompleteTextView actAssetUserFilter;
    private MaterialAutoCompleteTextView actAssetLocationFilter;
    private MaterialButton btnImportCsv;
    private MaterialButton btnClearFilters;
    private MaterialButton btnToggleFilters;
    private View cardAssetsFilters;
    private AssetRepository.CacheSnapshot currentSnapshot;
    private volatile long activeFilterRequestToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        setContentView(R.layout.activity_ido_assets);

        bindViews();
        setupRecyclerView();
        setupControls();
        setFilterPanelVisible(false);
        refreshFromRuntimeCacheAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFromRuntimeCacheAsync();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(applyFiltersRunnable);
        filterExecutor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        tvDataStatus = findViewById(R.id.tvDataStatus);
        tvAssetResultCount = findViewById(R.id.tvAssetResultCount);
        tvAssetsSummaryTotal = findViewById(R.id.tvAssetsSummaryTotal);
        tvAssetsSummaryVisible = findViewById(R.id.tvAssetsSummaryVisible);
        tvAssetsSummaryFilters = findViewById(R.id.tvAssetsSummaryFilters);
        tvAssetsEmpty = findViewById(R.id.tvAssetsEmpty);
        etAssetSearch = findViewById(R.id.etAssetSearch);
        actAssetTypeFilter = findViewById(R.id.actAssetTypeFilter);
        actAssetDepartmentFilter = findViewById(R.id.actAssetDepartmentFilter);
        actAssetUserFilter = findViewById(R.id.actAssetUserFilter);
        actAssetLocationFilter = findViewById(R.id.actAssetLocationFilter);
        btnImportCsv = findViewById(R.id.btnImportAssetsCsv);
        btnClearFilters = findViewById(R.id.btnClearAssetFilters);
        btnToggleFilters = findViewById(R.id.btnToggleAssetFilters);
        cardAssetsFilters = findViewById(R.id.cardAssetsFilters);
    }

    private void setupRecyclerView() {
        RecyclerView rvAssets = findViewById(R.id.rvAssets);
        rvAssets.setLayoutManager(new LinearLayoutManager(this));
        rvAssets.setAdapter(assetListAdapter);
    }

    private void setupControls() {
        btnImportCsv.setOnClickListener(v -> csvPickerLauncher.launch(new String[]{
                "text/*",
                "text/csv",
                "application/vnd.ms-excel"
        }));
        btnClearFilters.setOnClickListener(v -> clearFilters());
        btnToggleFilters.setOnClickListener(v -> toggleFilters());

        etAssetSearch.addTextChangedListener(simpleWatcher(() -> scheduleFilterApply(false)));
        bindFilterChange(actAssetTypeFilter);
        bindFilterChange(actAssetDepartmentFilter);
        bindFilterChange(actAssetUserFilter);
        bindFilterChange(actAssetLocationFilter);
    }

    private void bindFilterChange(MaterialAutoCompleteTextView view) {
        view.setOnItemClickListener((parent, itemView, position, id) -> scheduleFilterApply(true));
        view.addTextChangedListener(simpleWatcher(() -> scheduleFilterApply(false)));
    }

    private TextWatcher simpleWatcher(Runnable onChanged) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChanged.run();
            }
        };
    }

    private void onCsvSelected(Uri uri) {
        if (uri == null) {
            showToast(getString(R.string.assets_csv_cancelled));
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        setLoadingState(true);
        repository.importAssetsFromCsv(this, uri, new AssetRepositoryCallback() {
            @Override
            public void onSuccess(List<Asset> assets, String message) {
                refreshFromRuntimeCacheAsync();
                showToast(message);
            }

            @Override
            public void onError(String message) {
                setLoadingState(false);
                tvDataStatus.setText(message);
                showToast(message);
                scheduleFilterApply(true);
            }
        });
    }

    private void refreshFromRuntimeCacheAsync() {
        setLoadingState(true);
        repository.loadCacheSnapshotAsync(snapshot -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            setLoadingState(false);
            bindSnapshot(snapshot);
        });
    }

    private void bindSnapshot(AssetRepository.CacheSnapshot snapshot) {
        currentSnapshot = snapshot;
        List<Asset> cachedAssets = snapshot == null ? new ArrayList<>() : snapshot.getAssets();
        if (cachedAssets.isEmpty()) {
            tvDataStatus.setText(R.string.assets_cache_empty);
            assetListAdapter.submitItems(new ArrayList<>());
            populateFilterOptions(snapshot);
            scheduleFilterApply(true);
            return;
        }
        String source = snapshot == null ? "CACHE" : snapshot.getSource();
        bindAssets(getString(R.string.assets_cache_loaded, cachedAssets.size(), source), snapshot);
    }

    private void bindAssets(String message, AssetRepository.CacheSnapshot snapshot) {
        tvDataStatus.setText(message);
        populateFilterOptions(snapshot);
        scheduleFilterApply(true);
    }

    private void populateFilterOptions(AssetRepository.CacheSnapshot snapshot) {
        List<String> typeOptions = buildAssetTypeOptions(snapshot);
        List<String> departmentOptions = buildDepartmentOptions(snapshot);
        List<String> userOptions = buildOptions(snapshot == null ? new ArrayList<>() : snapshot.getDistinctValues("assignedUser"));
        List<String> locationOptions = buildLocationOptions(snapshot);

        bindDropdown(actAssetTypeFilter, typeOptions);
        bindDropdown(actAssetDepartmentFilter, departmentOptions);
        bindDropdown(actAssetUserFilter, userOptions);
        bindDropdown(actAssetLocationFilter, locationOptions);
    }

    private List<String> buildAssetTypeOptions(AssetRepository.CacheSnapshot snapshot) {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_asset_type_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeAssetTypeForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }

        List<String> runtimeValues = snapshot == null ? new ArrayList<>() : snapshot.getDistinctValues("assetType");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetFieldNormalizer.normalizeAssetTypeForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }

        return buildOptions(new ArrayList<>(options));
    }

    private List<String> buildDepartmentOptions(AssetRepository.CacheSnapshot snapshot) {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_department_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }

        List<String> runtimeValues = snapshot == null ? new ArrayList<>() : snapshot.getDistinctValues("department");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }

        return buildOptions(new ArrayList<>(options));
    }

    private List<String> buildLocationOptions(AssetRepository.CacheSnapshot snapshot) {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_location_options);
        for (String option : defaults) {
            String normalized = AssetLocationUtils.normalizeLocationForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }

        List<String> runtimeValues = snapshot == null ? new ArrayList<>() : snapshot.getDistinctValues("location");
        if (runtimeValues != null) {
            for (String value : runtimeValues) {
                String normalized = AssetLocationUtils.normalizeLocationForDisplay(value);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }

        return buildOptions(new ArrayList<>(options));
    }

    private List<String> buildOptions(List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(ALL_OPTION);
        if (values != null) {
            options.addAll(values);
        }
        return options;
    }

    private void bindDropdown(MaterialAutoCompleteTextView view, List<String> options) {
        view.setSimpleItems(options.toArray(new String[0]));
        String current = normalizeCurrentValue(view);
        if (current.isEmpty() || !options.contains(current)) {
            view.setText(ALL_OPTION, false);
            return;
        }
        if (!current.equals(textOf(view))) {
            view.setText(current, false);
        }
    }

    private void clearFilters() {
        etAssetSearch.setText("");
        actAssetTypeFilter.setText(ALL_OPTION, false);
        actAssetDepartmentFilter.setText(ALL_OPTION, false);
        actAssetUserFilter.setText(ALL_OPTION, false);
        actAssetLocationFilter.setText(ALL_OPTION, false);
        scheduleFilterApply(true);
    }

    private void toggleFilters() {
        setFilterPanelVisible(cardAssetsFilters.getVisibility() != View.VISIBLE);
    }

    private void setFilterPanelVisible(boolean visible) {
        cardAssetsFilters.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnToggleFilters.setSelected(visible);
        btnToggleFilters.setAlpha(visible ? 1f : 0.85f);
    }

    private void scheduleFilterApply(boolean immediate) {
        mainHandler.removeCallbacks(applyFiltersRunnable);
        if (immediate) {
            mainHandler.post(applyFiltersRunnable);
            return;
        }
        mainHandler.postDelayed(applyFiltersRunnable, FILTER_DEBOUNCE_MS);
    }

    private void applyFilters() {
        List<Asset> snapshotAssets = currentSnapshot == null ? new ArrayList<>() : currentSnapshot.getAssets();
        AssetFilterCriteria criteria = new AssetFilterCriteria(
                textOf(etAssetSearch),
                "",
                selectedFilterValue(actAssetTypeFilter),
                selectedFilterValue(actAssetDepartmentFilter),
                selectedFilterValue(actAssetUserFilter),
                selectedFilterValue(actAssetLocationFilter)
        );
        int activeFilters = countActiveFilters(criteria);
        int totalCount = snapshotAssets.size();
        long requestToken = ++activeFilterRequestToken;
        PerfLogger.Trace trace = PerfLogger.start(
                SCREEN,
                FLOW_FILTER,
                "filter_requested",
                "queryLength=" + criteria.getQuery().length() + " | activeFilters=" + activeFilters
        );
        filterExecutor.execute(() -> {
            if (requestToken != activeFilterRequestToken) {
                return;
            }
            List<Asset> filteredAssets = repository.filterAssets(snapshotAssets, criteria);
            if (requestToken != activeFilterRequestToken) {
                return;
            }
            mainHandler.post(() -> {
                if (isFinishing() || isDestroyed() || requestToken != activeFilterRequestToken) {
                    return;
                }
                assetListAdapter.submitItems(filteredAssets);
                tvAssetResultCount.setText(getString(R.string.assets_result_count, filteredAssets.size()));
                updateSummary(totalCount, filteredAssets.size(), activeFilters);
                updateEmptyState(filteredAssets.isEmpty(), totalCount > 0);
                trace.finish(logRepository, "filter_completed", "resultCount=" + filteredAssets.size());
            });
        });
    }

    private void updateSummary(int totalCount, int visibleCount, int activeFilterCount) {
        tvAssetsSummaryTotal.setText(String.valueOf(totalCount));
        tvAssetsSummaryVisible.setText(String.valueOf(visibleCount));
        tvAssetsSummaryFilters.setText(String.valueOf(activeFilterCount));
    }

    private int countActiveFilters(AssetFilterCriteria criteria) {
        int count = 0;
        if (criteria != null && !criteria.getQuery().isEmpty()) {
            count++;
        }
        if (criteria != null && !criteria.getAssetType().isEmpty()) {
            count++;
        }
        if (criteria != null && !criteria.getDepartment().isEmpty()) {
            count++;
        }
        if (criteria != null && !criteria.getAssignedUser().isEmpty()) {
            count++;
        }
        if (criteria != null && !criteria.getLocation().isEmpty()) {
            count++;
        }
        return count;
    }

    private void updateEmptyState(boolean empty, boolean hasCache) {
        tvAssetsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) {
            return;
        }
        if (hasCache) {
            tvAssetsEmpty.setText(R.string.assets_empty_filtered);
        } else {
            tvAssetsEmpty.setText(R.string.assets_empty_state);
        }
    }

    private void openAssetDetail(Asset asset) {
        if (asset == null) {
            showToast(getString(R.string.assets_detail_open_error));
            return;
        }
        Intent intent = new Intent(this, LookupActivity.class);
        intent.putExtra(LookupActivity.EXTRA_ASSET_CODE, asset.getAssetCode());
        intent.putExtra(LookupActivity.EXTRA_ASSET_TID, asset.getTid());
        startActivity(intent);
    }

    private void setLoadingState(boolean loading) {
        btnImportCsv.setEnabled(!loading);
        btnClearFilters.setEnabled(!loading);
        btnToggleFilters.setEnabled(!loading);
        if (loading) {
            tvDataStatus.setText(R.string.assets_loading);
        }
    }

    private String selectedFilterValue(MaterialAutoCompleteTextView view) {
        String value = normalizeCurrentValue(view);
        return ALL_OPTION.equalsIgnoreCase(value) ? "" : value;
    }

    private String normalizeCurrentValue(MaterialAutoCompleteTextView view) {
        if (view == actAssetDepartmentFilter) {
            return AssetFieldNormalizer.normalizeDepartmentForDisplay(textOf(view));
        }
        if (view == actAssetLocationFilter) {
            return AssetLocationUtils.normalizeLocationForDisplay(textOf(view));
        }
        return textOf(view);
    }

    private String textOf(TextView view) {
        return view == null || view.getText() == null ? "" : view.getText().toString().trim();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
