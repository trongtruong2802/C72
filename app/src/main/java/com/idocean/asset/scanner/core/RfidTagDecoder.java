package com.idocean.asset.scanner.core;

import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.scanner.rfid.UhfScanData;
import com.idocean.asset.utils.EpcUtils;
import com.rscja.deviceapi.entity.UHFTAGInfo;

public final class RfidTagDecoder {

    private RfidTagDecoder() {
    }

    public interface TidFallbackReader {
        String readTid(String epcHex);
    }

    public static DecodedTag decode(UHFTAGInfo tagInfo, TidFallbackReader tidFallbackReader) {
        UhfScanData baseScan = UhfScanData.from(tagInfo);
        String epcHex = RfidTagNormalizer.normalizeHex(baseScan.getEpcHex());
        String tid = RfidTagNormalizer.sanitizeTid(baseScan.getTid());
        String code = safe(baseScan.getEpcAsciiCode());
        if (tid.isEmpty() && !epcHex.isEmpty() && tidFallbackReader != null) {
            tid = RfidTagNormalizer.sanitizeTid(tidFallbackReader.readTid(epcHex));
        }
        if (code.isEmpty()) {
            code = EpcUtils.hexToAscii(epcHex);
        }
        return new DecodedTag(
                tid,
                code,
                epcHex,
                safe(baseScan.getRssi()),
                baseScan.getPhase(),
                System.currentTimeMillis(),
                tagInfo
        );
    }

    public static final class DecodedTag {
        private final String tid;
        private final String code;
        private final String epcHex;
        private final String rssi;
        private final int phase;
        private final long scannedAt;
        private final UHFTAGInfo rawTagInfo;

        DecodedTag(String tid, String code, String epcHex, String rssi, int phase, long scannedAt, UHFTAGInfo rawTagInfo) {
            this.tid = safe(tid);
            this.code = safe(code);
            this.epcHex = safe(epcHex);
            this.rssi = safe(rssi);
            this.phase = phase;
            this.scannedAt = scannedAt;
            this.rawTagInfo = rawTagInfo;
        }

        public String getTid() {
            return tid;
        }

        public String getCode() {
            return code;
        }

        public String getEpcHex() {
            return epcHex;
        }

        public long getScannedAt() {
            return scannedAt;
        }

        public UhfScanData toUhfScanData() {
            return new UhfScanData(tid, epcHex, code, rssi, phase, scannedAt, rawTagInfo);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
