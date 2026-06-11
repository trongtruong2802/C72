package com.idocean.asset.ui.checkout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.google.android.material.button.MaterialButton;
import com.idocean.asset.model.CheckoutAssetItem;
import com.idocean.asset.utils.TimeFormatUtils;

import java.util.ArrayList;
import java.util.List;

public class CheckoutAssetAdapter extends RecyclerView.Adapter<CheckoutAssetAdapter.ViewHolder> {

    public interface OnRemoveClickListener {
        void onRemoveClicked(CheckoutAssetItem item);
    }

    private final List<CheckoutAssetItem> items = new ArrayList<>();
    private final OnRemoveClickListener onRemoveClickListener;

    public CheckoutAssetAdapter(OnRemoveClickListener onRemoveClickListener) {
        this.onRemoveClickListener = onRemoveClickListener;
    }

    public void submitItems(List<CheckoutAssetItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkout_asset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckoutAssetItem item = items.get(position);
        holder.tvIndex.setText(String.format("%d.", position + 1));
        holder.tvName.setText(valueOrDash(item.getAssetName()));
        holder.tvCode.setText("Code: " + valueOrDash(item.getCode()));
        holder.tvTid.setText("TID: " + valueOrDash(item.getTid()));
        holder.tvMeta.setText(holder.itemView.getContext().getString(
                R.string.checkout_item_meta,
                valueOrDash(item.getAssetType()),
                valueOrDash(item.getSerialNumber())
        ));
        holder.tvOwner.setText(holder.itemView.getContext().getString(
                R.string.checkout_item_owner,
                valueOrDash(item.getAssignedUser()),
                valueOrDash(item.getDepartment()),
                valueOrDash(item.getLocation())
        ));
        holder.tvSource.setText(holder.itemView.getContext().getString(
                R.string.checkout_item_source,
                valueOrDash(item.getScanSource()),
                item.isMatchedFromCache()
                        ? holder.itemView.getContext().getString(R.string.common_yes)
                        : holder.itemView.getContext().getString(R.string.common_no)
        ));
        holder.tvScannedAt.setText(holder.itemView.getContext().getString(
                R.string.checkout_item_scanned_at,
                item.getScannedAt() > 0L
                        ? TimeFormatUtils.displayTimestamp(item.getScannedAt())
                        : holder.itemView.getContext().getString(R.string.common_unknown_value)
        ));
        holder.btnRemove.setOnClickListener(v -> {
            if (onRemoveClickListener != null) {
                onRemoveClickListener.onRemoveClicked(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIndex;
        final TextView tvName;
        final TextView tvCode;
        final TextView tvTid;
        final TextView tvMeta;
        final TextView tvOwner;
        final TextView tvSource;
        final TextView tvScannedAt;
        final MaterialButton btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvCheckoutItemIndex);
            tvName = itemView.findViewById(R.id.tvCheckoutItemName);
            tvCode = itemView.findViewById(R.id.tvCheckoutItemCode);
            tvTid = itemView.findViewById(R.id.tvCheckoutItemTid);
            tvMeta = itemView.findViewById(R.id.tvCheckoutItemMeta);
            tvOwner = itemView.findViewById(R.id.tvCheckoutItemOwner);
            tvSource = itemView.findViewById(R.id.tvCheckoutItemSource);
            tvScannedAt = itemView.findViewById(R.id.tvCheckoutItemScannedAt);
            btnRemove = itemView.findViewById(R.id.btnCheckoutItemRemove);
        }
    }
}
