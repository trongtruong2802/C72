package com.idocean.asset.ui.lookup;

import android.content.Context;

import com.idocean.asset.R;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.scanner.core.RfidReaderSession;
import com.idocean.asset.scanner.core.RfidSingleScanRunner;
import com.idocean.asset.scanner.core.RfidTagDecoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RfidScannerController {
    private static final String SCREEN = "Lookup";
    private static final String FLOW_RFID_SINGLE = "rfid_single";

    private final ExecutorService executor;
    private final LookupScannerCallback callback;
    private final RfidReaderSession rfidReaderSession;
    private volatile boolean started;

    public RfidScannerController(LookupScannerCallback callback) {
        this(callback, new RfidReaderSession(), Executors.newSingleThreadExecutor());
    }

    RfidScannerController(LookupScannerCallback callback, RfidReaderSession rfidReaderSession, ExecutorService executor) {
        this.callback = callback;
        this.rfidReaderSession = rfidReaderSession == null ? new RfidReaderSession() : rfidReaderSession;
        this.executor = executor;
    }

    public void onStart() {
        started = true;
    }

    public void onStop() {
        started = false;
        release();
    }

    public void shutdown() {
        onStop();
        executor.shutdownNow();
    }

    public void scanSingle(Context context) {
        if (!started || callback == null || context == null) {
            return;
        }

        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_RFID_SINGLE, "scan_requested", "mode=single");
        trace.markStart(null);
        callback.onScannerPreparingChanged(true);
        executor.execute(() -> {
            if (!started) {
                return;
            }
            RfidLookupResult result = readSingleRfid(context.getApplicationContext());
            if (!started) {
                return;
            }
            callback.onScannerPreparingChanged(false);
            if (!result.success) {
                String errorCode;
                if ("Khong mo duoc dau doc UHF.".equals(result.errorMessage)) {
                    errorCode = AppErrorCodes.RFID_INIT_FAILED;
                } else if ("Khong chuyen duoc reader sang mode EPC + TID.".equals(result.errorMessage)) {
                    errorCode = AppErrorCodes.RFID_MODE_FAILED;
                } else {
                    errorCode = AppErrorCodes.RFID_SINGLE_SCAN_FAILED;
                }
                trace.fail(null, "scan_failed", errorCode, result.errorMessage);
                callback.onScannerError(result.errorMessage);
                return;
            }
            trace.finish(null, "scan_result", "tid=" + abbreviate(result.tid) + " | code=" + abbreviate(result.code));
            callback.onScanResult(ScanResult.rfid(result.tid, result.code, result.timestamp));
        });
    }

    public boolean isReady() {
        return rfidReaderSession.isReady();
    }

    private RfidLookupResult readSingleRfid(Context context) {
        RfidSingleScanRunner.Result result = RfidSingleScanRunner.run(
                context,
                rfidReaderSession,
                "Khong mo duoc dau doc UHF.",
                "Khong chuyen duoc reader sang mode EPC + TID.",
                "Khong doc duoc the RFID."
        );
        if (!result.isSuccess()) {
            return RfidLookupResult.error(result.getErrorMessage());
        }
        RfidTagDecoder.DecodedTag decodedTag = result.getDecodedTag();
        if (decodedTag.getTid().isEmpty()) {
            return RfidLookupResult.error(context.getString(R.string.lookup_scanner_unknown));
        }
        return RfidLookupResult.success(decodedTag.getTid(), decodedTag.getCode());
    }

    public void release() {
        rfidReaderSession.release();
    }

    private String abbreviate(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= 48) {
            return safeValue;
        }
        return safeValue.substring(0, 48) + "...";
    }

    private static final class RfidLookupResult {
        private final boolean success;
        private final String tid;
        private final String code;
        private final String errorMessage;
        private final long timestamp;

        private RfidLookupResult(boolean success, String tid, String code, String errorMessage, long timestamp) {
            this.success = success;
            this.tid = tid;
            this.code = code;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }

        static RfidLookupResult success(String tid, String code) {
            return new RfidLookupResult(true, tid, code, "", System.currentTimeMillis());
        }

        static RfidLookupResult error(String errorMessage) {
            return new RfidLookupResult(false, "", "", errorMessage, System.currentTimeMillis());
        }
    }
}
