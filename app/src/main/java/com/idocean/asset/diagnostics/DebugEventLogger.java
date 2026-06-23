package com.idocean.asset.diagnostics;

import android.util.Log;

import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.utils.TimeFormatUtils;

public final class DebugEventLogger {
    private static final String TAG = "IDO_DEBUG";
    private static final String ACTION = "APP_DEBUG";

    private DebugEventLogger() {
    }

    public static void info(String screen, String flow, String event, String detail) {
        emit(null, false, screen, flow, event, "", detail, -1L);
    }

    public static void info(LogRepository logRepository, String screen, String flow, String event, String detail) {
        emit(logRepository, false, screen, flow, event, "", detail, -1L);
    }

    public static void duration(String screen, String flow, String event, String detail, long durationMs) {
        emit(null, false, screen, flow, event, "", detail, durationMs);
    }

    public static void duration(
            LogRepository logRepository,
            String screen,
            String flow,
            String event,
            String detail,
            long durationMs
    ) {
        emit(logRepository, false, screen, flow, event, "", detail, durationMs);
    }

    public static void error(String screen, String flow, String event, String errorCode, String detail) {
        emit(null, true, screen, flow, event, errorCode, detail, -1L);
    }

    public static void error(
            LogRepository logRepository,
            String screen,
            String flow,
            String event,
            String errorCode,
            String detail
    ) {
        emit(logRepository, true, screen, flow, event, errorCode, detail, -1L);
    }

    private static void emit(
            LogRepository logRepository,
            boolean error,
            String screen,
            String flow,
            String event,
            String errorCode,
            String detail,
            long durationMs
    ) {
        String payload = buildPayload(screen, flow, event, errorCode, detail, durationMs);
        if (error) {
            Log.e(TAG, payload);
            if (logRepository != null) {
                logRepository.logError(ACTION, safe(event), payload);
            }
            return;
        }

        Log.d(TAG, payload);
        if (logRepository != null) {
            logRepository.logInfo(ACTION, safe(event), payload);
        }
    }

    private static String buildPayload(
            String screen,
            String flow,
            String event,
            String errorCode,
            String detail,
            long durationMs
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("timestamp=").append(TimeFormatUtils.displayTimestamp(System.currentTimeMillis()));
        builder.append(" | screen=").append(safe(screen));
        builder.append(" | flow=").append(safe(flow));
        builder.append(" | event=").append(safe(event));
        if (!safe(errorCode).isEmpty()) {
            builder.append(" | errorCode=").append(errorCode.trim());
        }
        if (durationMs >= 0L) {
            builder.append(" | durationMs=").append(durationMs);
        }
        if (!safe(detail).isEmpty()) {
            builder.append(" | detail=").append(detail.trim());
        }
        return builder.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
