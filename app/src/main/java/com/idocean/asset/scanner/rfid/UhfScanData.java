package com.idocean.asset.scanner.rfid;

import com.idocean.asset.utils.EpcUtils;
import com.rscja.deviceapi.entity.UHFTAGInfo;

/**
 * Du lieu doc duoc tu UHF, uu tien TID de match nghiep vu.
 */
public class UhfScanData {
    private final String tid;
    private final String epcHex;
    private final String epcAsciiCode;
    private final String rssi;
    private final int phase;
    private final long scannedAt;
    private final UHFTAGInfo rawTagInfo;

    public UhfScanData(String tid, String epcHex, String epcAsciiCode, String rssi,
                       int phase, long scannedAt, UHFTAGInfo rawTagInfo) {
        this.tid = safe(tid);
        this.epcHex = safe(epcHex);
        this.epcAsciiCode = safe(epcAsciiCode);
        this.rssi = safe(rssi);
        this.phase = phase;
        this.scannedAt = scannedAt;
        this.rawTagInfo = rawTagInfo;
    }

    public static UhfScanData from(UHFTAGInfo tagInfo) {
        String epcHex = tagInfo == null ? "" : tagInfo.getEPC();
        String tid = tagInfo == null ? "" : tagInfo.getTid();
        if ((tid == null || tid.trim().isEmpty()) && tagInfo != null && tagInfo.getTidBytes() != null) {
            tid = bytesToHex(tagInfo.getTidBytes());
        }
        return new UhfScanData(
                tid,
                epcHex,
                EpcUtils.hexToAscii(epcHex),
                tagInfo == null ? "" : tagInfo.getRssi(),
                tagInfo == null ? 0 : tagInfo.getPhase(),
                System.currentTimeMillis(),
                tagInfo
        );
    }

    public String getTid() {
        return tid;
    }

    public String getEpcHex() {
        return epcHex;
    }

    public String getEpcAsciiCode() {
        return epcAsciiCode;
    }

    public String getRssi() {
        return rssi;
    }

    public int getPhase() {
        return phase;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public long getTimestamp() {
        return scannedAt;
    }

    public UHFTAGInfo getRawTagInfo() {
        return rawTagInfo;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String bytesToHex(byte[] value) {
        if (value == null || value.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format("%02X", item));
        }
        return builder.toString();
    }
}
