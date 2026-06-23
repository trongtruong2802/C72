package com.idocean.asset.ui.inventory;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.idocean.asset.R;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.scanner.core.RfidContinuousInventorySession;
import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.core.QrScannerSession;
import com.idocean.asset.scanner.core.RfidReaderSession;
import com.idocean.asset.scanner.core.RfidSingleScanRunner;
import com.idocean.asset.scanner.core.RfidTagDecoder;
import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.scanner.rfid.UhfScanData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller boc tach phan scanner hardware cho InventoryActivity.
 * Xu ly:
 * - warmup/acquire QR + RFID
 * - start/stop scan
 * - decode RFID raw tag sang UhfScanData
 * - release hardware dung luc
 */
public final class InventoryScannerController implements BarcodeScannerListener {
    public interface Callback {
        void onQrScanResult(String code, long timestamp);

        void onRfidScanResult(UhfScanData scanData);

        void onRfidSingleScanCompleted();

        void onScannerError(String message);

        void onScannerStateChanged();
    }

    private static final String SCREEN = "Inventory";
    private static final String FLOW_WARMUP = "scanner_warmup";
    private static final String FLOW_QR = "qr_scan";
    private static final String FLOW_RFID_SINGLE = "rfid_single";
    private static final String FLOW_RFID_CONTINUOUS = "rfid_continuous";

    private final QrScannerSession qrScannerSession = new QrScannerSession();
    private final RfidReaderSession rfidReaderSession;
    private final RfidContinuousInventorySession continuousInventorySession;
    private final LogRepository logRepository;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile boolean started;
    private volatile boolean sessionActive;
    private boolean warmupScheduled;

    private long qrScanRequestedAtMs = -1L;

    public InventoryScannerController(Callback callback, LogRepository logRepository) {
        this.callback = callback;
        this.logRepository = logRepository;
        this.rfidReaderSession = new RfidReaderSession();
        this.continuousInventorySession = new RfidContinuousInventorySession(
                rfidReaderSession,
                logRepository,
                FLOW_RFID_CONTINUOUS
        );
    }

    public void onStart(Context activityContext) {
        started = true;
        qrScannerSession.registerListener(this);
    }

    public void onStop() {
        started = false;
        sessionActive = false;
        stopAllScanning();
        qrScannerSession.unregisterListener(this);
        releaseBarcodeSession();
        releaseDemoReader();
    }

    public void shutdown() {
        onStop();
        executor.shutdownNow();
    }

    public void warmup(Context activityContext) {
        if (warmupScheduled || activityContext == null) {
            return;
        }
        warmupScheduled = true;
        sessionActive = true;
        final Context appContext = activityContext.getApplicationContext();
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_WARMUP, "warmup_requested", "background");
        trace.markStart(logRepository);
        executor.execute(() -> {
            if (!sessionActive) {
                return;
            }
            if (!qrScannerSession.isReady()) {
                qrScannerSession.ensureReady(activityContext);
            }
            if (!sessionActive) {
                releaseBarcodeSession();
                return;
            }
            boolean readerReady = rfidReaderSession.initIfNeeded(appContext);
            dispatchScannerStateChanged();
            trace.finish(
                    logRepository,
                    "warmup_completed",
                    "qrReady=" + qrScannerSession.isReady() + " | rfidReady=" + readerReady
            );
        });
    }

    public void handleTriggerDown(Context context, boolean qrSelected, boolean singleScan) {
        if (!started || context == null) {
            return;
        }

        if (qrSelected) {
            startQrScan(context);
            return;
        }

        if (singleScan) {
            startSingleRfidScan(context);
            return;
        }

        if (!rfidReaderSession.isInventoryRunning()) {
            startContinuousRfidScan(context);
        }
    }

    public void handleTriggerUp(boolean continuousScanEnabled) {
        if (continuousScanEnabled) {
            stopDemoInventory();
            dispatchScannerStateChanged();
        }
    }

    public void stopAllScanning() {
        boolean wasQrScanning = qrScannerSession.isScanning();
        boolean wasRfidRunning = rfidReaderSession.isInventoryRunning();
        stopDemoInventory();
        qrScannerSession.stopScan();
        if (wasQrScanning || wasRfidRunning) {
            DebugEventLogger.info(
                    logRepository,
                    SCREEN,
                    wasQrScanning ? FLOW_QR : FLOW_RFID_CONTINUOUS,
                    "stop_requested",
                    wasQrScanning ? "mode=QR" : "mode=RFID"
            );
        }
    }

    public boolean isAnyScannerRunning() {
        return qrScannerSession.isScanning() || rfidReaderSession.isInventoryRunning();
    }

    public boolean isQrScanning() {
        return qrScannerSession.isScanning();
    }

    public boolean isDemoInventoryRunning() {
        return continuousInventorySession.isRunning();
    }

    public boolean isDemoReaderReady() {
        return rfidReaderSession.isReady();
    }

    public String getQrStatusMessage() {
        return qrScannerSession.getStatusMessage();
    }

    private void startQrScan(Context context) {
        qrScanRequestedAtMs = SystemClock.elapsedRealtime();
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_QR, "scan_requested", "mode=QR");
        trace.markStart(logRepository);
        if (!qrScannerSession.ensureReady(context)) {
            qrScanRequestedAtMs = -1L;
            trace.fail(logRepository, "scan_failed", AppErrorCodes.QR_OPEN_FAILED, "scannerReady=false");
            dispatchScannerError(context.getString(R.string.inventory_scanner_qr_open_failed));
            dispatchScannerStateChanged();
            return;
        }
        if (!qrScannerSession.startScan()) {
            qrScanRequestedAtMs = -1L;
            trace.fail(logRepository, "scan_failed", AppErrorCodes.QR_SCAN_FAILED, "startScan=false");
            dispatchScannerError(context.getString(R.string.inventory_scanner_qr_start_failed));
        } else {
            DebugEventLogger.info(logRepository, SCREEN, FLOW_QR, "scan_started", "scannerReady=true");
        }
        dispatchScannerStateChanged();
    }

    private void startContinuousRfidScan(Context context) {
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_RFID_CONTINUOUS, "scan_requested", "mode=continuous");
        trace.markStart(logRepository);
        if (!rfidReaderSession.initIfNeeded(context.getApplicationContext())) {
            trace.fail(logRepository, "scan_failed", AppErrorCodes.RFID_INIT_FAILED, "readerReady=false");
            dispatchScannerError(context.getString(R.string.inventory_reader_open_failed));
            dispatchScannerStateChanged();
            return;
        }
        RfidContinuousInventorySession.StartResult result = continuousInventorySession.start(
                context.getApplicationContext(),
                SCREEN,
                context.getString(R.string.inventory_reader_open_failed),
                context.getString(R.string.inventory_reader_mode_failed),
                context.getString(R.string.inventory_reader_inventory_failed),
                decodedTag -> dispatchRfidScanResult(decodedTag.toUhfScanData())
        );
        if (!result.isStarted()) {
            trace.fail(logRepository, "scan_failed", result.getErrorCode(), result.getErrorMessage());
            dispatchScannerError(result.getErrorMessage());
        } else {
            trace.finish(logRepository, "scan_started", "mode=continuous");
        }
        dispatchScannerStateChanged();
    }

    private void startSingleRfidScan(Context context) {
        PerfLogger.Trace trace = PerfLogger.start(SCREEN, FLOW_RFID_SINGLE, "scan_requested", "mode=single");
        trace.markStart(logRepository);
        RfidSingleScanRunner.Result result = RfidSingleScanRunner.run(
                context.getApplicationContext(),
                rfidReaderSession,
                context.getString(R.string.inventory_reader_open_failed),
                context.getString(R.string.inventory_reader_mode_failed),
                context.getString(R.string.inventory_reader_read_failed)
        );
        if (!result.isSuccess()) {
            trace.fail(logRepository, "scan_failed", result.getErrorCode(), result.getErrorMessage());
            dispatchScannerError(result.getErrorMessage());
            dispatchScannerStateChanged();
            return;
        }
        dispatchRfidScanResult(result.getDecodedTag().toUhfScanData());
        trace.finish(logRepository, "scan_result", "mode=single");
        dispatchRfidSingleScanCompleted();
    }

    private void stopDemoInventory() {
        continuousInventorySession.stop();
    }

    private void releaseBarcodeSession() {
        qrScannerSession.releaseSession();
    }

    private void releaseDemoReader() {
        continuousInventorySession.release();
    }

    private void dispatchRfidScanResult(UhfScanData scanData) {
        if (callback == null || scanData == null) {
            return;
        }
        mainHandler.post(() -> callback.onRfidScanResult(scanData));
    }

    private void dispatchRfidSingleScanCompleted() {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onRfidSingleScanCompleted());
    }

    private void dispatchScannerError(String message) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onScannerError(message));
    }

    private void dispatchScannerStateChanged() {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onScannerStateChanged());
    }

    @Override
    public void onBarcodeScanned(String code, long timestamp) {
        if (!started || callback == null) {
            return;
        }
        if (qrScanRequestedAtMs > 0L) {
            long durationMs = Math.max(0L, SystemClock.elapsedRealtime() - qrScanRequestedAtMs);
            DebugEventLogger.duration(
                    logRepository,
                    SCREEN,
                    FLOW_QR,
                    "scan_result",
                    "code=" + abbreviate(code),
                    durationMs
            );
            qrScanRequestedAtMs = -1L;
        }
        mainHandler.post(() -> callback.onQrScanResult(code, timestamp));
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
        DebugEventLogger.error(logRepository, SCREEN, FLOW_QR, "scan_error", AppErrorCodes.QR_SCAN_FAILED, detail);
        dispatchScannerError(message);
        dispatchScannerStateChanged();
    }

    public static String sanitizeTidValue(String value) {
        return RfidTagNormalizer.sanitizeTid(value);
    }

    public static String normalizeHexValue(String value) {
        return RfidTagNormalizer.normalizeHex(value);
    }

    public static String buildOutsideRfidKey(String tid, String epcHex) {
        return RfidTagNormalizer.buildOutsideRfidKey(tid, epcHex);
    }

    private String abbreviate(String value) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.length() <= 48) {
            return safeValue;
        }
        return safeValue.substring(0, 48) + "...";
    }
}
