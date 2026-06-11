package com.idocean.asset.scanner.barcode;

public interface BarcodeScannerListener {
    void onBarcodeScanned(String code, long timestamp);

    void onBarcodeScanError(String message);
}
