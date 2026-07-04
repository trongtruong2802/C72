package com.idocean.asset.data.io;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.idocean.asset.config.AppConfig;

import java.io.File;

/**
 * Quan ly duong dan file export va media scanner cho cac luong xuat file.
 */
public final class ExportFileManager {
    private ExportFileManager() {
    }

    public static File getExportDirectory() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File exportDir = new File(documentsDir, AppConfig.EXPORT_FOLDER_NAME);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        return exportDir;
    }

    public static File resolveExportFile(String fileName) {
        return new File(getExportDirectory(), sanitizeFileName(fileName));
    }

    public static String sanitizeFileNameSegment(String value) {
        String safe = safe(value);
        if (safe.isEmpty()) {
            return "khong-ro";
        }
        safe = safe
                .replaceAll("[\\\\/:*?\"<>|]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return safe.isEmpty() ? "khong-ro" : safe;
    }

    public static String sanitizeFileName(String value) {
        String safe = safe(value);
        if (safe.isEmpty()) {
            safe = com.idocean.asset.utils.TimeFormatUtils.fileTimestamp();
        }
        return safe.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    public static void notifyMediaScanner(Context context, File file) {
        if (context == null || file == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        context.sendBroadcast(intent);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
