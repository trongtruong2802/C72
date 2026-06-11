package com.idocean.asset.ui.lookup;

public interface LookupScannerCallback {
    void onScannerPreparingChanged(boolean preparing);

    void onScanResult(ScanResult result);

    void onScannerError(String message);

    void onQrScannerUnavailable();

    void onQrScannerBusy();
}
