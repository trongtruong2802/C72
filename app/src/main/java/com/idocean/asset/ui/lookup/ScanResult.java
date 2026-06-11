package com.idocean.asset.ui.lookup;

import java.util.Locale;

public final class ScanResult {
    public enum ScannerType {
        QR,
        RFID
    }

    private final ScannerType scannerType;
    private final String tid;
    private final String code;
    private final String rawValue;
    private final long timestamp;

    private ScanResult(ScannerType scannerType, String tid, String code, String rawValue, long timestamp) {
        this.scannerType = scannerType;
        this.tid = valueOrEmpty(tid);
        this.code = valueOrEmpty(code);
        this.rawValue = valueOrEmpty(rawValue);
        this.timestamp = timestamp;
    }

    public static ScanResult qr(String code, long timestamp) {
        String safeCode = valueOrEmpty(code).trim();
        return new ScanResult(ScannerType.QR, "", safeCode, safeCode, timestamp);
    }

    public static ScanResult rfid(String tid, String code, long timestamp) {
        String safeTid = valueOrEmpty(tid).trim().toUpperCase(Locale.ROOT);
        String safeCode = valueOrEmpty(code).trim();
        return new ScanResult(ScannerType.RFID, safeTid, safeCode, safeTid, timestamp);
    }

    public ScannerType getScannerType() {
        return scannerType;
    }

    public String getTid() {
        return tid;
    }

    public String getCode() {
        return code;
    }

    public String getRawValue() {
        return rawValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isQr() {
        return scannerType == ScannerType.QR;
    }

    public boolean isRfid() {
        return scannerType == ScannerType.RFID;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
