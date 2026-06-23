package com.idocean.asset.diagnostics;

import com.idocean.asset.data.repository.LogRepository;

public final class AppFailureReporter {
    private AppFailureReporter() {
    }

    public static void report(LogRepository logRepository,
                              PerfLogger.Trace trace,
                              String screen,
                              String flow,
                              String event,
                              String errorCode,
                              Exception exception) {
        String detail = exception == null || exception.getMessage() == null
                ? ""
                : exception.getMessage();
        if (trace != null) {
            trace.fail(logRepository, event, errorCode, detail);
        }
        DebugEventLogger.error(logRepository, screen, flow, event, errorCode, detail);
    }
}
