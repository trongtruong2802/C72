package com.idocean.asset.diagnostics;

import android.os.SystemClock;

import com.idocean.asset.data.repository.LogRepository;

public final class PerfLogger {

    private PerfLogger() {
    }

    public static Trace start(String screen, String flow, String event) {
        return new Trace(screen, flow, event, "");
    }

    public static Trace start(String screen, String flow, String event, String detail) {
        return new Trace(screen, flow, event, detail);
    }

    public static final class Trace {
        private final String screen;
        private final String flow;
        private final String startEvent;
        private final String startDetail;
        private final long startedAtMs;

        Trace(String screen, String flow, String startEvent, String startDetail) {
            this.screen = screen == null ? "" : screen;
            this.flow = flow == null ? "" : flow;
            this.startEvent = startEvent == null ? "" : startEvent;
            this.startDetail = startDetail == null ? "" : startDetail;
            this.startedAtMs = SystemClock.elapsedRealtime();
        }

        public void markStart(LogRepository logRepository) {
            DebugEventLogger.info(logRepository, screen, flow, startEvent, startDetail);
        }

        public long finish(String finishEvent, String detail) {
            long durationMs = elapsedMs();
            DebugEventLogger.duration(screen, flow, finishEvent, detail, durationMs);
            return durationMs;
        }

        public long finish(LogRepository logRepository, String finishEvent, String detail) {
            long durationMs = elapsedMs();
            DebugEventLogger.duration(logRepository, screen, flow, finishEvent, detail, durationMs);
            return durationMs;
        }

        public long fail(LogRepository logRepository, String finishEvent, String errorCode, String detail) {
            long durationMs = elapsedMs();
            DebugEventLogger.error(
                    logRepository,
                    screen,
                    flow,
                    finishEvent,
                    errorCode,
                    appendDuration(detail, durationMs)
            );
            return durationMs;
        }

        public long elapsedMs() {
            return Math.max(0L, SystemClock.elapsedRealtime() - startedAtMs);
        }

        private String appendDuration(String detail, long durationMs) {
            String safeDetail = detail == null ? "" : detail.trim();
            if (safeDetail.isEmpty()) {
                return "durationMs=" + durationMs;
            }
            return safeDetail + " | durationMs=" + durationMs;
        }
    }
}
