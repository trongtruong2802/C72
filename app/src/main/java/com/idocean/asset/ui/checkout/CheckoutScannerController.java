package com.idocean.asset.ui.checkout;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idocean.asset.R;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.barcode.ChainwayBarcodeService;
import com.idocean.asset.ui.checkout.CheckoutActivity.ScreenTab;
import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.utils.EpcUtils;
import com.idocean.asset.scanner.rfid.UhfScanData;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.InventoryModeEntity;
import com.rscja.deviceapi.entity.InventoryParameter;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback;

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

    private static final String TAG_DEMO_UHF = "DEMO_UHF";
    private static final String TAG_INV_PERF = "INV_PERF";

    private final ChainwayBarcodeService barcodeService = ChainwayBarcodeService.getInstance();
    private final LogRepository logRepository;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService scannerExecutor = Executors.newSingleThreadExecutor();
    private final Object readerLock = new Object();

    private volatile boolean started;
    private boolean scannerPreparing;
    private ScreenTab scannerPreparingTab = ScreenTab.CHECKOUT;
    private boolean barcodeSessionAcquired;
    private RFIDWithUHFUART reader;
    private InventoryModeEntity previousInventoryMode;
    private boolean readerReady;
    private boolean readerInventoryRunning;
    private ScreenTab inventoryRunningTab = ScreenTab.CHECKOUT;

    public CheckoutScannerController(Callback callback, LogRepository logRepository) {
        this.callback = callback;
        this.logRepository = logRepository;
    }

    public void onStart(Context activityContext) {
        started = true;
        barcodeService.registerListener(this);
    }

    public void onStop() {
        started = false;
        stopAllScanning();
        barcodeService.unregisterListener(this);
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
        boolean wasQrScanning = barcodeService.isScanning();
        boolean wasRfidRunning = readerInventoryRunning;
        barcodeService.stopScan();
        stopContinuousInventory();
        if (wasQrScanning || wasRfidRunning) {
            logRepository.logInfo(
                    "STOP_SCAN",
                    "Dung scanner check out/check in",
                    wasQrScanning ? "QR" : "RFID"
            );
        }
        dispatchScannerStateChanged();
    }

    public boolean isAnyScannerRunning() {
        return barcodeService.isScanning() || readerInventoryRunning;
    }

    public boolean isQrScanning() {
        return barcodeService.isScanning();
    }

    public boolean isBarcodeReady() {
        return barcodeService.isReady();
    }

    public boolean isReaderInventoryRunning() {
        return readerInventoryRunning;
    }

    public boolean isReaderReady() {
        return readerReady;
    }

    public boolean isScannerPreparing() {
        return scannerPreparing;
    }

    public ScreenTab getScannerPreparingTab() {
        return scannerPreparingTab;
    }

    public String getQrStatusMessage() {
        return barcodeService.getStatusMessage();
    }

    private void startQrScan(ScreenTab targetTab, Context context) {
        logRepository.logInfo("START_SCAN", "Bat dau scan QR", describeTab(targetTab));
        if (barcodeService.isReady()) {
            startQrScanInternal(targetTab);
            return;
        }

        setScannerPreparing(targetTab, true);
        scannerExecutor.execute(() -> {
            boolean ready = ensureBarcodeReady(context);
            mainHandler.post(() -> {
                setScannerPreparing(targetTab, false);
                if (!started) {
                    if (ready && barcodeSessionAcquired) {
                        barcodeService.release();
                        barcodeSessionAcquired = false;
                    }
                    return;
                }
                if (!ready) {
                    logRepository.logError("ERROR", "Khong mo duoc scanner QR o man " + describeTab(targetTab));
                    dispatchScannerError(context.getString(R.string.checkout_scanner_unknown));
                    return;
                }
                startQrScanInternal(targetTab);
            });
        });
    }

    private void startQrScanInternal(ScreenTab targetTab) {
        if (!barcodeService.startScan()) {
            logRepository.logError("ERROR", "Scanner QR dang ban hoac dang cho ket qua", describeTab(targetTab));
            dispatchScannerError("Scanner QR dang ban hoac dang cho ket qua.");
        }
        dispatchScannerStateChanged();
    }

    private void runSingleRfidScan(ScreenTab targetTab, Context context) {
        stopContinuousInventory();
        setScannerPreparing(targetTab, true);
        scannerExecutor.execute(() -> {
            RfidReadResult result = readSingleRfid(context.getApplicationContext());
            mainHandler.post(() -> {
                setScannerPreparing(targetTab, false);
                if (!started) {
                    if (reader != null) {
                        releaseReader();
                    }
                    return;
                }
                dispatchScannerStateChanged();
                if (!result.isSuccess()) {
                    logRepository.logError("ERROR", "Doc RFID that bai o man " + describeTab(targetTab), result.getErrorMessage());
                    dispatchScannerError(result.getErrorMessage());
                    return;
                }
                dispatchRfidScanResult(targetTab, result, false);
            });
        });
    }

    private void startContinuousRfidScan(ScreenTab targetTab, Context context) {
        stopContinuousInventory();
        setScannerPreparing(targetTab, true);
        scannerExecutor.execute(() -> {
            String errorMessage = null;
            boolean inventoryStarted = false;

            if (!initReaderIfNeeded(context.getApplicationContext())) {
                errorMessage = "Khong mo duoc dau doc UHF.";
            } else if (!ensureTidMode()) {
                errorMessage = "Khong chuyen duoc reader sang mode EPC + TID.";
            } else {
                synchronized (readerLock) {
                    if (reader == null) {
                        errorMessage = "Khong mo duoc dau doc UHF.";
                    } else {
                        reader.setInventoryCallback(new IUHFInventoryCallback() {
                            @Override
                            public void callback(UHFTAGInfo uhftagInfo) {
                                handleContinuousTag(context.getApplicationContext(), targetTab, uhftagInfo);
                            }
                        });

                        InventoryParameter parameter = new InventoryParameter();
                        parameter.setResultData(new InventoryParameter.ResultData().setNeedPhase(false));
                        inventoryStarted = reader.startInventoryTag(parameter);
                        inventoryRunningTab = targetTab;
                        readerInventoryRunning = inventoryStarted;
                        if (!inventoryStarted) {
                            reader.setInventoryCallback(null);
                            errorMessage = "Khong the bat dau inventory UHF.";
                        }
                    }
                }
            }

            boolean finalInventoryStarted = inventoryStarted;
            String finalErrorMessage = errorMessage;
            mainHandler.post(() -> {
                setScannerPreparing(targetTab, false);
                if (!started) {
                    if (!started) {
                        stopContinuousInventory();
                        releaseReader();
                    }
                    return;
                }
                if (!finalInventoryStarted && finalErrorMessage != null) {
                    logRepository.logError("ERROR", "Khong the bat dau inventory RFID o man " + describeTab(targetTab), finalErrorMessage);
                    dispatchScannerError(finalErrorMessage);
                }
                dispatchScannerStateChanged();
            });
        });
    }

    private void handleContinuousTag(Context context, ScreenTab targetTab, UHFTAGInfo tagInfo) {
        RfidReadResult result = buildRfidReadResult(context, tagInfo);
        if (!result.isSuccess()) {
            return;
        }
        mainHandler.post(() -> {
            if (!started) {
                return;
            }
            dispatchRfidScanResult(targetTab, result, true);
        });
    }

    private RfidReadResult readSingleRfid(Context context) {
        synchronized (readerLock) {
            if (!initReaderIfNeeded(context)) {
                return RfidReadResult.error("Khong mo duoc dau doc UHF.");
            }
            if (!ensureTidMode()) {
                return RfidReadResult.error("Khong chuyen duoc reader sang mode EPC + TID.");
            }

            UHFTAGInfo tagInfo = reader == null ? null : reader.inventorySingleTag();
            if (tagInfo == null) {
                return RfidReadResult.error("Khong doc duoc the RFID.");
            }

            return buildRfidReadResult(context, tagInfo);
        }
    }

    private RfidReadResult buildRfidReadResult(Context context, UHFTAGInfo tagInfo) {
        UhfScanData scanData = UhfScanData.from(tagInfo);
        String tid = RfidTagNormalizer.sanitizeTid(scanData.getTid());
        String epcHex = RfidTagNormalizer.normalizeHex(scanData.getEpcHex());
        String code = safe(scanData.getEpcAsciiCode());
        if (tid.isEmpty() && !epcHex.isEmpty()) {
            tid = readTidFromBank(epcHex);
        }
        if (code.isEmpty()) {
            code = EpcUtils.hexToAscii(epcHex);
        }
        if (tid.isEmpty()) {
            return RfidReadResult.error(context.getString(R.string.checkout_need_identifier));
        }
        return RfidReadResult.success(tid, code, epcHex, System.currentTimeMillis());
    }

    private boolean ensureBarcodeReady(Context context) {
        if (barcodeService.isReady()) {
            return true;
        }
        boolean ready = barcodeService.acquire(context);
        if (ready) {
            barcodeSessionAcquired = true;
            return true;
        }
        barcodeService.release();
        barcodeSessionAcquired = false;
        return false;
    }

    private boolean initReaderIfNeeded(Context appContext) {
        synchronized (readerLock) {
            if (readerReady && reader != null) {
                return true;
            }
            try {
                reader = RFIDWithUHFUART.getInstance();
                if (reader == null) {
                    readerReady = false;
                    return false;
                }
                readerReady = reader.init(appContext);
                if (readerReady) {
                    previousInventoryMode = reader.getEPCAndTIDUserMode();
                }
                return readerReady;
            } catch (Exception exception) {
                readerReady = false;
                return false;
            }
        }
    }

    private boolean ensureTidMode() {
        synchronized (readerLock) {
            if (reader == null) {
                return false;
            }
            boolean result = reader.setEPCAndTIDUserMode(
                    new InventoryModeEntity.Builder()
                            .setMode(InventoryModeEntity.MODE_EPC_TID)
                            .build()
            );
            if (!result) {
                result = reader.setEPCAndTIDMode();
            }
            return result;
        }
    }

    private void stopContinuousInventory() {
        synchronized (readerLock) {
            if (reader == null) {
                readerInventoryRunning = false;
                return;
            }
            try {
                if (readerInventoryRunning || reader.isInventorying()) {
                    reader.stopInventory();
                }
            } catch (Exception ignored) {
            }
            try {
                reader.setInventoryCallback(null);
            } catch (Exception ignored) {
            }
            readerInventoryRunning = false;
        }
    }

    private void releaseBarcodeSession() {
        if (!barcodeSessionAcquired) {
            return;
        }
        barcodeService.release();
        barcodeSessionAcquired = false;
    }

    private void releaseReader() {
        synchronized (readerLock) {
            if (reader == null) {
                readerReady = false;
                return;
            }
            stopContinuousInventory();
            try {
                if (previousInventoryMode != null) {
                    reader.setEPCAndTIDUserMode(previousInventoryMode);
                }
            } catch (Exception ignored) {
            }
            try {
                reader.free();
            } catch (Exception ignored) {
            }
            reader = null;
            previousInventoryMode = null;
            readerReady = false;
            inventoryRunningTab = ScreenTab.CHECKOUT;
        }
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
        mainHandler.post(() -> callback.onQrScanResult(code, timestamp));
    }

    @Override
    public void onBarcodeScanError(String message) {
        if (!started || callback == null) {
            return;
        }
        dispatchScannerError(message);
        dispatchScannerStateChanged();
    }

    public static String sanitizeTidValue(String value) {
        return RfidTagNormalizer.sanitizeTid(value);
    }

    public static String normalizeHexValue(String value) {
        return RfidTagNormalizer.normalizeHex(value);
    }

    private String readTidFromBank(String epcHex) {
        synchronized (readerLock) {
            if (reader == null || epcHex == null || epcHex.trim().isEmpty()) {
                return "";
            }
            try {
                String tid = reader.readData(
                        "00000000",
                        RFIDWithUHFUART.Bank_EPC,
                        32,
                        epcHex.trim().length() * 4,
                        epcHex.trim(),
                        RFIDWithUHFUART.Bank_TID,
                        0,
                        6
                );
                return RfidTagNormalizer.sanitizeTid(tid);
            } catch (Exception exception) {
                Log.e(TAG_DEMO_UHF, "[DEMO_UHF] read TID bank failed", exception);
                return "";
            }
        }
    }

    private String describeTab(ScreenTab tab) {
        return tab == ScreenTab.CHECKIN ? "Check In" : "Check Out";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
