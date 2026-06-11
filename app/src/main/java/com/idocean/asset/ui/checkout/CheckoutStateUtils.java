package com.idocean.asset.ui.checkout;

import java.util.Locale;

final class CheckoutStateUtils {
    private CheckoutStateUtils() {
    }

    static String normalizeKey(String value) {
        return safe(value).trim().toUpperCase(Locale.ROOT);
    }

    static String safe(String value) {
        return value == null ? "" : value;
    }
}
