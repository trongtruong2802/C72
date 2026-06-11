package com.idocean.asset.ui.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScanResultTest {
    @Test
    public void qrResult_keepsCodeAsRawValue() {
        ScanResult result = ScanResult.qr("  QR-123  ", 1234L);

        assertEquals(ScanResult.ScannerType.QR, result.getScannerType());
        assertEquals("QR-123", result.getCode());
        assertEquals("QR-123", result.getRawValue());
        assertEquals(1234L, result.getTimestamp());
        assertTrue(result.isQr());
        assertFalse(result.isRfid());
    }

    @Test
    public void rfidResult_keepsTidAsRawValue() {
        ScanResult result = ScanResult.rfid("  e2801190abcd  ", " EPC-CODE ", 5678L);

        assertEquals(ScanResult.ScannerType.RFID, result.getScannerType());
        assertEquals("E2801190ABCD", result.getTid());
        assertEquals("EPC-CODE", result.getCode());
        assertEquals("E2801190ABCD", result.getRawValue());
        assertEquals(5678L, result.getTimestamp());
        assertFalse(result.isQr());
        assertTrue(result.isRfid());
    }
}
