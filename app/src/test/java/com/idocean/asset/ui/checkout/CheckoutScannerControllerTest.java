package com.idocean.asset.ui.checkout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CheckoutScannerControllerTest {
    @Test
    public void sanitizeTidValue_returnsEmptyForZeroTid() {
        assertEquals("", CheckoutScannerController.sanitizeTidValue("0000000000000000"));
        assertEquals("", CheckoutScannerController.sanitizeTidValue("000000000000000000000000"));
    }

    @Test
    public void sanitizeTidValue_trimsAndUppercasesTid() {
        assertEquals("ABC123", CheckoutScannerController.sanitizeTidValue("  abc123  "));
    }

    @Test
    public void normalizeHexValue_trimsAndUppercasesValue() {
        assertEquals("A1B2", CheckoutScannerController.normalizeHexValue("  a1b2  "));
    }
}
