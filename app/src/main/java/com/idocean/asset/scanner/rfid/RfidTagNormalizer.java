package com.idocean.asset.scanner.rfid;

import java.util.Locale;

public final class RfidTagNormalizer {
    private static final String ZERO_TID_16 = "0000000000000000";
    private static final String ZERO_TID_24 = "000000000000000000000000";

    private RfidTagNormalizer() {
    }

    public static String normalizeHex(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static String sanitizeTid(String value) {
        String normalized = normalizeHex(value);
        if (ZERO_TID_16.equals(normalized) || ZERO_TID_24.equals(normalized)) {
            return "";
        }
        return normalized;
    }

    public static String buildOutsideRfidKey(String tid, String epcHex) {
        String normalizedTid = normalizeHex(tid);
        if (!normalizedTid.isEmpty()) {
            return "OUTSIDE_RFID:TID:" + normalizedTid;
        }
        String normalizedEpc = normalizeHex(epcHex);
        if (!normalizedEpc.isEmpty()) {
            return "OUTSIDE_RFID:EPC:" + normalizedEpc;
        }
        return "OUTSIDE_RFID:UNKNOWN";
    }
}
