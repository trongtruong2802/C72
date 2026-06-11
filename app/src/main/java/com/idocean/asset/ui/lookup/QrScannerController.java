package com.idocean.asset.ui.lookup;

import android.content.Context;

import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.barcode.ChainwayBarcodeService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class QrScannerController implements BarcodeScannerListener {
    private final ChainwayBarcodeService barcodeService;
    private final ExecutorService executor;
    private final LookupScannerCallback callback;
    private volatile boolean started;
    private boolean sessionAcquired;

    public QrScannerController(LookupScannerCallback callback) {
        this(callback, ChainwayBarcodeService.getInstance(), Executors.newSingleThreadExecutor());
    }

    QrScannerController(LookupScannerCallback callback, ChainwayBarcodeService barcodeService, ExecutorService executor) {
        this.callback = callback;
        this.barcodeService = barcodeService;
        this.executor = executor;
    }

    public void onStart() {
        started = true;
        barcodeService.registerListener(this);
    }

    public void onStop() {
        started = false;
        stopScan();
        if (sessionAcquired) {
            barcodeService.release();
            sessionAcquired = false;
        }
        barcodeService.unregisterListener(this);
    }

    public void shutdown() {
        onStop();
        executor.shutdownNow();
    }

    public void startScan(Context context) {
        if (!started || callback == null || context == null) {
            return;
        }

        if (barcodeService.isReady()) {
            if (!started) {
                return;
            }
            if (!barcodeService.startScan()) {
                callback.onQrScannerBusy();
            }
            return;
        }

        callback.onScannerPreparingChanged(true);
        executor.execute(() -> {
            if (!started) {
                return;
            }
            boolean ready = ensureBarcodeReady(context.getApplicationContext());
            if (!started) {
                if (sessionAcquired) {
                    barcodeService.release();
                    sessionAcquired = false;
                }
                return;
            }

            callback.onScannerPreparingChanged(false);
            if (!ready) {
                callback.onQrScannerUnavailable();
                return;
            }

            if (!started) {
                return;
            }
            if (!barcodeService.startScan()) {
                callback.onQrScannerBusy();
            }
        });
    }

    public boolean stopScan() {
        boolean wasScanning = barcodeService.isScanning();
        barcodeService.stopScan();
        return wasScanning;
    }

    public boolean isReady() {
        return barcodeService.isReady();
    }

    public boolean isScanning() {
        return barcodeService.isScanning();
    }

    @Override
    public void onBarcodeScanned(String code, long timestamp) {
        if (!started || callback == null) {
            return;
        }
        callback.onScanResult(ScanResult.qr(code, timestamp));
    }

    @Override
    public void onBarcodeScanError(String message) {
        if (!started || callback == null) {
            return;
        }
        callback.onScannerError(message);
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
}
