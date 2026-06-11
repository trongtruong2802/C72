package com.idocean.asset.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper format thời gian để hiển thị và export.
 */
public final class TimeFormatUtils {

    private TimeFormatUtils() {
    }

    public static String formatDuration(long elapsedMillis) {
        return String.format(Locale.getDefault(), "%.1f giây", elapsedMillis / 1000f);
    }

    public static String fileTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    public static String displayTimestamp(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(millis));
    }
}
