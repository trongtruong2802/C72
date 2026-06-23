package com.idocean.asset.ui.inventory;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.InventorySessionItem;

import java.util.List;

final class InventoryUiRenderer {
    private final InventoryResultAdapter adapter;
    private final RecyclerView rvInventoryResults;
    private final TextView tvInventoryEmpty;
    private final TextView tvSummaryTotal;
    private final TextView tvSummaryDatasetTotal;
    private final TextView tvSummaryChecked;
    private final TextView tvScannerStatus;
    private final Button btnInventoryStart;

    InventoryUiRenderer(InventoryResultAdapter adapter,
                        RecyclerView rvInventoryResults,
                        TextView tvInventoryEmpty,
                        TextView tvSummaryTotal,
                        TextView tvSummaryDatasetTotal,
                        TextView tvSummaryChecked,
                        TextView tvScannerStatus,
                        Button btnInventoryStart) {
        this.adapter = adapter;
        this.rvInventoryResults = rvInventoryResults;
        this.tvInventoryEmpty = tvInventoryEmpty;
        this.tvSummaryTotal = tvSummaryTotal;
        this.tvSummaryDatasetTotal = tvSummaryDatasetTotal;
        this.tvSummaryChecked = tvSummaryChecked;
        this.tvScannerStatus = tvScannerStatus;
        this.btnInventoryStart = btnInventoryStart;
    }

    void renderResults(List<InventorySessionItem> items,
                       InventoryController.InventorySummary summary,
                       boolean scrollToTop,
                       String emptyMessage) {
        adapter.submitItems(items);
        if (summary != null) {
            tvSummaryTotal.setText(String.valueOf(summary.getScannedCount()));
            tvSummaryDatasetTotal.setText(String.valueOf(summary.getDatasetCount()));
            tvSummaryChecked.setText(String.valueOf(summary.getMatchedCount()));
        }
        boolean hasItems = items != null && !items.isEmpty();
        rvInventoryResults.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (tvInventoryEmpty != null) {
            tvInventoryEmpty.setVisibility(hasItems ? View.GONE : View.VISIBLE);
            tvInventoryEmpty.setText(emptyMessage == null ? "" : emptyMessage);
        }
        if (scrollToTop && hasItems) {
            rvInventoryResults.scrollToPosition(0);
        }
    }

    void renderScannerStatus(String message) {
        if (tvScannerStatus != null) {
            tvScannerStatus.setText(message == null ? "" : message);
        }
    }

    void renderPrimaryAction(boolean scannerRunning) {
        if (btnInventoryStart != null) {
            btnInventoryStart.setText(scannerRunning
                    ? R.string.inventory_stop_action
                    : R.string.inventory_start_action);
        }
    }
}
