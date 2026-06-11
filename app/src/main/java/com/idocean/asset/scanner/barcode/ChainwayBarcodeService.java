package com.idocean.asset.scanner.barcode;

import android.content.Context;
import android.util.Log;

import com.rscja.barcode.BarcodeDecoder;
import com.rscja.barcode.BarcodeFactory;
import com.rscja.deviceapi.entity.BarcodeEntity;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Lop boc mong bam sat demo 2D goc cua Chainway:
 * BarcodeFactory -> BarcodeDecoder -> open/startScan/stopScan/DecodeCallback/close.
 */
public class ChainwayBarcodeService {
    private static final String TAG = "DEMO_2D";

    private static ChainwayBarcodeService instance;

    private final Set<BarcodeScannerListener> listeners = new CopyOnWriteArraySet<>();
    private BarcodeDecoder barcodeDecoder;
    private boolean opened;
    private boolean scanning;
    private int acquireCount;

    private ChainwayBarcodeService() {
    }

    public static synchronized ChainwayBarcodeService getInstance() {
        if (instance == null) {
            instance = new ChainwayBarcodeService();
        }
        return instance;
    }

    public synchronized boolean acquire(Context context) {
        acquireCount++;
        try {
            if (barcodeDecoder == null) {
                barcodeDecoder = BarcodeFactory.getInstance().getBarcodeDecoder();
            }
            if (barcodeDecoder == null) {
                opened = false;
                return false;
            }
            if (!opened) {
                barcodeDecoder.open(context);
                barcodeDecoder.setDecodeCallback(new BarcodeDecoder.DecodeCallback() {
                    @Override
                    public void onDecodeComplete(BarcodeEntity barcodeEntity) {
                        handleDecodeResult(barcodeEntity);
                    }
                });
                opened = true;
                Log.d(TAG, "[DEMO_2D] scanner initialized");
            }
            return true;
        } catch (Exception exception) {
            opened = false;
            Log.e(TAG, "[DEMO_2D] scanner init failed", exception);
            return false;
        }
    }

    public synchronized void release() {
        acquireCount = Math.max(0, acquireCount - 1);
        if (acquireCount > 0) {
            return;
        }
        stopScan();
        if (barcodeDecoder != null && opened) {
            try {
                barcodeDecoder.close();
            } catch (Exception exception) {
                Log.e(TAG, "[DEMO_2D] scanner close failed", exception);
            }
        }
        opened = false;
        scanning = false;
    }

    public void registerListener(BarcodeScannerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(BarcodeScannerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public synchronized boolean isReady() {
        return barcodeDecoder != null && opened;
    }

    public synchronized boolean isScanning() {
        return scanning;
    }

    public synchronized boolean startScan() {
        if (!isReady() || scanning) {
            return false;
        }
        try {
            Log.d(TAG, "[DEMO_2D] trigger received");
            scanning = true;
            barcodeDecoder.startScan();
            return true;
        } catch (Exception exception) {
            scanning = false;
            Log.e(TAG, "[DEMO_2D] startScan failed", exception);
            dispatchError("Khong the bat dau quet QR/2D.");
            return false;
        }
    }

    public synchronized void stopScan() {
        if (barcodeDecoder == null) {
            scanning = false;
            return;
        }
        try {
            barcodeDecoder.stopScan();
        } catch (Exception exception) {
            Log.e(TAG, "[DEMO_2D] stopScan failed", exception);
        }
        scanning = false;
    }

    public String getStatusMessage() {
        return isReady() ? "Scanner QR/2D Chainway da san sang." : "Scanner QR/2D chua mo.";
    }

    private void handleDecodeResult(BarcodeEntity barcodeEntity) {
        synchronized (this) {
            scanning = false;
        }
        if (barcodeEntity == null) {
            Log.d(TAG, "[DEMO_2D] raw scan result=null");
            dispatchError("Khong nhan duoc du lieu QR/2D.");
            return;
        }

        String rawData = barcodeEntity.getBarcodeData();
        Log.d(TAG, "[DEMO_2D] raw scan result=" + rawData);
        if (barcodeEntity.getResultCode() == BarcodeDecoder.DECODE_SUCCESS
                && rawData != null
                && !rawData.trim().isEmpty()) {
            dispatchCode(rawData.trim());
        } else {
            dispatchError("Khong doc duoc ma QR/2D.");
        }
    }

    private void dispatchCode(String code) {
        long timestamp = System.currentTimeMillis();
        for (BarcodeScannerListener listener : listeners) {
            listener.onBarcodeScanned(code, timestamp);
        }
    }

    private void dispatchError(String message) {
        for (BarcodeScannerListener listener : listeners) {
            listener.onBarcodeScanError(message);
        }
    }
}
