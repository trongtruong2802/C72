package com.idocean.asset.ui.logs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.idocean.asset.R;
import com.idocean.asset.model.OperationLogEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OperationLogAdapter extends RecyclerView.Adapter<OperationLogAdapter.OperationLogViewHolder> {

    private final List<OperationLogEntry> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US);

    public void submitItems(List<OperationLogEntry> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OperationLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_operation_log, parent, false);
        return new OperationLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OperationLogViewHolder holder, int position) {
        OperationLogEntry item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvLogAction.setText(item.isError()
                ? context.getString(R.string.logs_level_error)
                : context.getString(R.string.logs_level_info));
        holder.tvLogAction.setBackgroundResource(item.isError()
                ? R.drawable.bg_dashboard_chip_empty
                : R.drawable.bg_dashboard_chip_ready);
        holder.tvLogAction.setTextColor(ContextCompat.getColor(
                context,
                item.isError() ? R.color.dashboard_chip_empty_text : R.color.dashboard_chip_ready_text
        ));
        holder.tvLogTime.setText(formatTimestamp(item.getTimestamp()));
        holder.tvLogMessage.setText(valueOrFallback(item.getMessage(), context.getString(R.string.logs_detail_empty)));
        holder.tvLogDetail.setText(valueOrFallback(item.getDetail(), context.getString(R.string.logs_detail_empty)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatTimestamp(long timestamp) {
        if (timestamp <= 0L) {
            return "-";
        }
        return timeFormat.format(new Date(timestamp));
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    static final class OperationLogViewHolder extends RecyclerView.ViewHolder {
        final TextView tvLogAction;
        final TextView tvLogTime;
        final TextView tvLogMessage;
        final TextView tvLogDetail;

        OperationLogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLogAction = itemView.findViewById(R.id.tvLogAction);
            tvLogTime = itemView.findViewById(R.id.tvLogTime);
            tvLogMessage = itemView.findViewById(R.id.tvLogMessage);
            tvLogDetail = itemView.findViewById(R.id.tvLogDetail);
        }
    }
}
