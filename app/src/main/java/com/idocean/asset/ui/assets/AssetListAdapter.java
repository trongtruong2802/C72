package com.idocean.asset.ui.assets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.Asset;

import java.util.ArrayList;
import java.util.List;

public class AssetListAdapter extends RecyclerView.Adapter<AssetListAdapter.ViewHolder> {
    public interface AssetClickListener {
        void onAssetClick(Asset asset);
    }

    private final List<Asset> items = new ArrayList<>();
    private final AssetClickListener assetClickListener;

    public AssetListAdapter(AssetClickListener assetClickListener) {
        this.assetClickListener = assetClickListener;
    }

    public void submitItems(List<Asset> assets) {
        items.clear();
        if (assets != null) {
            items.addAll(assets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asset_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asset asset = items.get(position);
        holder.tvName.setText((position + 1) + ". " + valueOrDash(asset.getAssetName()));
        holder.tvIdentity.setText(
                "Code: " + valueOrDash(asset.getAssetCode()) + " | TID: " + valueOrDash(asset.getTid())
        );
        holder.tvMeta.setText(
                "Phòng ban: " + valueOrDash(asset.getDepartment()) + " | Người dùng: " + valueOrDash(asset.getAssignedUser())
        );
        holder.tvOwner.setText(
                "Loại: " + valueOrDash(asset.getAssetType()) + " | Serial: " + valueOrDash(asset.getSerialNumber())
        );
        holder.tvLocation.setText("Vị trí: " + valueOrDash(asset.getLocation()));

        String status = valueOrDash(asset.getInventoryStatus());
        holder.tvStatus.setText(status);
        holder.tvStatus.setBackgroundResource(resolveStatusBackground(status));

        holder.itemView.setOnClickListener(v -> {
            if (assetClickListener != null) {
                assetClickListener.onAssetClick(asset);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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
