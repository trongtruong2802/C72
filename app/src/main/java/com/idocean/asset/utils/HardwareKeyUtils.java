package com.idocean.asset.utils;

/**
 * Gom nhóm các keycode trigger scanner của thiết bị Chainway.
 */
public final class HardwareKeyUtils {

    private static final int[] SCANNER_KEYS = new int[]{
            139, 280, 291, 293, 294, 311, 312, 313, 315, 591, 593, 594, 595, 596
    };

    private HardwareKeyUtils() {
    }

    public static boolean isScannerTrigger(int keyCode) {
        for (int supportedKey : SCANNER_KEYS) {
            if (supportedKey == keyCode) {
                return true;
            }
        }
        return false;
    }
}
