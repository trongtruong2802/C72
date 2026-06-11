package com.idocean.asset.ui.checkout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckInResultStatus;
import com.idocean.asset.utils.TimeFormatUtils;

import java.util.ArrayList;
import java.util.List;

public class CheckInResultAdapter extends RecyclerView.Adapter<CheckInResultAdapter.ViewHolder> {
    private final List<CheckInResultItem> items = new ArrayList<>();

    public void submitItems(List<CheckInResultItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkin_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckInResultItem item = items.get(position);
        holder.tvIndex.setText(String.format("%d.", position + 1));
        holder.tvName.setText(valueOrDash(item.getAssetName()));
        holder.tvCode.setText("Code: " + valueOrDash(item.getCode()));
        holder.tvTid.setText("TID: " + valueOrDash(item.getTid()));
        holder.tvExpected.setText(holder.itemView.getContext().getString(
                R.string.checkin_item_expected,
                item.isExpectedInTicket()
                        ? holder.itemView.getContext().getString(R.string.common_yes)
                        : holder.itemView.getContext().getString(R.string.common_no),
                valueOrDash(item.getMatchedBy())
        ));
        holder.tvMeta.setText(holder.itemView.getContext().getString(
                R.string.checkin_item_meta,
                valueOrDash(item.getAssetType()),
                valueOrDash(item.getSerialNumber())
        ));
        holder.tvOwner.setText(holder.itemView.getContext().getString(
                R.string.checkin_item_owner,
                valueOrDash(item.getAssignedUser()),
                valueOrDash(item.getDepartment()),
                valueOrDash(item.getLocation())
        ));
        holder.tvScan.setText(buildScanLabel(holder, item));
        bindStatus(holder, item.getStatus());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindStatus(ViewHolder holder, CheckInResultStatus status) {
        holder.tvStatus.setText(status.getLabel());
        if (status == CheckInResultStatus.RETURNED) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_inventory_status_checked);
        } else if (status == CheckInResultStatus.OUTSIDE) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_inventory_status_outside);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_inventory_status_missing);
        }
    }

    private String buildScanLabel(ViewHolder holder, CheckInResultItem item) {
        String checkoutScan = item.getCheckoutScannedAt() > 0L
                ? valueOrDash(item.getCheckoutScanSource()) + " " + TimeFormatUtils.displayTimestamp(item.getCheckoutScannedAt())
                : holder.itemView.getContext().getString(R.string.common_unknown_value);
        String checkinScan = item.getCheckinScannedAt() > 0L
                ? valueOrDash(item.getCheckinScanSource()) + " " + TimeFormatUtils.displayTimestamp(item.getCheckinScannedAt())
                : holder.itemView.getContext().getString(R.string.checkin_item_scan_empty);
        return holder.itemView.getContext().getString(R.string.checkin_item_scan, checkoutScan, checkinScan);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIndex;
        final TextView tvName;
        final TextView tvStatus;
        final TextView tvCode;
        final TextView tvTid;
        final TextView tvExpected;
        final TextView tvMeta;
        final TextView tvOwner;
        final TextView tvScan;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvCheckinItemIndex);
            tvName = itemView.findViewById(R.id.tvCheckinItemName);
            tvStatus = itemView.findViewById(R.id.tvCheckinItemStatus);
            tvCode = itemView.findViewById(R.id.tvCheckinItemCode);
            tvTid = itemView.findViewById(R.id.tvCheckinItemTid);
            tvExpected = itemView.findViewById(R.id.tvCheckinItemExpected);
            tvMeta = itemView.findViewById(R.id.tvCheckinItemMeta);
            tvOwner = itemView.findViewById(R.id.tvCheckinItemOwner);
            tvScan = itemView.findViewById(R.id.tvCheckinItemScan);
        }
    }
}
