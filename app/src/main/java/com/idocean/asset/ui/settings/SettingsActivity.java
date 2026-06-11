package com.idocean.asset.ui.settings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.SessionRepository;
import com.idocean.asset.model.SessionConfig;
import com.idocean.asset.utils.AssetFieldNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private final AssetRepository assetRepository = AssetRepository.getInstance();

    private SessionRepository sessionRepository;
    private EditText etOperatorName;
    private MaterialAutoCompleteTextView actDepartment;
    private EditText etSessionNote;
    private CheckBox cbManualEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        setContentView(R.layout.activity_ido_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarIdoSettings);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.session_title);
        toolbar.setNavigationOnClickListener(v -> finish());

        sessionRepository = new SessionRepository(this);

        etOperatorName = findViewById(R.id.etOperatorName);
        actDepartment = findViewById(R.id.actDepartment);
        etSessionNote = findViewById(R.id.etSessionNote);
        cbManualEntry = findViewById(R.id.cbManualEntry);
        Button btnSaveSession = findViewById(R.id.btnSaveSession);

        bindDepartmentOptions(sessionRepository.getSession().getDepartment());
        bindSession(sessionRepository.getSession());

        actDepartment.setOnClickListener(v -> actDepartment.showDropDown());
        actDepartment.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actDepartment.showDropDown();
            }
        });

        btnSaveSession.setOnClickListener(v -> saveSession());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindDepartmentOptions(sessionRepository.getSession().getDepartment());
        bindSession(sessionRepository.getSession());
    }

    private void bindSession(SessionConfig config) {
        etOperatorName.setText(config.getOperatorName());
        actDepartment.setText(AssetFieldNormalizer.normalizeDepartmentForDisplay(config.getDepartment()), false);
        etSessionNote.setText(config.getSessionNote());
        cbManualEntry.setChecked(config.isManualEntryEachSession());
    }

    private void saveSession() {
        SessionConfig config = new SessionConfig(
                textOf(etOperatorName),
                AssetFieldNormalizer.normalizeDepartmentForDisplay(textOf(actDepartment)),
                textOf(etSessionNote),
                cbManualEntry.isChecked()
        );
        sessionRepository.saveSession(config);
        Toast.makeText(this, R.string.session_saved, Toast.LENGTH_SHORT).show();
    }

    private String textOf(TextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }

    private void bindDepartmentOptions(String selectedDepartment) {
        Set<String> options = new LinkedHashSet<>();
        String[] defaults = getResources().getStringArray(R.array.known_department_options);
        for (String option : defaults) {
            String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(option);
            if (!normalized.isEmpty()) {
                options.add(normalized);
            }
        }

        List<String> runtimeDepartments = assetRepository.collectDistinctValues("department");
        if (runtimeDepartments != null) {
            for (String department : runtimeDepartments) {
                String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(department);
                if (!normalized.isEmpty()) {
                    options.add(normalized);
                }
            }
        }

        if (selectedDepartment != null && !selectedDepartment.trim().isEmpty()) {
            options.add(AssetFieldNormalizer.normalizeDepartmentForDisplay(selectedDepartment));
        }

        List<String> items = new ArrayList<>(options);
        actDepartment.setSimpleItems(items.toArray(new String[0]));
    }
}
