package com.idocean.asset.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Helper format thời gian để hiển thị và export.
 */
public final class TimeFormatUtils {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.getDefault());
    private static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    private TimeFormatUtils() {
    }

    public static String formatDuration(long elapsedMillis) {
        return String.format(Locale.getDefault(), "%.1f giây", elapsedMillis / 1000f);
    }

    public static String fileTimestamp() {
        return LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
    }

    public static String displayTimestamp(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                .format(DISPLAY_TIMESTAMP_FORMATTER);
    }
}
