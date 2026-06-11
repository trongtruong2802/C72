package com.idocean.asset.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tap trung chuan hoa cac truong hien thi chung cua asset.
 */
public final class AssetFieldNormalizer {
    private static final String TT16 = "TT16";
    private static final String BASEMENT_TT16 = "T\u1ea7ng B - TT16";
    private static final String GROUND_TT16 = "T\u1ea7ng G - TT16";
    private static final String FLOOR_PREFIX = "L\u1ea7u ";
    private static final String FLOOR_SUFFIX_TT16 = " - TT16";
    private static final String FLOOR_SUFFIX_TT17 = " - TT17";

    private static final Pattern IDOPLEX_FLOOR_PATTERN =
            Pattern.compile("^idoplex\\s*-\\s*([1-6])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TT16_FLOOR_PATTERN =
            Pattern.compile("^l\\u1ea7u\\s*([1-6])\\s*-\\s*tt16$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TT17_FLOOR_PATTERN =
            Pattern.compile("^l\\u1ea7u\\s*([1-6])\\s*-\\s*tt17$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASCII_TT16_FLOOR_PATTERN =
            Pattern.compile("^lau\\s*([1-6])\\s*-\\s*tt16$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASCII_TT17_FLOOR_PATTERN =
            Pattern.compile("^lau\\s*([1-6])\\s*-\\s*tt17$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASCII_GROUND_TT16_PATTERN =
            Pattern.compile("^tang\\s*g\\s*-\\s*tt16$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASCII_BASEMENT_TT16_PATTERN =
            Pattern.compile("^tang\\s*b\\s*-\\s*tt16$", Pattern.CASE_INSENSITIVE);

    private AssetFieldNormalizer() {
    }

    public static String normalizeDepartmentForDisplay(String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return "";
        }

        if (safeValue.equalsIgnoreCase("AD")) {
            return "AD";
        }
        if (safeValue.equalsIgnoreCase("BID")) {
            return "BID";
        }
        if (safeValue.equalsIgnoreCase("BOD")) {
            return "BOD";
        }
        if (safeValue.equalsIgnoreCase("HR")) {
            return "HR";
        }
        if (safeValue.equalsIgnoreCase("IT")) {
            return "IT";
        }
        if (safeValue.equalsIgnoreCase("PD")) {
            return "PD";
        }
        if (safeValue.equalsIgnoreCase("QA")) {
            return "QA";
        }
        if (safeValue.equalsIgnoreCase("R&D")
                || safeValue.equalsIgnoreCase("R & D")
                || safeValue.equalsIgnoreCase("R and D")) {
            return "R&D";
        }
        if (safeValue.equalsIgnoreCase("HR & Admin")
                || safeValue.equalsIgnoreCase("HR and Admin")) {
            return "HR & Admin";
        }
        if (safeValue.equalsIgnoreCase("Finance & Accountant")
                || safeValue.equalsIgnoreCase("Finance and Accountant")) {
            return "Finance & Accountant";
        }
        if (safeValue.equalsIgnoreCase("Sales & Marketing")
                || safeValue.equalsIgnoreCase("Sales and Marketing")) {
            return "Sales & Marketing";
        }
        if (safeValue.equalsIgnoreCase("Operation")) {
            return "Operation";
        }
        if (safeValue.equalsIgnoreCase("Procurement")) {
            return "Procurement";
        }
        if (safeValue.equalsIgnoreCase("Production")) {
            return "Production";
        }
        if (safeValue.equalsIgnoreCase("Technical")) {
            return "Technical";
        }

        return safeValue;
    }

    public static String normalizeLocationForDisplay(String value) {
        String safeValue = repairLegacyEncoding(safe(value));
        if (safeValue.isEmpty()) {
            return "";
        }

        if (safeValue.equalsIgnoreCase("Idoplex")) {
            return TT16;
        }
        if (safeValue.equalsIgnoreCase("Idoplex-G")
                || safeValue.equalsIgnoreCase("Idoplex - G")
                || safeValue.equalsIgnoreCase(GROUND_TT16)) {
            return GROUND_TT16;
        }
        if (ASCII_GROUND_TT16_PATTERN.matcher(safeValue).matches()) {
            return GROUND_TT16;
        }
        if (safeValue.equalsIgnoreCase("Idoplex-B")
                || safeValue.equalsIgnoreCase("Idoplex - B")
                || safeValue.equalsIgnoreCase(BASEMENT_TT16)) {
            return BASEMENT_TT16;
        }
        if (ASCII_BASEMENT_TT16_PATTERN.matcher(safeValue).matches()) {
            return BASEMENT_TT16;
        }

        Matcher idoplexFloorMatcher = IDOPLEX_FLOOR_PATTERN.matcher(safeValue);
        if (idoplexFloorMatcher.matches()) {
            return buildFloorLabel(idoplexFloorMatcher.group(1));
        }

        Matcher tt16FloorMatcher = TT16_FLOOR_PATTERN.matcher(safeValue);
        if (tt16FloorMatcher.matches()) {
            return buildFloorLabel(tt16FloorMatcher.group(1));
        }
        Matcher asciiTt16FloorMatcher = ASCII_TT16_FLOOR_PATTERN.matcher(safeValue);
        if (asciiTt16FloorMatcher.matches()) {
            return buildFloorLabel(asciiTt16FloorMatcher.group(1));
        }

        Matcher tt17FloorMatcher = TT17_FLOOR_PATTERN.matcher(safeValue);
        if (tt17FloorMatcher.matches()) {
            return buildFloorLabel(tt17FloorMatcher.group(1));
        }
        Matcher asciiTt17FloorMatcher = ASCII_TT17_FLOOR_PATTERN.matcher(safeValue);
        if (asciiTt17FloorMatcher.matches()) {
            return buildFloorLabel(asciiTt17FloorMatcher.group(1));
        }

        return safeValue;
    }

    public static String normalizeAssetTypeForDisplay(String value) {
        return safe(value);
    }

    public static String normalizeAssetTypeForFilter(String value) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) {
            return "";
        }
        return safeValue.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    public static String normalizeInventoryStatusForDisplay(String value) {
        return safe(value);
    }

    public static String normalizeConditionForDisplay(String value) {
        return safe(value);
    }

    public static List<String> normalizeDisplayValues(Set<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : new LinkedHashSet<>(values)) {
            String safeValue = safe(value);
            if (!safeValue.isEmpty()) {
                result.add(safeValue);
            }
        }
        return result;
    }

    private static String buildFloorLabel(String floor) {
        return FLOOR_PREFIX + floor + FLOOR_SUFFIX_TT16;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String repairLegacyEncoding(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("TÃ¡ÂºÂ§ng", "Táº§ng")
                .replace("LÃ¡ÂºÂ§u", "Láº§u");
    }
}
