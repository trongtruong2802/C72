package com.idocean.asset.ui.inventory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.InventoryItemStatus;
import com.idocean.asset.model.InventorySessionItem;

import java.util.ArrayList;
import java.util.List;

public class InventoryResultAdapter extends RecyclerView.Adapter<InventoryResultAdapter.InventoryViewHolder> {

    private final List<InventorySessionItem> items = new ArrayList<>();

    public void submitList(List<InventorySessionItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_result, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventorySessionItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvItemName.setText((position + 1) + ". " + valueOrDash(item.getAssetName()));
        holder.tvItemIdentity.setText(
                context.getString(R.string.inventory_field_code)
                        + ": " + valueOrDash(item.getDisplayCode())
                        + " | "
                        + context.getString(R.string.inventory_field_tid)
                        + ": " + valueOrDash(item.getDisplayTid())
        );
        holder.tvItemMeta.setText(
                context.getString(R.string.inventory_field_department)
                        + ": " + valueOrDash(item.getDepartment())
                        + " | "
                        + context.getString(R.string.inventory_field_user)
                        + ": " + valueOrDash(item.getAssignedUser())
        );
        holder.tvItemOwner.setText(
                context.getString(R.string.inventory_field_type)
                        + ": " + valueOrDash(item.getAssetType())
                        + " | "
                        + context.getString(R.string.inventory_field_serial)
                        + ": " + valueOrDash(item.getSerialNumber())
        );
        holder.tvItemLocation.setText(
                context.getString(R.string.inventory_field_location) + ": " + valueOrDash(item.getLocation())
        );

        DisplayBadge badge = resolveBadge(context, item);
        holder.tvItemBadge.setText(badge.label);
        holder.tvItemBadge.setBackgroundResource(badge.backgroundRes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private DisplayBadge resolveBadge(Context context, InventorySessionItem item) {
        if (item.getStatus() == InventoryItemStatus.OUTSIDE) {
            return new DisplayBadge(
                    context.getString(R.string.inventory_badge_outside),
                    R.drawable.bg_inventory_status_outside
            );
        }
        if (item.getStatus() == InventoryItemStatus.CHECKED) {
            return new DisplayBadge(
                    context.getString(R.string.inventory_badge_matched),
                    R.drawable.bg_inventory_status_checked
            );
        }
        return new DisplayBadge(
                context.getString(R.string.inventory_badge_missing),
                R.drawable.bg_inventory_status_missing
        );
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    static final class InventoryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvItemName;
        final TextView tvItemIdentity;
        final TextView tvItemMeta;
        final TextView tvItemOwner;
        final TextView tvItemLocation;
        final TextView tvItemBadge;

        InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemIdentity = itemView.findViewById(R.id.tvItemIdentity);
            tvItemMeta = itemView.findViewById(R.id.tvItemMeta);
            tvItemOwner = itemView.findViewById(R.id.tvItemOwner);
            tvItemLocation = itemView.findViewById(R.id.tvItemLocation);
            tvItemBadge = itemView.findViewById(R.id.tvItemBadge);
        }
    }

    private static final class DisplayBadge {
        final String label;
        final int backgroundRes;

        DisplayBadge(String label, int backgroundRes) {
            this.label = label;
            this.backgroundRes = backgroundRes;
        }
    }
}
