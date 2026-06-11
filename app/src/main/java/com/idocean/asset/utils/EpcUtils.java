package com.idocean.asset.utils;

import java.nio.charset.StandardCharsets;

/**
 * Tien ich xu ly EPC, chi decode hien thi, khong dung de match nghiep vu RFID.
 */
public final class EpcUtils {

    private EpcUtils() {
    }

    public static String hexToAscii(String epcHex) {
        if (epcHex == null) {
            return "";
        }

        String normalizedHex = epcHex.trim();
        if (normalizedHex.isEmpty() || normalizedHex.length() % 2 != 0) {
            return "";
        }

        int byteCount = normalizedHex.length() / 2;
        byte[] bytes = new byte[byteCount];
        try {
            for (int index = 0; index < byteCount; index++) {
                int start = index * 2;
                bytes[index] = (byte) Integer.parseInt(normalizedHex.substring(start, start + 2), 16);
            }
        } catch (NumberFormatException exception) {
            return "";
        }

        return new String(bytes, StandardCharsets.US_ASCII).replace("\u0000", "").trim();
    }
}
