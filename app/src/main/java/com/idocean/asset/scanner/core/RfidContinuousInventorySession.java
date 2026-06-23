package com.idocean.asset.scanner.core;

import android.content.Context;
import android.os.SystemClock;

import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback;

public final class RfidContinuousInventorySession {
    public interface TagCallback {
        void onDecodedTag(RfidTagDecoder.DecodedTag decodedTag);
    }

    public static final class StartResult {
        private final boolean started;
        private final String errorCode;
        private final String errorMessage;

        private StartResult(boolean started, String errorCode, String errorMessage) {
            this.started = started;
            this.errorCode = errorCode == null ? "" : errorCode;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }

        public static StartResult success() {
            return new StartResult(true, "", "");
        }

        public static StartResult error(String errorCode, String errorMessage) {
            return new StartResult(false, errorCode, errorMessage);
        }

        public boolean isStarted() {
            return started;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private final RfidReaderSession rfidReaderSession;
    private final LogRepository logRepository;
    private final String flow;

    private long continuousStartedAtMs = -1L;
    private int continuousTagCount;
    private String activeScreen = "";

    public RfidContinuousInventorySession(RfidReaderSession rfidReaderSession,
                                          LogRepository logRepository,
                                          String flow) {
        this.rfidReaderSession = rfidReaderSession == null ? new RfidReaderSession() : rfidReaderSession;
        this.logRepository = logRepository;
        this.flow = flow == null ? "" : flow;
    }

    public StartResult start(Context appContext,
                             String screen,
                             String initFailedMessage,
                             String modeFailedMessage,
                             String startFailedMessage,
                             TagCallback tagCallback) {
        stop();
        if (!rfidReaderSession.initIfNeeded(appContext)) {
            return StartResult.error(AppErrorCodes.RFID_INIT_FAILED, initFailedMessage);
        }
        if (!rfidReaderSession.ensureTidMode()) {
            return StartResult.error(AppErrorCodes.RFID_MODE_FAILED, modeFailedMessage);
        }
        boolean inventoryStarted = rfidReaderSession.startInventory(new IUHFInventoryCallback() {
            @Override
            public void callback(UHFTAGInfo uhftagInfo) {
                handleTag(screen, uhftagInfo, tagCallback);
            }
        });
        if (!inventoryStarted) {
            return StartResult.error(AppErrorCodes.RFID_CONTINUOUS_SCAN_FAILED, startFailedMessage);
        }
        activeScreen = safe(screen);
        continuousStartedAtMs = SystemClock.elapsedRealtime();
        continuousTagCount = 0;
        return StartResult.success();
    }

    public void stop() {
        boolean wasRunning = rfidReaderSession.isInventoryRunning();
        if (wasRunning && continuousStartedAtMs > 0L) {
            long burstDurationMs = Math.max(0L, SystemClock.elapsedRealtime() - continuousStartedAtMs);
            DebugEventLogger.duration(
                    logRepository,
                    safe(activeScreen),
                    flow,
                    "burst_completed",
                    "tagCount=" + continuousTagCount,
                    burstDurationMs
            );
        }
        rfidReaderSession.stopInventory();
        activeScreen = "";
        continuousStartedAtMs = -1L;
        continuousTagCount = 0;
    }

    public void release() {
        stop();
        rfidReaderSession.release();
    }

    public boolean isRunning() {
        return rfidReaderSession.isInventoryRunning();
    }

    private void handleTag(String screen, UHFTAGInfo tagInfo, TagCallback tagCallback) {
        if (tagInfo == null) {
            return;
        }
        RfidTagDecoder.DecodedTag decodedTag = RfidTagDecoder.decode(tagInfo, rfidReaderSession::readTidFromBank);
        continuousTagCount++;
        if (continuousTagCount == 1 && continuousStartedAtMs > 0L) {
            long firstTagLatencyMs = Math.max(0L, SystemClock.elapsedRealtime() - continuousStartedAtMs);
            DebugEventLogger.duration(
                    logRepository,
                    safe(screen),
                    flow,
                    "first_tag_received",
                    "tid=" + abbreviate(decodedTag.getTid()) + " | code=" + abbreviate(decodedTag.getCode()),
                    firstTagLatencyMs
            );
        }
        if (tagCallback != null) {
            tagCallback.onDecodedTag(decodedTag);
        }
    }

    private String abbreviate(String value) {
        String safeValue = safe(value).trim();
        if (safeValue.length() <= 48) {
            return safeValue;
        }
        return safeValue.substring(0, 48) + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
