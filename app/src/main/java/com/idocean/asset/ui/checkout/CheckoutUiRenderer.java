package com.idocean.asset.ui.checkout;

import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.idocean.asset.R;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckoutAssetItem;

import java.util.List;

final class CheckoutUiRenderer {
    static final class ButtonsState {
        final boolean checkoutScanEnabled;
        final boolean checkoutStopEnabled;
        final boolean checkoutClearEnabled;
        final boolean checkoutExportEnabled;
        final boolean checkinScanEnabled;
        final boolean checkinStopEnabled;
        final boolean checkinClearEnabled;
        final boolean checkinExportEnabled;

        ButtonsState(boolean checkoutScanEnabled,
                     boolean checkoutStopEnabled,
                     boolean checkoutClearEnabled,
                     boolean checkoutExportEnabled,
                     boolean checkinScanEnabled,
                     boolean checkinStopEnabled,
                     boolean checkinClearEnabled,
                     boolean checkinExportEnabled) {
            this.checkoutScanEnabled = checkoutScanEnabled;
            this.checkoutStopEnabled = checkoutStopEnabled;
            this.checkoutClearEnabled = checkoutClearEnabled;
            this.checkoutExportEnabled = checkoutExportEnabled;
            this.checkinScanEnabled = checkinScanEnabled;
            this.checkinStopEnabled = checkinStopEnabled;
            this.checkinClearEnabled = checkinClearEnabled;
            this.checkinExportEnabled = checkinExportEnabled;
        }
    }

    private final CheckoutAssetAdapter checkoutAdapter;
    private final CheckInResultAdapter checkinAdapter;
    private final TextView tvCheckoutCount;
    private final TextView tvCheckoutSummarySelected;
    private final TextView tvCheckoutSummaryCached;
    private final TextView tvCheckoutSummaryOutsideCache;
    private final TextView tvCheckinSummaryHeadline;
    private final TextView tvCheckinSummaryDetail;
    private final TextView tvCheckinSummaryTotal;
    private final TextView tvCheckinSummaryReturned;
    private final TextView tvCheckinSummaryMissing;
    private final LinearProgressIndicator progressCheckinSummary;
    private final TextView tvCheckoutScannerStatus;
    private final TextView tvCheckinScannerStatus;
    private final Button btnCheckoutScan;
    private final Button btnCheckoutStop;
    private final Button btnCheckoutClear;
    private final Button btnCheckoutExport;
    private final Button btnCheckinScan;
    private final Button btnCheckinStop;
    private final Button btnCheckinClear;
    private final Button btnCheckinExport;

    CheckoutUiRenderer(CheckoutAssetAdapter checkoutAdapter,
                       CheckInResultAdapter checkinAdapter,
                       TextView tvCheckoutCount,
                       TextView tvCheckoutSummarySelected,
                       TextView tvCheckoutSummaryCached,
                       TextView tvCheckoutSummaryOutsideCache,
                       TextView tvCheckinSummaryHeadline,
                       TextView tvCheckinSummaryDetail,
                       TextView tvCheckinSummaryTotal,
                       TextView tvCheckinSummaryReturned,
                       TextView tvCheckinSummaryMissing,
                       LinearProgressIndicator progressCheckinSummary,
                       TextView tvCheckoutScannerStatus,
                       TextView tvCheckinScannerStatus,
                       Button btnCheckoutScan,
                       Button btnCheckoutStop,
                       Button btnCheckoutClear,
                       Button btnCheckoutExport,
                       Button btnCheckinScan,
                       Button btnCheckinStop,
                       Button btnCheckinClear,
                       Button btnCheckinExport) {
        this.checkoutAdapter = checkoutAdapter;
        this.checkinAdapter = checkinAdapter;
        this.tvCheckoutCount = tvCheckoutCount;
        this.tvCheckoutSummarySelected = tvCheckoutSummarySelected;
        this.tvCheckoutSummaryCached = tvCheckoutSummaryCached;
        this.tvCheckoutSummaryOutsideCache = tvCheckoutSummaryOutsideCache;
        this.tvCheckinSummaryHeadline = tvCheckinSummaryHeadline;
        this.tvCheckinSummaryDetail = tvCheckinSummaryDetail;
        this.tvCheckinSummaryTotal = tvCheckinSummaryTotal;
        this.tvCheckinSummaryReturned = tvCheckinSummaryReturned;
        this.tvCheckinSummaryMissing = tvCheckinSummaryMissing;
        this.progressCheckinSummary = progressCheckinSummary;
        this.tvCheckoutScannerStatus = tvCheckoutScannerStatus;
        this.tvCheckinScannerStatus = tvCheckinScannerStatus;
        this.btnCheckoutScan = btnCheckoutScan;
        this.btnCheckoutStop = btnCheckoutStop;
        this.btnCheckoutClear = btnCheckoutClear;
        this.btnCheckoutExport = btnCheckoutExport;
        this.btnCheckinScan = btnCheckinScan;
        this.btnCheckinStop = btnCheckinStop;
        this.btnCheckinClear = btnCheckinClear;
        this.btnCheckinExport = btnCheckinExport;
    }

    void renderCheckoutList(List<CheckoutAssetItem> orderedItems, CheckoutController.CheckoutSummary summary, String countLabel) {
        checkoutAdapter.submitItems(orderedItems);
        tvCheckoutCount.setText(countLabel == null ? "" : countLabel);
        if (summary != null) {
            tvCheckoutSummarySelected.setText(String.valueOf(summary.getSelectedCount()));
            tvCheckoutSummaryCached.setText(String.valueOf(summary.getCachedCount()));
            tvCheckoutSummaryOutsideCache.setText(String.valueOf(summary.getOutsideCacheCount()));
        }
    }

    void renderCheckinList(List<CheckInResultItem> orderedItems,
                           CheckoutController.CheckinSummary summary,
                           String headline,
                           String detail,
                           int completionPercent) {
        checkinAdapter.submitItems(orderedItems);
        if (summary != null) {
            tvCheckinSummaryTotal.setText(String.valueOf(summary.getTotalCount()));
            tvCheckinSummaryReturned.setText(String.valueOf(summary.getReturnedCount()));
            tvCheckinSummaryMissing.setText(String.valueOf(summary.getMissingCount()));
        }
        tvCheckinSummaryHeadline.setText(headline == null ? "" : headline);
        tvCheckinSummaryDetail.setText(detail == null ? "" : detail);
        progressCheckinSummary.setProgressCompat(completionPercent, false);
    }

    void renderScannerStatuses(String checkoutStatus, String checkinStatus) {
        tvCheckoutScannerStatus.setText(checkoutStatus == null ? "" : checkoutStatus);
        tvCheckinScannerStatus.setText(checkinStatus == null ? "" : checkinStatus);
    }

    void renderButtons(ButtonsState state) {
        if (state == null) {
            return;
        }
        btnCheckoutScan.setEnabled(state.checkoutScanEnabled);
        btnCheckoutStop.setEnabled(state.checkoutStopEnabled);
        btnCheckoutClear.setEnabled(state.checkoutClearEnabled);
        btnCheckoutExport.setEnabled(state.checkoutExportEnabled);
        btnCheckinScan.setEnabled(state.checkinScanEnabled);
        btnCheckinStop.setEnabled(state.checkinStopEnabled);
        btnCheckinClear.setEnabled(state.checkinClearEnabled);
        btnCheckinExport.setEnabled(state.checkinExportEnabled);
    }

    String emptyCheckinHeadline(TextView sourceView) {
        return sourceView.getContext().getString(R.string.checkin_summary_headline, 0, 0);
    }
}
