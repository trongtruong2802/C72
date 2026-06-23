package com.idocean.asset.ui.checkout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckInResultStatus;
import com.idocean.asset.utils.TimeFormatUtils;

import java.util.ArrayList;
import java.util.List;

public class CheckInResultAdapter extends ListAdapter<CheckInResultAdapter.CheckInRow, CheckInResultAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<CheckInRow> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CheckInRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull CheckInRow oldItem, @NonNull CheckInRow newItem) {
                    return sameValue(oldItem.identityKey, newItem.identityKey);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CheckInRow oldItem, @NonNull CheckInRow newItem) {
                    return oldItem.hasSameContent(newItem);
                }
            };

    public CheckInResultAdapter() {
        super(DIFF_CALLBACK);
        setHasStableIds(true);
    }

    public void submitItems(List<CheckInResultItem> newItems) {
        List<CheckInRow> rows = new ArrayList<>();
        if (newItems != null) {
            for (CheckInResultItem item : newItems) {
                rows.add(CheckInRow.from(item));
            }
        }
        super.submitList(rows);
    }

    @Override
    public long getItemId(int position) {
        CheckInRow row = getItem(position);
        return row == null ? RecyclerView.NO_ID : row.identityKey.hashCode();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_checkin_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckInRow item = getItem(position);
        holder.tvIndex.setText(String.format("%d.", position + 1));
        holder.tvName.setText(valueOrDash(item.assetName));
        holder.tvCode.setText("Code: " + valueOrDash(item.code));
        holder.tvTid.setText("TID: " + valueOrDash(item.tid));
        holder.tvExpected.setText(holder.itemView.getContext().getString(
                R.string.checkin_item_expected,
                item.expectedInTicket
                        ? holder.itemView.getContext().getString(R.string.common_yes)
                        : holder.itemView.getContext().getString(R.string.common_no),
                valueOrDash(item.matchedBy)
        ));
        holder.tvMeta.setText(holder.itemView.getContext().getString(
                R.string.checkin_item_meta,
                valueOrDash(item.assetType),
                valueOrDash(item.serialNumber)
        ));
        holder.tvOwner.setText(holder.itemView.getContext().getString(
                R.string.checkin_item_owner,
                valueOrDash(item.assignedUser),
                valueOrDash(item.department),
                valueOrDash(item.location)
        ));
        holder.tvScan.setText(buildScanLabel(holder, item));
        bindStatus(holder, item.status);
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

    private String buildScanLabel(ViewHolder holder, CheckInRow item) {
        String checkoutScan = item.checkoutScannedAt > 0L
                ? valueOrDash(item.checkoutScanSource) + " " + TimeFormatUtils.displayTimestamp(item.checkoutScannedAt)
                : holder.itemView.getContext().getString(R.string.common_unknown_value);
        String checkinScan = item.checkinScannedAt > 0L
                ? valueOrDash(item.checkinScanSource) + " " + TimeFormatUtils.displayTimestamp(item.checkinScannedAt)
                : holder.itemView.getContext().getString(R.string.checkin_item_scan_empty);
        return holder.itemView.getContext().getString(R.string.checkin_item_scan, checkoutScan, checkinScan);
    }

    private static boolean sameValue(String left, String right) {
        return left == null ? right == null : left.equals(right);
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

    static final class CheckInRow {
        final String identityKey;
        final boolean expectedInTicket;
        final String tid;
        final String code;
        final String assetName;
        final String assetType;
        final String serialNumber;
        final String department;
        final String assignedUser;
        final String location;
        final String matchedBy;
        final String checkoutScanSource;
        final long checkoutScannedAt;
        final String checkinScanSource;
        final long checkinScannedAt;
        final CheckInResultStatus status;

        private CheckInRow(String identityKey,
                           boolean expectedInTicket,
                           String tid,
                           String code,
                           String assetName,
                           String assetType,
                           String serialNumber,
                           String department,
                           String assignedUser,
                           String location,
                           String matchedBy,
                           String checkoutScanSource,
                           long checkoutScannedAt,
                           String checkinScanSource,
                           long checkinScannedAt,
                           CheckInResultStatus status) {
            this.identityKey = identityKey == null ? "" : identityKey;
            this.expectedInTicket = expectedInTicket;
            this.tid = tid == null ? "" : tid;
            this.code = code == null ? "" : code;
            this.assetName = assetName == null ? "" : assetName;
            this.assetType = assetType == null ? "" : assetType;
            this.serialNumber = serialNumber == null ? "" : serialNumber;
            this.department = department == null ? "" : department;
            this.assignedUser = assignedUser == null ? "" : assignedUser;
            this.location = location == null ? "" : location;
            this.matchedBy = matchedBy == null ? "" : matchedBy;
            this.checkoutScanSource = checkoutScanSource == null ? "" : checkoutScanSource;
            this.checkoutScannedAt = checkoutScannedAt;
            this.checkinScanSource = checkinScanSource == null ? "" : checkinScanSource;
            this.checkinScannedAt = checkinScannedAt;
            this.status = status == null ? CheckInResultStatus.MISSING : status;
        }

        static CheckInRow from(CheckInResultItem item) {
            if (item == null) {
                return new CheckInRow("", false, "", "", "", "", "", "", "", "", "", "", 0L, "", 0L, CheckInResultStatus.MISSING);
            }
            return new CheckInRow(
                    item.getIdentityKey(),
                    item.isExpectedInTicket(),
                    item.getTid(),
                    item.getCode(),
                    item.getAssetName(),
                    item.getAssetType(),
                    item.getSerialNumber(),
                    item.getDepartment(),
                    item.getAssignedUser(),
                    item.getLocation(),
                    item.getMatchedBy(),
                    item.getCheckoutScanSource(),
                    item.getCheckoutScannedAt(),
                    item.getCheckinScanSource(),
                    item.getCheckinScannedAt(),
                    item.getStatus()
            );
        }

        boolean hasSameContent(CheckInRow other) {
            return other != null
                    && expectedInTicket == other.expectedInTicket
                    && checkoutScannedAt == other.checkoutScannedAt
                    && checkinScannedAt == other.checkinScannedAt
                    && status == other.status
                    && sameValue(tid, other.tid)
                    && sameValue(code, other.code)
                    && sameValue(assetName, other.assetName)
                    && sameValue(assetType, other.assetType)
                    && sameValue(serialNumber, other.serialNumber)
                    && sameValue(department, other.department)
                    && sameValue(assignedUser, other.assignedUser)
                    && sameValue(location, other.location)
                    && sameValue(matchedBy, other.matchedBy)
                    && sameValue(checkoutScanSource, other.checkoutScanSource)
                    && sameValue(checkinScanSource, other.checkinScanSource);
        }
    }
}
