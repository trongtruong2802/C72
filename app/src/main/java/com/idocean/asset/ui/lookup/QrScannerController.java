package com.idocean.asset.ui.lookup;

import android.content.Context;
import android.os.SystemClock;

import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.core.QrScannerSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QrScannerController implements BarcodeScannerListener {
    private static final String SCREEN = "Lookup";
    private static final String FLOW_QR = "qr_scan";

    private final QrScannerSession qrScannerSession;
    private final ExecutorService executor;
    private final LookupScannerCallback callback;
    private volatile boolean started;
    private long qrScanRequestedAtMs = -1L;

    public QrScannerController(LookupScannerCallback callback) {
        this(callback, new QrScannerSession(), Executors.newSingleThreadExecutor());
    }

    QrScannerController(LookupScannerCallback callback, QrScannerSession qrScannerSession, ExecutorService executor) {
        this.callback = callback;
        this.qrScannerSession = qrScannerSession == null ? new QrScannerSession() : qrScannerSession;
        this.executor = executor;
    }

    public void onStart() {
        started = true;
        qrScannerSession.registerListener(this);
    }

    public void onStop() {
        started = false;
        stopScan();
        qrScannerSession.releaseSession();
        qrScannerSession.unregisterListener(this);
    }

    public void shutdown() {
        onStop();
        executor.shutdownNow();
    }

    public void startScan(Context context) {
        if (!started || callback == null || context == null) {
            return;
        }

        qrScanRequestedAtMs = SystemClock.elapsedRealtime();
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_QR, "scan_requested", "mode=QR");
        trace.markStart(null);
        if (qrScannerSession.isReady()) {
            if (!started) {
                return;
            }
            if (!qrScannerSession.startScan()) {
                trace.fail(null, "scan_failed", AppErrorCodes.QR_SCAN_FAILED, "startScan=false");
                callback.onQrScannerBusy();
            } else {
                trace.finish(null, "scan_started", "mode=QR");
            }
            return;
        }

        callback.onScannerPreparingChanged(true);
        executor.execute(() -> {
            if (!started) {
                return;
            }
            boolean ready = qrScannerSession.ensureReady(context.getApplicationContext());
            if (!started) {
                if (ready) {
                    qrScannerSession.releaseSession();
                }
                return;
            }

            callback.onScannerPreparingChanged(false);
            if (!ready) {
                qrScanRequestedAtMs = -1L;
                trace.fail(null, "scan_failed", AppErrorCodes.QR_OPEN_FAILED, "scannerReady=false");
                callback.onQrScannerUnavailable();
                return;
            }

            if (!started) {
                return;
            }
            if (!qrScannerSession.startScan()) {
                qrScanRequestedAtMs = -1L;
                trace.fail(null, "scan_failed", AppErrorCodes.QR_SCAN_FAILED, "startScan=false");
                callback.onQrScannerBusy();
            } else {
                trace.finish(null, "scan_started", "mode=QR");
            }
        });
    }

    public boolean stopScan() {
        return qrScannerSession.stopScan();
    }

    public boolean isReady() {
        return qrScannerSession.isReady();
    }

    public boolean isScanning() {
        return qrScannerSession.isScanning();
    }

    @Override
    public void onBarcodeScanned(String code, long timestamp) {
        if (!started || callback == null) {
            return;
        }
        if (qrScanRequestedAtMs > 0L) {
            long durationMs = Math.max(0L, SystemClock.elapsedRealtime() - qrScanRequestedAtMs);
            DebugEventLogger.duration(null, SCREEN, FLOW_QR, "scan_result", "code=" + abbreviate(code), durationMs);
            qrScanRequestedAtMs = -1L;
        }
        callback.onScanResult(ScanResult.qr(code, timestamp));
    }

    @Override
    public void onBarcodeScanError(String message) {
        if (!started || callback == null) {
            return;
        }
        String detail = message == null ? "" : message.trim();
        if (qrScanRequestedAtMs > 0L) {
            long durationMs = Math.max(0L, SystemClock.elapsedRealtime() - qrScanRequestedAtMs);
            detail = detail.isEmpty() ? "durationMs=" + durationMs : detail + " | durationMs=" + durationMs;
            qrScanRequestedAtMs = -1L;
        }
        DebugEventLogger.error(null, SCREEN, FLOW_QR, "scan_error", AppErrorCodes.QR_SCAN_FAILED, detail);
        callback.onScannerError(message);
    }

    private String abbreviate(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= 48) {
            return safeValue;
        }
        return safeValue.substring(0, 48) + "...";
    }
}
