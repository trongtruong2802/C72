package com.idocean.asset.data.repository;

import android.content.Context;

import com.idocean.asset.model.OperationLogEntry;
import com.idocean.asset.data.io.CsvWriter;
import com.idocean.asset.data.io.ExportFileManager;
import com.idocean.asset.utils.TimeFormatUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Luu log runtime trong bo nho de xem nhanh va export khi can.
 */
public class LogRepository {
    private static final int MAX_LOGS = 500;

    private static LogRepository instance;

    private final List<OperationLogEntry> logs = new ArrayList<>();
    private long nextId = 1L;

    private LogRepository() {
        addInternal("SYSTEM", "Khoi tao khung log runtime IDO Asset", "", false);
    }

    public static synchronized LogRepository getInstance() {
        if (instance == null) {
            instance = new LogRepository();
        }
        return instance;
    }

    public synchronized void logInfo(String action, String message) {
        addInternal(action, message, "", false);
    }

    public synchronized void logInfo(String action, String message, String detail) {
        addInternal(action, message, detail, false);
    }

    public synchronized void logError(String action, String message) {
        addInternal(action, message, "", true);
    }

    public synchronized void logError(String action, String message, String detail) {
        addInternal(action, message, detail, true);
    }

    public synchronized List<OperationLogEntry> getRecentLogs() {
        List<OperationLogEntry> copy = new ArrayList<>(logs);
        Collections.sort(copy, new Comparator<OperationLogEntry>() {
            @Override
            public int compare(OperationLogEntry left, OperationLogEntry right) {
                return Long.compare(right.getTimestamp(), left.getTimestamp());
            }
        });
        return copy;
    }

    public synchronized int size() {
        return logs.size();
    }

    public synchronized File exportCsv(Context context) throws IOException {
        File exportFile = ExportFileManager.resolveExportFile("logs_" + TimeFormatUtils.fileTimestamp() + ".csv");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(exportFile, false), StandardCharsets.UTF_8))) {
            writer.write("timestamp;action;level;message;detail");
            writer.newLine();
            for (OperationLogEntry entry : getRecentLogs()) {
                CsvWriter.writeQuotedLine(
                        writer,
                        ';',
                        TimeFormatUtils.displayTimestamp(entry.getTimestamp()),
                        entry.getAction(),
                        entry.isError() ? "ERROR" : "INFO",
                        entry.getMessage(),
                        entry.getDetail()
                );
            }
        }
        ExportFileManager.notifyMediaScanner(context, exportFile);
        addInternal("EXPORT", "Da export nhat ky thao tac", exportFile.getAbsolutePath(), false);
        return exportFile;
    }

    private void addInternal(String action, String message, String detail, boolean error) {
        logs.add(new OperationLogEntry(
                nextId++,
                System.currentTimeMillis(),
                safe(action, "SYSTEM"),
                safe(message, ""),
                safe(detail, ""),
                error
        ));
        while (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

}
