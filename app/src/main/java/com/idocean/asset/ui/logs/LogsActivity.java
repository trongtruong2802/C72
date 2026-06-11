package com.idocean.asset.ui.logs;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.AppRuntimeContext;
import com.idocean.asset.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.model.OperationLogEntry;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogsActivity extends AppCompatActivity {
    private LogRepository logRepository;
    private OperationLogAdapter adapter;
    private TextView tvSummary;
    private final ExecutorService exportExecutor = Executors.newSingleThreadExecutor();
    private MaterialButton btnExport;
    private boolean exporting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppRuntimeContext.init(getApplicationContext());
        setContentView(R.layout.activity_ido_logs);

        logRepository = LogRepository.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarIdoLogs);
        toolbar.setTitle(R.string.logs_title_runtime);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSummary = findViewById(R.id.tvLogsSummary);
        btnExport = findViewById(R.id.btnExportLogs);
        RecyclerView rvLogs = findViewById(R.id.rvLogs);

        adapter = new OperationLogAdapter();
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(adapter);

        btnExport.setOnClickListener(v -> exportLogs());
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindLogs();
    }

    @Override
    protected void onDestroy() {
        exportExecutor.shutdownNow();
        super.onDestroy();
    }

    private void bindLogs() {
        List<OperationLogEntry> logs = logRepository.getRecentLogs();
        adapter.submitItems(logs);
        if (logs.isEmpty()) {
            tvSummary.setText(R.string.logs_empty_runtime);
        } else {
            tvSummary.setText(getString(R.string.logs_summary_count, logs.size()));
        }
    }

    private void exportLogs() {
        if (exporting) {
            return;
        }

        exporting = true;
        btnExport.setEnabled(false);
        tvSummary.setText(R.string.logs_export_running);
        exportExecutor.execute(() -> {
            try {
                File exportFile = logRepository.exportCsv(this);
                runOnUiThread(() -> {
                    exporting = false;
                    btnExport.setEnabled(true);
                    bindLogs();
                    String message = getString(R.string.logs_export_done, exportFile.getAbsolutePath());
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            } catch (IOException exception) {
                runOnUiThread(() -> {
                    exporting = false;
                    btnExport.setEnabled(true);
                    bindLogs();
                    Toast.makeText(this, R.string.logs_export_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
