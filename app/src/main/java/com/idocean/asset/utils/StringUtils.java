package com.idocean.asset.utils;

import java.util.Locale;

public final class StringUtils {
    private StringUtils() {
    }

    public static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
