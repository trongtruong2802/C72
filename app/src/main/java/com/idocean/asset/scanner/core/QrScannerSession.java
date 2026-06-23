package com.idocean.asset.scanner.core;

import android.content.Context;

import com.idocean.asset.scanner.barcode.BarcodeScannerListener;
import com.idocean.asset.scanner.barcode.ChainwayBarcodeService;

public final class QrScannerSession {
    private final ChainwayBarcodeService barcodeService;
    private boolean sessionAcquired;

    public QrScannerSession() {
        this(ChainwayBarcodeService.getInstance());
    }

    QrScannerSession(ChainwayBarcodeService barcodeService) {
        this.barcodeService = barcodeService == null ? ChainwayBarcodeService.getInstance() : barcodeService;
    }

    public void registerListener(BarcodeScannerListener listener) {
        barcodeService.registerListener(listener);
    }

    public void unregisterListener(BarcodeScannerListener listener) {
        barcodeService.unregisterListener(listener);
    }

    public boolean ensureReady(Context context) {
        if (barcodeService.isReady()) {
            return true;
        }
        boolean ready = barcodeService.acquire(context == null ? null : context.getApplicationContext());
        if (ready) {
            sessionAcquired = true;
            return true;
        }
        barcodeService.release();
        sessionAcquired = false;
        return false;
    }

    public boolean startScan() {
        return barcodeService.startScan();
    }

    public boolean stopScan() {
        boolean wasScanning = barcodeService.isScanning();
        barcodeService.stopScan();
        return wasScanning;
    }

    public void releaseSession() {
        if (!sessionAcquired) {
            return;
        }
        barcodeService.release();
        sessionAcquired = false;
    }

    public boolean isReady() {
        return barcodeService.isReady();
    }

    public boolean isScanning() {
        return barcodeService.isScanning();
    }

    public String getStatusMessage() {
        return barcodeService.getStatusMessage();
    }
}
