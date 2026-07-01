package com.idocean.asset.ui.assets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.Asset;

import java.util.ArrayList;
import java.util.List;

public class AssetListAdapter extends ListAdapter<Asset, AssetListAdapter.ViewHolder> {
    private static final DiffUtil.ItemCallback<Asset> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Asset>() {
                @Override
                public boolean areItemsTheSame(@NonNull Asset oldItem, @NonNull Asset newItem) {
                    return stableKey(oldItem).equals(stableKey(newItem));
                }

                @Override
                public boolean areContentsTheSame(@NonNull Asset oldItem, @NonNull Asset newItem) {
                    return sameValue(oldItem.getAssetCode(), newItem.getAssetCode())
                            && sameValue(oldItem.getTid(), newItem.getTid())
                            && sameValue(oldItem.getAssetName(), newItem.getAssetName())
                            && sameValue(oldItem.getDepartment(), newItem.getDepartment())
                            && sameValue(oldItem.getAssignedUser(), newItem.getAssignedUser())
                            && sameValue(oldItem.getAssetType(), newItem.getAssetType())
                            && sameValue(oldItem.getSerialNumber(), newItem.getSerialNumber())
                            && sameValue(oldItem.getLocation(), newItem.getLocation())
                            && sameValue(oldItem.getInventoryStatus(), newItem.getInventoryStatus());
                }
            };

    public interface AssetClickListener {
        void onAssetClick(Asset asset);
    }

    private final AssetClickListener assetClickListener;

    public AssetListAdapter(AssetClickListener assetClickListener) {
        super(DIFF_CALLBACK);
        this.assetClickListener = assetClickListener;
        setHasStableIds(true);
    }

    public void submitItems(List<Asset> assets) {
        super.submitList(assets == null ? new ArrayList<>() : new ArrayList<>(assets));
    }

    @Override
    public long getItemId(int position) {
        Asset asset = getItem(position);
        return asset == null ? RecyclerView.NO_ID : stableKey(asset).hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asset asset = getItem(position);
        Context context = holder.itemView.getContext();
        holder.tvName.setText((position + 1) + ". " + valueOrDash(asset.getAssetName()));
        holder.tvIdentity.setText(
                context.getString(R.string.inventory_field_code)
                        + ": " + valueOrDash(asset.getAssetCode())
                        + " | "
                        + context.getString(R.string.inventory_field_tid)
                        + ": " + valueOrDash(asset.getTid())
        );
        holder.tvMeta.setText(
                context.getString(R.string.inventory_field_department)
                        + ": " + valueOrDash(asset.getDepartment())
                        + " | "
                        + context.getString(R.string.inventory_field_user)
                        + ": " + valueOrDash(asset.getAssignedUser())
        );
        holder.tvOwner.setText(
                context.getString(R.string.inventory_field_type)
                        + ": " + valueOrDash(asset.getAssetType())
                        + " | "
                        + context.getString(R.string.inventory_field_serial)
                        + ": " + valueOrDash(asset.getSerialNumber())
        );
        holder.tvLocation.setText(
                context.getString(R.string.inventory_field_location) + ": " + valueOrDash(asset.getLocation())
        );

        String status = asset.getInventoryStatus();
        if (status == null || status.trim().isEmpty()) {
            status = asset.getAssetCondition();
        }
        String displayStatus = valueOrDash(status);
        holder.tvStatus.setText(displayStatus);
        holder.tvStatus.setBackgroundResource(resolveStatusBackground(displayStatus));

        holder.itemView.setOnClickListener(v -> {
            if (assetClickListener != null) {
                assetClickListener.onAssetClick(asset);
            }
        });
    }

    private int resolveStatusBackground(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        if (normalized.contains("ngoai")) {
            return R.drawable.bg_inventory_status_outside;
        }
        if (normalized.contains("da") || normalized.contains("kiem")) {
            return R.drawable.bg_inventory_status_checked;
        }
        return R.drawable.bg_inventory_status_missing;
    }

    private static String stableKey(Asset asset) {
        if (asset == null) {
            return "";
        }
        if (asset.getRowNumber() != null) {
            return "row:" + asset.getRowNumber();
        }
        return valuePart(asset.getAssetCode()) + "|" + valuePart(asset.getTid()) + "|" + valuePart(asset.getSerialNumber());
    }

    private static String valuePart(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean sameValue(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvStatus;
        final TextView tvName;
        final TextView tvIdentity;
        final TextView tvMeta;
        final TextView tvOwner;
        final TextView tvLocation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvAssetItemStatus);
            tvName = itemView.findViewById(R.id.tvAssetItemName);
            tvIdentity = itemView.findViewById(R.id.tvAssetItemIdentity);
            tvMeta = itemView.findViewById(R.id.tvAssetItemMeta);
            tvOwner = itemView.findViewById(R.id.tvAssetItemOwner);
            tvLocation = itemView.findViewById(R.id.tvAssetItemLocation);
        }
    }
}
