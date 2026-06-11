package com.idocean.asset.scanner.rfid;

/**
 * Fragment nào cần nhận trigger cứng sẽ implement interface này.
 */
public interface ScannerTriggerHandler {
    default void onScannerTrigger() {
        onScannerTriggerDown();
    }

    default void onScannerTriggerDown() {
    }

    default void onScannerTriggerUp() {
    }
}
