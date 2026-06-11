package com.idocean.asset.ui.inventory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InventoryScannerControllerTest {
    @Test
    public void sanitizeTidValue_returnsEmptyForZeroTid() {
        assertEquals("", InventoryScannerController.sanitizeTidValue("0000000000000000"));
        assertEquals("", InventoryScannerController.sanitizeTidValue("000000000000000000000000"));
    }

    @Test
    public void sanitizeTidValue_trimsAndUppercasesTid() {
        assertEquals("ABC123", InventoryScannerController.sanitizeTidValue("  abc123  "));
    }

    @Test
    public void buildOutsideRfidKey_prefersTidThenEpcThenUnknown() {
        assertEquals("OUTSIDE_RFID:TID:TID001", InventoryScannerController.buildOutsideRfidKey(" tid001 ", "epc"));
        assertEquals("OUTSIDE_RFID:EPC:EPC001", InventoryScannerController.buildOutsideRfidKey("", " epc001 "));
        assertEquals("OUTSIDE_RFID:UNKNOWN", InventoryScannerController.buildOutsideRfidKey("", ""));
    }
}
