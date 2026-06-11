package com.idocean.asset.ui.assets;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.AssetFilterCriteria;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.ui.lookup.LookupActivity;
import com.idocean.asset.utils.AssetLocationUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AssetsActivity extends AppCompatActivity {
    private static final String ALL_OPTION = "Tat ca";

    private final AssetRepository repository = AssetRepository.getInstance();
    private final AssetListAdapter assetListAdapter = new AssetListAdapter(this::openAssetDetail);

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

        etAssetSearch.addTextChangedListener(simpleWatcher(this::applyFilters));
        bindFilterChange(actAssetTypeFilter);
        bindFilterChange(actAssetDepartmentFilter);
        bindFilterChange(actAssetUserFilter);
        bindFilterChange(actAssetLocationFilter);
    }

    private void bindFilterChange(MaterialAutoCompleteTextView view) {
        view.setOnItemClickListener((parent, itemView, position, id) -> applyFilters());
        view.addTextChangedListener(simpleWatcher(this::applyFilters));
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
                applyFilters();
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
        List<Asset> cachedAssets = snapshot == null ? new ArrayList<>() : snapshot.getAssets();
        if (cachedAssets.isEmpty()) {
            tvDataStatus.setText(R.string.assets_cache_empty);
            assetListAdapter.submitItems(new ArrayList<>());
            populateFilterOptions(snapshot);
            applyFilters();
            return;
        }
        String source = snapshot == null ? "CACHE" : snapshot.getSource();
        bindAssets(getString(R.string.assets_cache_loaded, cachedAssets.size(), source), snapshot);
    }

    private void bindAssets(String message, AssetRepository.CacheSnapshot snapshot) {
        tvDataStatus.setText(message);
        populateFilterOptions(snapshot);
        applyFilters();
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
        applyFilters();
    }

    private void toggleFilters() {
        setFilterPanelVisible(cardAssetsFilters.getVisibility() != View.VISIBLE);
    }

    private void setFilterPanelVisible(boolean visible) {
        cardAssetsFilters.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnToggleFilters.setSelected(visible);
        btnToggleFilters.setAlpha(visible ? 1f : 0.85f);
    }

    private void applyFilters() {
        List<Asset> filteredAssets = repository.filterAssets(new AssetFilterCriteria(
                textOf(etAssetSearch),
                "",
                selectedFilterValue(actAssetTypeFilter),
                selectedFilterValue(actAssetDepartmentFilter),
                selectedFilterValue(actAssetUserFilter),
                selectedFilterValue(actAssetLocationFilter)
        ));
        assetListAdapter.submitItems(filteredAssets);
        tvAssetResultCount.setText(getString(R.string.assets_result_count, filteredAssets.size()));
        updateSummary(filteredAssets.size());
        updateEmptyState(filteredAssets.isEmpty());
    }

    private void updateSummary(int visibleCount) {
        int totalCount = repository.getCachedAssets().size();
        tvAssetsSummaryTotal.setText(String.valueOf(totalCount));
        tvAssetsSummaryVisible.setText(String.valueOf(visibleCount));
        tvAssetsSummaryFilters.setText(String.valueOf(countActiveFilters()));
    }

    private int countActiveFilters() {
        int count = 0;
        if (!textOf(etAssetSearch).isEmpty()) {
            count++;
        }
        if (!selectedFilterValue(actAssetTypeFilter).isEmpty()) {
            count++;
        }
        if (!selectedFilterValue(actAssetDepartmentFilter).isEmpty()) {
            count++;
        }
        if (!selectedFilterValue(actAssetUserFilter).isEmpty()) {
            count++;
        }
        if (!selectedFilterValue(actAssetLocationFilter).isEmpty()) {
            count++;
        }
        return count;
    }

    private void updateEmptyState(boolean empty) {
        boolean hasCache = !repository.getCachedAssets().isEmpty();
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
