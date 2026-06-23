package com.idocean.asset.ui.checkout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.idocean.asset.R;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.diagnostics.AppErrorCodes;
import com.idocean.asset.diagnostics.DebugEventLogger;
import com.idocean.asset.diagnostics.PerfLogger;
import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.core.QrScannerSession;
import com.idocean.asset.scanner.core.RfidContinuousInventorySession;
import com.idocean.asset.scanner.core.RfidReaderSession;
import com.idocean.asset.scanner.core.RfidSingleScanRunner;
import com.idocean.asset.scanner.core.RfidTagDecoder;
import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.ui.checkout.CheckoutActivity.ScreenTab;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller tach rien phan scanner hardware cho CheckoutActivity.
 * Activity chi con render UI, con controller lo:
 * - acquire/release barcode session
 * - khoi tao / stop / release RFID reader
 * - scan QR / RFID
 * - callback ket qua ve Activity
 */
public final class CheckoutScannerController implements BarcodeScannerListener {
    public interface Callback {
        void onQrScanResult(String code, long timestamp);

        void onRfidScanResult(ScreenTab targetTab, RfidReadResult result, boolean suppressDuplicateToast);

        void onScannerError(String message);

        void onScannerStateChanged();
    }

    public static final class RfidReadResult {
        private final String tid;
        private final String code;
        private final String epcHex;
        private final long scannedAt;
        private final String errorMessage;

        private RfidReadResult(String tid, String code, String epcHex, long scannedAt, String errorMessage) {
            this.tid = tid == null ? "" : tid;
            this.code = code == null ? "" : code;
            this.epcHex = epcHex == null ? "" : epcHex;
            this.scannedAt = scannedAt;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }

        public static RfidReadResult success(String tid, String code, String epcHex, long scannedAt) {
            return new RfidReadResult(tid, code, epcHex, scannedAt, null);
        }

        public static RfidReadResult error(String errorMessage) {
            return new RfidReadResult("", "", "", 0L, errorMessage);
        }

        public String getTid() {
            return tid;
        }

        public String getCode() {
            return code;
        }

        public String getEpcHex() {
            return epcHex;
        }

        public long getScannedAt() {
            return scannedAt;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null || errorMessage.trim().isEmpty();
        }
    }

    private static final String FLOW_QR = "qr_scan";
    private static final String FLOW_RFID_SINGLE = "rfid_single";
    private static final String FLOW_RFID_CONTINUOUS = "rfid_continuous";

    private final QrScannerSession qrScannerSession = new QrScannerSession();
    private final RfidReaderSession rfidReaderSession;
    private final RfidContinuousInventorySession continuousInventorySession;
    private final LogRepository logRepository;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scannerExecutor = Executors.newSingleThreadExecutor();

    private volatile boolean started;
    private boolean scannerPreparing;
    private ScreenTab scannerPreparingTab = ScreenTab.CHECKOUT;
    private ScreenTab qrRequestedTab = ScreenTab.CHECKOUT;
    private long qrScanRequestedAtMs = -1L;
    private ScreenTab inventoryRunningTab = ScreenTab.CHECKOUT;

    public CheckoutScannerController(Callback callback, LogRepository logRepository) {
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
        stopAllScanning();
        qrScannerSession.unregisterListener(this);
        releaseBarcodeSession();
        releaseReader();
        scannerPreparing = false;
    }

    public void shutdown() {
        onStop();
        scannerExecutor.shutdownNow();
    }

    public void handleTriggerDown(Context context, ScreenTab targetTab, boolean qrSelected, boolean singleScan) {
        if (!started || context == null || scannerPreparing) {
            return;
        }

        if (qrSelected) {
            startQrScan(targetTab, context);
            return;
        }

        if (singleScan) {
            runSingleRfidScan(targetTab, context);
        } else {
            startContinuousRfidScan(targetTab, context);
        }
    }

    public void handleTriggerUp(boolean continuousScanEnabled) {
        if (continuousScanEnabled) {
            stopContinuousInventory();
            dispatchScannerStateChanged();
        }
    }

    public void stopAllScanning() {
        boolean wasQrScanning = qrScannerSession.isScanning();
        boolean wasRfidRunning = rfidReaderSession.isInventoryRunning();
        qrScannerSession.stopScan();
        stopContinuousInventory();
        if (wasQrScanning || wasRfidRunning) {
            DebugEventLogger.info(
                    logRepository,
                    screenName(wasRfidRunning ? inventoryRunningTab : qrRequestedTab),
                    wasQrScanning ? FLOW_QR : FLOW_RFID_CONTINUOUS,
                    "stop_requested",
                    wasQrScanning ? "mode=QR" : "mode=RFID"
            );
        }
        dispatchScannerStateChanged();
    }

    public boolean isAnyScannerRunning() {
        return qrScannerSession.isScanning() || rfidReaderSession.isInventoryRunning();
    }

    public boolean isQrScanning() {
        return qrScannerSession.isScanning();
    }

    public boolean isBarcodeReady() {
        return qrScannerSession.isReady();
    }

    public boolean isReaderInventoryRunning() {
        return continuousInventorySession.isRunning();
    }

    public boolean isReaderReady() {
        return rfidReaderSession.isReady();
    }

    public boolean isScannerPreparing() {
        return scannerPreparing;
    }

    public ScreenTab getScannerPreparingTab() {
        return scannerPreparingTab;
    }

    public String getQrStatusMessage() {
        return qrScannerSession.getStatusMessage();
    }

    private void startQrScan(ScreenTab targetTab, Context context) {
        qrRequestedTab = targetTab == null ? ScreenTab.CHECKOUT : targetTab;
        qrScanRequestedAtMs = SystemClock.elapsedRealtime();
        PerfLogger.Trace trace = PerfLogger.start(screenName(qrRequestedTab), FLOW_QR, "scan_requested", "mode=QR");
        trace.markStart(logRepository);
        if (qrScannerSession.isReady()) {
            startQrScanInternal(qrRequestedTab, trace);
            return;
        }

        setScannerPreparing(qrRequestedTab, true);
        scannerExecutor.execute(() -> {
            boolean ready = qrScannerSession.ensureReady(context);
            mainHandler.post(() -> {
                setScannerPreparing(qrRequestedTab, false);
                if (!started) {
                    if (ready) {
                        qrScannerSession.releaseSession();
                    }
                    return;
                }
                if (!ready) {
                    qrScanRequestedAtMs = -1L;
                    trace.fail(logRepository, "scan_failed", AppErrorCodes.QR_OPEN_FAILED, "scannerReady=false");
                    dispatchScannerError(context.getString(R.string.checkout_scanner_unknown));
                    return;
                }
                startQrScanInternal(qrRequestedTab, trace);
            });
        });
    }

    private void startQrScanInternal(ScreenTab targetTab, PerfLogger.Trace trace) {
        if (!qrScannerSession.startScan()) {
            qrScanRequestedAtMs = -1L;
            trace.fail(logRepository, "scan_failed", AppErrorCodes.QR_SCAN_FAILED, "startScan=false");
            dispatchScannerError("Scanner QR dang ban hoac dang cho ket qua.");
        } else {
            trace.finish(logRepository, "scan_started", "mode=QR");
        }
        dispatchScannerStateChanged();
    }

    private void runSingleRfidScan(ScreenTab targetTab, Context context) {
        stopContinuousInventory();
        setScannerPreparing(targetTab, true);
        PerfLogger.Trace trace = PerfLogger.start(screenName(targetTab), FLOW_RFID_SINGLE, "scan_requested", "mode=single");
        trace.markStart(logRepository);
        scannerExecutor.execute(() -> {
            RfidReadResult result = readSingleRfid(context.getApplicationContext());
            mainHandler.post(() -> {
                setScannerPreparing(targetTab, false);
                if (!started) {
                    releaseReader();
                    return;
                }
                dispatchScannerStateChanged();
                if (!result.isSuccess()) {
                    trace.fail(logRepository, "scan_failed", AppErrorCodes.RFID_SINGLE_SCAN_FAILED, result.getErrorMessage());
                    dispatchScannerError(result.getErrorMessage());
                    return;
                }
                trace.finish(
                        logRepository,
                        "scan_result",
                        "tid=" + abbreviate(result.getTid()) + " | code=" + abbreviate(result.getCode())
                );
                dispatchRfidScanResult(targetTab, result, false);
            });
        });
    }

    private void startContinuousRfidScan(ScreenTab targetTab, Context context) {
        stopContinuousInventory();
        setScannerPreparing(targetTab, true);
        PerfLogger.Trace trace = PerfLogger.start(screenName(targetTab), FLOW_RFID_CONTINUOUS, "scan_requested", "mode=continuous");
        trace.markStart(logRepository);
        scannerExecutor.execute(() -> {
            RfidContinuousInventorySession.StartResult result = continuousInventorySession.start(
                    context.getApplicationContext(),
                    screenName(targetTab),
                    "Khong mo duoc dau doc UHF.",
                    "Khong chuyen duoc reader sang mode EPC + TID.",
                    "Khong the bat dau inventory UHF.",
                    decodedTag -> handleContinuousTag(targetTab, context.getApplicationContext(), decodedTag)
            );
            mainHandler.post(() -> {
                setScannerPreparing(targetTab, false);
                if (!started) {
                    stopContinuousInventory();
                    releaseReader();
                    return;
                }
                if (!result.isStarted()) {
                    trace.fail(logRepository, "scan_failed", result.getErrorCode(), result.getErrorMessage());
                    dispatchScannerError(result.getErrorMessage());
                } else {
                    inventoryRunningTab = targetTab == null ? ScreenTab.CHECKOUT : targetTab;
                    trace.finish(logRepository, "scan_started", "mode=continuous");
                }
                dispatchScannerStateChanged();
            });
        });
    }

    private void handleContinuousTag(ScreenTab targetTab, Context context, RfidTagDecoder.DecodedTag decodedTag) {
        RfidReadResult result = buildRfidReadResult(context, decodedTag);
        if (!result.isSuccess()) {
            return;
        }
        if (!started) {
            return;
        }
        dispatchRfidScanResult(targetTab, result, true);
    }

    private RfidReadResult readSingleRfid(Context context) {
        RfidSingleScanRunner.Result result = RfidSingleScanRunner.run(
                context,
                rfidReaderSession,
                "Khong mo duoc dau doc UHF.",
                "Khong chuyen duoc reader sang mode EPC + TID.",
                "Khong doc duoc the RFID."
        );
        if (!result.isSuccess()) {
            return RfidReadResult.error(result.getErrorMessage());
        }
        return buildRfidReadResult(context, result.getDecodedTag());
    }

    private RfidReadResult buildRfidReadResult(Context context, RfidTagDecoder.DecodedTag decodedTag) {
        if (decodedTag.getTid().isEmpty()) {
            return RfidReadResult.error(context.getString(R.string.checkout_need_identifier));
        }
        return RfidReadResult.success(
                decodedTag.getTid(),
                decodedTag.getCode(),
                decodedTag.getEpcHex(),
                decodedTag.getScannedAt()
        );
    }

    private void stopContinuousInventory() {
        continuousInventorySession.stop();
    }

    private void releaseBarcodeSession() {
        qrScannerSession.releaseSession();
    }

    private void releaseReader() {
        continuousInventorySession.release();
        inventoryRunningTab = ScreenTab.CHECKOUT;
    }

    private void dispatchRfidScanResult(ScreenTab targetTab, RfidReadResult result, boolean suppressDuplicateToast) {
        if (callback == null || result == null) {
            return;
        }
        mainHandler.post(() -> callback.onRfidScanResult(targetTab, result, suppressDuplicateToast));
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
        mainHandler.post(callback::onScannerStateChanged);
    }

    private void setScannerPreparing(ScreenTab targetTab, boolean preparing) {
        scannerPreparing = preparing;
        if (preparing && targetTab != null) {
            scannerPreparingTab = targetTab;
        }
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
                    screenName(qrRequestedTab),
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
        DebugEventLogger.error(
                logRepository,
                screenName(qrRequestedTab),
                FLOW_QR,
                "scan_error",
                AppErrorCodes.QR_SCAN_FAILED,
                detail
        );
        dispatchScannerError(message);
        dispatchScannerStateChanged();
    }

    public static String sanitizeTidValue(String value) {
        return RfidTagNormalizer.sanitizeTid(value);
    }

    public static String normalizeHexValue(String value) {
        return RfidTagNormalizer.normalizeHex(value);
    }

    private String screenName(ScreenTab tab) {
        return tab == ScreenTab.CHECKIN ? "Check In" : "Check Out";
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
