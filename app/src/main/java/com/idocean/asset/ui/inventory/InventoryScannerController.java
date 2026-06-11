package com.idocean.asset.ui.inventory;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idocean.asset.R;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.barcode.ChainwayBarcodeService;
import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.scanner.rfid.UhfScanData;
import com.idocean.asset.utils.EpcUtils;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.InventoryModeEntity;
import com.rscja.deviceapi.entity.InventoryParameter;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback;

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

    private static final String TAG_DEMO_UHF = "DEMO_UHF";
    private static final String TAG_INVENTORY = "INVENTORY";
    private static final String TAG_INV_PERF = "INV_PERF";

    private final ChainwayBarcodeService barcodeService = ChainwayBarcodeService.getInstance();
    private final LogRepository logRepository;
    private final Callback callback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object readerLock = new Object();

    private volatile boolean started;
    private volatile boolean sessionActive;
    private boolean warmupScheduled;
    private boolean sessionAcquired;
    private RFIDWithUHFUART demoReader;
    private InventoryModeEntity previousInventoryMode;
    private boolean demoReaderReady;
    private boolean demoInventoryRunning;

    public InventoryScannerController(Callback callback, LogRepository logRepository) {
        this.callback = callback;
        this.logRepository = logRepository;
    }

    public void onStart(Context activityContext) {
        started = true;
        barcodeService.registerListener(this);
    }

    public void onStop() {
        started = false;
        sessionActive = false;
        stopAllScanning();
        barcodeService.unregisterListener(this);
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
        executor.execute(() -> {
            if (!sessionActive) {
                return;
            }
            if (!barcodeService.isReady()) {
                ensureBarcodeReady(activityContext);
            }
            if (!sessionActive) {
                releaseBarcodeSession();
                return;
            }
            initDemoReaderIfNeeded(appContext);
            dispatchScannerStateChanged();
            Log.d(TAG_INV_PERF, "[INV_PERF] scanner warmup end");
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

        if (!demoInventoryRunning) {
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
        boolean wasQrScanning = barcodeService.isScanning();
        boolean wasRfidRunning = demoInventoryRunning;
        stopDemoInventory();
        barcodeService.stopScan();
        if (wasQrScanning || wasRfidRunning) {
            logRepository.logInfo(
                    "STOP_SCAN",
                    "Dung scanner kiem ke",
                    wasQrScanning ? "QR" : "RFID"
            );
        }
    }

    public boolean isAnyScannerRunning() {
        return barcodeService.isScanning() || demoInventoryRunning;
    }

    public boolean isQrScanning() {
        return barcodeService.isScanning();
    }

    public boolean isDemoInventoryRunning() {
        return demoInventoryRunning;
    }

    public boolean isDemoReaderReady() {
        return demoReaderReady;
    }

    public String getQrStatusMessage() {
        return barcodeService.getStatusMessage();
    }

    private void startQrScan(Context context) {
        logRepository.logInfo("START_SCAN", "Bat dau scan QR kiem ke", "QR");
        if (!ensureBarcodeReady(context)) {
            logRepository.logError("ERROR", "Khong mo duoc scanner QR/2D o man kiem ke");
            dispatchScannerError(context.getString(R.string.inventory_scanner_qr_open_failed));
            dispatchScannerStateChanged();
            return;
        }
        if (!barcodeService.startScan()) {
            logRepository.logError("ERROR", "Khong the bat dau quet QR/2D o man kiem ke");
            dispatchScannerError(context.getString(R.string.inventory_scanner_qr_start_failed));
        }
        dispatchScannerStateChanged();
    }

    private void startContinuousRfidScan(Context context) {
        logRepository.logInfo("START_SCAN", "Bat dau scan RFID kiem ke", "Lien tuc");
        if (!initDemoReaderIfNeeded(context.getApplicationContext())) {
            logRepository.logError("ERROR", "Khong mo duoc dau doc UHF o man kiem ke");
            dispatchScannerError(context.getString(R.string.inventory_reader_open_failed));
            dispatchScannerStateChanged();
            return;
        }
        if (!ensureDemoTidMode()) {
            logRepository.logError("ERROR", "Khong chuyen reader sang mode EPC + TID");
            dispatchScannerError(context.getString(R.string.inventory_reader_mode_failed));
            dispatchScannerStateChanged();
            return;
        }

        demoReader.setInventoryCallback(new IUHFInventoryCallback() {
            @Override
            public void callback(UHFTAGInfo uhftagInfo) {
                processDemoTag(uhftagInfo);
            }
        });

        InventoryParameter parameter = new InventoryParameter();
        parameter.setResultData(new InventoryParameter.ResultData().setNeedPhase(false));
        demoInventoryRunning = demoReader.startInventoryTag(parameter);
        if (!demoInventoryRunning) {
            demoReader.setInventoryCallback(null);
            logRepository.logError("ERROR", "Khong the bat dau inventory UHF kiem ke");
            dispatchScannerError(context.getString(R.string.inventory_reader_inventory_failed));
        }
        dispatchScannerStateChanged();
    }

    private void startSingleRfidScan(Context context) {
        logRepository.logInfo("START_SCAN", "Bat dau scan RFID kiem ke", "1 lan");
        if (!initDemoReaderIfNeeded(context.getApplicationContext())) {
            logRepository.logError("ERROR", "Khong mo duoc dau doc UHF o man kiem ke");
            dispatchScannerError(context.getString(R.string.inventory_reader_open_failed));
            dispatchScannerStateChanged();
            return;
        }
        if (!ensureDemoTidMode()) {
            logRepository.logError("ERROR", "Khong chuyen reader sang mode EPC + TID");
            dispatchScannerError(context.getString(R.string.inventory_reader_mode_failed));
            dispatchScannerStateChanged();
            return;
        }

        UHFTAGInfo tagInfo = demoReader.inventorySingleTag();
        if (tagInfo == null) {
            logRepository.logError("ERROR", "Khong doc duoc the RFID o man kiem ke");
            dispatchScannerError(context.getString(R.string.inventory_reader_read_failed));
            dispatchScannerStateChanged();
            return;
        }
        processDemoTag(tagInfo);
        dispatchRfidSingleScanCompleted();
    }

    private void processDemoTag(UHFTAGInfo tagInfo) {
        if (tagInfo == null) {
            return;
        }
        Log.d(TAG_DEMO_UHF, "[DEMO_UHF] raw callback data=" + tagInfo);

        UhfScanData baseScan = UhfScanData.from(tagInfo);
        String epcHex = normalizeHexValue(baseScan.getEpcHex());
        String tid = sanitizeTidValue(baseScan.getTid());
        String epcAsciiCode = EpcUtils.hexToAscii(epcHex);

        Log.d(TAG_DEMO_UHF, "[DEMO_UHF] epcHex=" + valueOrDash(epcHex));
        Log.d(TAG_DEMO_UHF, "[DEMO_UHF] tid=" + valueOrDash(tid));

        if (tid.isEmpty() && !epcHex.isEmpty()) {
            String tidByReadData = readTidFromDemoFlow(epcHex);
            if (!tidByReadData.isEmpty()) {
                tid = tidByReadData;
                Log.d(TAG_DEMO_UHF, "[DEMO_UHF] tid=" + tid);
            }
        }

        UhfScanData scanData = new UhfScanData(
                tid,
                epcHex,
                epcAsciiCode,
                baseScan.getRssi(),
                baseScan.getPhase(),
                System.currentTimeMillis(),
                tagInfo
        );
        dispatchRfidScanResult(scanData);
    }

    private String readTidFromDemoFlow(String epcHex) {
        synchronized (readerLock) {
            if (demoReader == null || epcHex == null || epcHex.trim().isEmpty()) {
                return "";
            }
            try {
                String tid = demoReader.readData(
                        "00000000",
                        RFIDWithUHFUART.Bank_EPC,
                        32,
                        epcHex.trim().length() * 4,
                        epcHex.trim(),
                        RFIDWithUHFUART.Bank_TID,
                        0,
                        6
                );
                return sanitizeTidValue(tid);
            } catch (Exception exception) {
                Log.e(TAG_DEMO_UHF, "[DEMO_UHF] read TID bank failed", exception);
                return "";
            }
        }
    }

    private boolean ensureBarcodeReady(Context context) {
        if (barcodeService.isReady()) {
            return true;
        }
        boolean ready = barcodeService.acquire(context);
        if (ready) {
            sessionAcquired = true;
            return true;
        }
        barcodeService.release();
        sessionAcquired = false;
        return false;
    }

    private boolean initDemoReaderIfNeeded(Context appContext) {
        synchronized (readerLock) {
            if (demoReaderReady && demoReader != null) {
                return true;
            }
            try {
                demoReader = RFIDWithUHFUART.getInstance();
                if (demoReader == null) {
                    demoReaderReady = false;
                    return false;
                }
                demoReaderReady = demoReader.init(appContext);
                if (demoReaderReady) {
                    previousInventoryMode = demoReader.getEPCAndTIDUserMode();
                }
                Log.d(TAG_DEMO_UHF, "[DEMO_UHF] init reader result=" + demoReaderReady);
                return demoReaderReady;
            } catch (Exception exception) {
                demoReaderReady = false;
                Log.e(TAG_DEMO_UHF, "[DEMO_UHF] init reader failed", exception);
                return false;
            }
        }
    }

    private boolean ensureDemoTidMode() {
        synchronized (readerLock) {
            if (demoReader == null) {
                return false;
            }
            boolean result = demoReader.setEPCAndTIDUserMode(
                    new InventoryModeEntity.Builder()
                            .setMode(InventoryModeEntity.MODE_EPC_TID)
                            .build()
            );
            if (!result) {
                result = demoReader.setEPCAndTIDMode();
            }
            Log.d(TAG_DEMO_UHF, "[DEMO_UHF] ensureEpcTidMode result=" + result);
            return result;
        }
    }

    private void stopDemoInventory() {
        synchronized (readerLock) {
            if (demoReader == null) {
                demoInventoryRunning = false;
                return;
            }
            try {
                if (demoInventoryRunning || demoReader.isInventorying()) {
                    demoReader.stopInventory();
                }
            } catch (Exception exception) {
                Log.e(TAG_DEMO_UHF, "[DEMO_UHF] stop inventory failed", exception);
            }
            demoReader.setInventoryCallback(null);
            demoInventoryRunning = false;
        }
    }

    private void releaseBarcodeSession() {
        if (!sessionAcquired) {
            return;
        }
        barcodeService.release();
        sessionAcquired = false;
    }

    private void releaseDemoReader() {
        synchronized (readerLock) {
            if (demoReader == null) {
                demoReaderReady = false;
                return;
            }
            stopDemoInventory();
            try {
                if (previousInventoryMode != null) {
                    demoReader.setEPCAndTIDUserMode(previousInventoryMode);
                }
            } catch (Exception exception) {
                Log.e(TAG_DEMO_UHF, "[DEMO_UHF] restore inventory mode failed", exception);
            }
            try {
                demoReader.free();
            } catch (Exception exception) {
                Log.e(TAG_DEMO_UHF, "[DEMO_UHF] free reader failed", exception);
            }
            demoReader = null;
            demoReaderReady = false;
            previousInventoryMode = null;
        }
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

    public static String buildOutsideRfidKey(String tid, String epcHex) {
        return RfidTagNormalizer.buildOutsideRfidKey(tid, epcHex);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
