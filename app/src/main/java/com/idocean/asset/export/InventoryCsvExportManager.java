package com.idocean.asset.export;

import android.content.Context;
import android.util.Log;

import com.idocean.asset.data.repository.SessionRepository;
import com.idocean.asset.model.InventorySessionItem;
import com.idocean.asset.storage.CsvWriter;
import com.idocean.asset.storage.ExportFileManager;
import com.idocean.asset.utils.TimeFormatUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Export kết quả kiểm kê ra CSV thật.
 */
public class InventoryCsvExportManager {
    private static final SimpleDateFormat INVENTORY_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public File export(Context context, List<InventorySessionItem> items) throws IOException {
        File exportFile = ExportFileManager.resolveExportFile(buildInventoryExportFileName(context, items));
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(exportFile, false), StandardCharsets.UTF_8))) {
            CsvWriter.writeUtf8Bom(writer);
            CsvWriter.writeExcelSeparatorHint(writer, ',');
            writer.write("code,tid,epc_hex,scan_source,scanned_at,inventory_status,asset_name,user,department,location,asset_type,serial,operator,note");
            writer.newLine();
            for (InventorySessionItem item : items) {
                Log.d("EXPORT", "[EXPORT] row tid=" + item.getDisplayTid());
                CsvWriter.writeQuotedLine(
                        writer,
                        ',',
                        item.getDisplayCode(),
                        item.getDisplayTid(),
                        item.getDisplayEpcHex(),
                        item.getScanSource().getLabel(),
                        item.getScannedAt() > 0 ? TimeFormatUtils.displayTimestamp(item.getScannedAt()) : "",
                        item.getStatus().getLabel(),
                        item.getAssetName(),
                        item.getAssignedUser(),
                        item.getDepartment(),
                        item.getLocation(),
                        item.getAssetType(),
                        item.getSerialNumber(),
                        item.getOperatorName(),
                        item.getInventoryNote()
                );
            }
        }
        Log.d("EXPORT", "[EXPORT] success file=" + exportFile.getAbsolutePath());
        ExportFileManager.notifyMediaScanner(context, exportFile);
        return exportFile;
    }

    private String buildInventoryExportFileName(Context context, List<InventorySessionItem> items) {
        com.idocean.asset.model.SessionConfig sessionConfig =
                new SessionRepository(context.getApplicationContext()).getSession();
        String[] segments = new String[]{
                "IDO_INVENTORY",
                ExportFileManager.sanitizeFileNameSegment(sessionConfig == null ? "" : sessionConfig.getDepartment()),
                ExportFileManager.sanitizeFileNameSegment(sessionConfig == null ? "" : sessionConfig.getOperatorName()),
                ExportFileManager.sanitizeFileNameSegment(resolveInventoryDate(items))
        };
        return String.join("_", segments) + ".csv";
    }

    private String resolveInventoryDate(List<InventorySessionItem> items) {
        long firstScannedAt = 0L;
        if (items != null) {
            for (InventorySessionItem item : items) {
                if (item == null || item.getScannedAt() <= 0L) {
                    continue;
                }
                if (firstScannedAt <= 0L || item.getScannedAt() < firstScannedAt) {
                    firstScannedAt = item.getScannedAt();
                }
            }
        }
        Date exportDate = firstScannedAt > 0L ? new Date(firstScannedAt) : new Date();
        synchronized (INVENTORY_DATE_FORMAT) {
            return INVENTORY_DATE_FORMAT.format(exportDate);
        }
    }
}
