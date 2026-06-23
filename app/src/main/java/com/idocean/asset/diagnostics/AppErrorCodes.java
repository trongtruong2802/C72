package com.idocean.asset.diagnostics;

public final class AppErrorCodes {
    public static final String QR_OPEN_FAILED = "QR_OPEN_FAILED";
    public static final String QR_SCAN_FAILED = "QR_SCAN_FAILED";
    public static final String RFID_INIT_FAILED = "RFID_INIT_FAILED";
    public static final String RFID_MODE_FAILED = "RFID_MODE_FAILED";
    public static final String RFID_SINGLE_SCAN_FAILED = "RFID_SINGLE_SCAN_FAILED";
    public static final String RFID_CONTINUOUS_SCAN_FAILED = "RFID_CONTINUOUS_SCAN_FAILED";
    public static final String CACHE_READ_FAILED = "CACHE_READ_FAILED";
    public static final String CACHE_WRITE_FAILED = "CACHE_WRITE_FAILED";
    public static final String CACHE_PARSE_FAILED = "CACHE_PARSE_FAILED";
    public static final String UI_RENDER_FAILED = "UI_RENDER_FAILED";

    private AppErrorCodes() {
    }
}
