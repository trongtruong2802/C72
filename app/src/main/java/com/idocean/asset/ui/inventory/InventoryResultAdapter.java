package com.idocean.asset.ui.inventory;

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
import com.idocean.asset.model.InventoryItemStatus;
import com.idocean.asset.model.InventorySessionItem;

import java.util.ArrayList;
import java.util.List;

public class InventoryResultAdapter extends ListAdapter<InventoryResultAdapter.InventoryRow, InventoryResultAdapter.InventoryViewHolder> {

    private static final DiffUtil.ItemCallback<InventoryRow> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<InventoryRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull InventoryRow oldItem, @NonNull InventoryRow newItem) {
                    return sameValue(oldItem.itemKey, newItem.itemKey);
                }

                @Override
                public boolean areContentsTheSame(@NonNull InventoryRow oldItem, @NonNull InventoryRow newItem) {
                    return oldItem.hasSameContent(newItem);
                }
            };

    public InventoryResultAdapter() {
        super(DIFF_CALLBACK);
        setHasStableIds(true);
    }

    public void submitItems(List<InventorySessionItem> newItems) {
        List<InventoryRow> rows = new ArrayList<>();
        if (newItems != null) {
            for (InventorySessionItem item : newItems) {
                rows.add(InventoryRow.from(item));
            }
        }
        super.submitList(rows);
    }

    @Override
    public long getItemId(int position) {
        InventoryRow row = getItem(position);
        return row == null ? RecyclerView.NO_ID : row.itemKey.hashCode();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_result, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryRow item = getItem(position);
        Context context = holder.itemView.getContext();

        holder.tvItemName.setText((position + 1) + ". " + valueOrDash(item.assetName));
        holder.tvItemIdentity.setText(
                context.getString(R.string.inventory_field_code)
                        + ": " + valueOrDash(item.displayCode)
                        + " | "
                        + context.getString(R.string.inventory_field_tid)
                        + ": " + valueOrDash(item.displayTid)
        );
        holder.tvItemMeta.setText(
                context.getString(R.string.inventory_field_department)
                        + ": " + valueOrDash(item.department)
                        + " | "
                        + context.getString(R.string.inventory_field_user)
                        + ": " + valueOrDash(item.assignedUser)
        );
        holder.tvItemOwner.setText(
                context.getString(R.string.inventory_field_type)
                        + ": " + valueOrDash(item.assetType)
                        + " | "
                        + context.getString(R.string.inventory_field_serial)
                        + ": " + valueOrDash(item.serialNumber)
        );
        holder.tvItemLocation.setText(
                context.getString(R.string.inventory_field_location) + ": " + valueOrDash(item.location)
        );

        DisplayBadge badge = resolveBadge(context, item.status);
        holder.tvItemBadge.setText(badge.label);
        holder.tvItemBadge.setBackgroundResource(badge.backgroundRes);
    }

    private DisplayBadge resolveBadge(Context context, InventoryItemStatus status) {
        if (status == InventoryItemStatus.OUTSIDE) {
            return new DisplayBadge(
                    context.getString(R.string.inventory_badge_outside),
                    R.drawable.bg_inventory_status_outside
            );
        }
        if (status == InventoryItemStatus.CHECKED) {
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

    private static String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static boolean sameValue(String left, String right) {
        return left == null ? right == null : left.equals(right);
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

    static final class InventoryRow {
        final String itemKey;
        final InventoryItemStatus status;
        final String displayCode;
        final String displayTid;
        final String assetName;
        final String department;
        final String assignedUser;
        final String assetType;
        final String serialNumber;
        final String location;

        private InventoryRow(String itemKey,
                             InventoryItemStatus status,
                             String displayCode,
                             String displayTid,
                             String assetName,
                             String department,
                             String assignedUser,
                             String assetType,
                             String serialNumber,
                             String location) {
            this.itemKey = itemKey == null ? "" : itemKey;
            this.status = status == null ? InventoryItemStatus.MISSING : status;
            this.displayCode = displayCode == null ? "" : displayCode;
            this.displayTid = displayTid == null ? "" : displayTid;
            this.assetName = assetName == null ? "" : assetName;
            this.department = department == null ? "" : department;
            this.assignedUser = assignedUser == null ? "" : assignedUser;
            this.assetType = assetType == null ? "" : assetType;
            this.serialNumber = serialNumber == null ? "" : serialNumber;
            this.location = location == null ? "" : location;
        }

        static InventoryRow from(InventorySessionItem item) {
            if (item == null) {
                return new InventoryRow("", InventoryItemStatus.MISSING, "", "", "", "", "", "", "", "");
            }
            return new InventoryRow(
                    item.getItemKey(),
                    item.getStatus(),
                    item.getDisplayCode(),
                    item.getDisplayTid(),
                    item.getAssetName(),
                    item.getDepartment(),
                    item.getAssignedUser(),
                    item.getAssetType(),
                    item.getSerialNumber(),
                    item.getLocation()
            );
        }

        boolean hasSameContent(InventoryRow other) {
            return other != null
                    && status == other.status
                    && sameValue(displayCode, other.displayCode)
                    && sameValue(displayTid, other.displayTid)
                    && sameValue(assetName, other.assetName)
                    && sameValue(department, other.department)
                    && sameValue(assignedUser, other.assignedUser)
                    && sameValue(assetType, other.assetType)
                    && sameValue(serialNumber, other.serialNumber)
                    && sameValue(location, other.location);
        }
    }
}
