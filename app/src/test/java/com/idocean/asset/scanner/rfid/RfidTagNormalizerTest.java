package com.idocean.asset.scanner.rfid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RfidTagNormalizerTest {
    @Test
    public void sanitizeTid_stripsZeroValues() {
        assertEquals("", RfidTagNormalizer.sanitizeTid("0000000000000000"));
        assertEquals("", RfidTagNormalizer.sanitizeTid("000000000000000000000000"));
    }

    @Test
    public void sanitizeTid_trimsAndUppercases() {
        assertEquals("ABC123", RfidTagNormalizer.sanitizeTid("  abc123  "));
    }

    @Test
    public void normalizeHex_trimsAndUppercases() {
        assertEquals("A1B2", RfidTagNormalizer.normalizeHex("  a1b2  "));
    }

    @Test
    public void buildOutsideRfidKey_prefersTidThenEpcThenUnknown() {
        assertEquals("OUTSIDE_RFID:TID:TID001", RfidTagNormalizer.buildOutsideRfidKey(" tid001 ", "epc"));
        assertEquals("OUTSIDE_RFID:EPC:EPC001", RfidTagNormalizer.buildOutsideRfidKey("", " epc001 "));
        assertEquals("OUTSIDE_RFID:UNKNOWN", RfidTagNormalizer.buildOutsideRfidKey("", ""));
    }
}
