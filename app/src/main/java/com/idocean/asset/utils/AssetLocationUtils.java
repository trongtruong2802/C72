package com.idocean.asset.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Chuan hoa ten vi tri de hien thi dropdown, doi alias/raw value thanh key on dinh cho sync
 * va van giu tap alias legacy khi can rollback.
 */
public final class AssetLocationUtils {
    private static final String TT16 = "TT16";
    private static final String BASEMENT_TT16 = "T\u1ea7ng B - TT16";
    private static final String GROUND_TT16 = "T\u1ea7ng G - TT16";
    private static final String FLOOR_PREFIX = "L\u1ea7u ";
    private static final String FLOOR_SUFFIX_TT16 = " - TT16";
    private static final String FLOOR_SUFFIX_TT17 = " - TT17";

    private static final List<LocationDescriptor> KNOWN_LOCATIONS = buildKnownLocations();

    private AssetLocationUtils() {
    }

    public static String normalizeLocationForDisplay(String value) {
        String rawValue = repairLegacyEncoding(safe(value));
        if (rawValue.isEmpty()) {
            return "";
        }
        LocationDescriptor descriptor = resolveDescriptor(rawValue);
        if (descriptor != null) {
            return descriptor.displayValue;
        }
        return AssetFieldNormalizer.normalizeLocationForDisplay(rawValue);
    }

    public static String resolveLocationKey(String value) {
        String rawValue = repairLegacyEncoding(safe(value));
        if (rawValue.isEmpty()) {
            return "";
        }
        LocationDescriptor descriptor = resolveDescriptor(rawValue);
        if (descriptor != null) {
            return descriptor.key;
        }
        String displayValue = AssetFieldNormalizer.normalizeLocationForDisplay(rawValue);
        return toKey(displayValue.isEmpty() ? rawValue : displayValue);
    }

    public static List<String> resolveLocationQueryAliases(String value) {
        String rawValue = repairLegacyEncoding(safe(value));
        if (rawValue.isEmpty()) {
            return new ArrayList<>();
        }

        LocationDescriptor descriptor = resolveDescriptor(rawValue);
        Set<String> aliases = new LinkedHashSet<>();
        if (descriptor != null) {
            aliases.add(descriptor.displayValue);
            aliases.addAll(descriptor.aliases);
            aliases.add(rawValue);
            return filterBlank(aliases);
        }

        String displayValue = AssetFieldNormalizer.normalizeLocationForDisplay(rawValue);
        if (!displayValue.isEmpty()) {
            aliases.add(displayValue);
        }
        aliases.add(rawValue);
        return filterBlank(aliases);
    }

    private static LocationDescriptor resolveDescriptor(String value) {
        String comparable = normalizeComparable(value);
        if (comparable.isEmpty()) {
            return null;
        }
        for (LocationDescriptor descriptor : KNOWN_LOCATIONS) {
            if (descriptor.matches(comparable)) {
                return descriptor;
            }
        }
        return null;
    }

    private static List<LocationDescriptor> buildKnownLocations() {
        List<LocationDescriptor> descriptors = new ArrayList<>();
        descriptors.add(new LocationDescriptor("TT16", TT16, Arrays.asList("Idoplex", TT16)));
        descriptors.add(new LocationDescriptor(
                "TT16_B",
                BASEMENT_TT16,
                Arrays.asList(BASEMENT_TT16, "Tang B - TT16", "Idoplex - B", "Idoplex-B")
        ));
        descriptors.add(new LocationDescriptor(
                "TT16_G",
                GROUND_TT16,
                Arrays.asList(GROUND_TT16, "Tang G - TT16", "Idoplex - G", "Idoplex-G")
        ));
        for (int floor = 1; floor <= 6; floor++) {
            descriptors.add(new LocationDescriptor(
                    "TT16_F" + floor,
                    FLOOR_PREFIX + floor + FLOOR_SUFFIX_TT16,
                    buildFloorAliases(floor)
            ));
        }
        descriptors.add(new LocationDescriptor("LA_FACTORY", "LA.Factory", Arrays.asList("LA.Factory", "LA Factory")));
        descriptors.add(new LocationDescriptor("WAREHOUSE", "Warehouse", Collections.singletonList("Warehouse")));
        return Collections.unmodifiableList(descriptors);
    }

    private static List<String> buildFloorAliases(int floor) {
        List<String> aliases = new ArrayList<>();
        aliases.add(FLOOR_PREFIX + floor + FLOOR_SUFFIX_TT16);
        aliases.add("Lau " + floor + FLOOR_SUFFIX_TT16);
        aliases.add("Idoplex - " + floor);
        aliases.add("Idoplex-" + floor);
        if (floor == 5) {
            aliases.add(FLOOR_PREFIX + floor + FLOOR_SUFFIX_TT17);
            aliases.add("Lau " + floor + FLOOR_SUFFIX_TT17);
        }
        return aliases;
    }

    private static List<String> filterBlank(Set<String> aliases) {
        List<String> values = new ArrayList<>();
        for (String alias : aliases) {
            String safeAlias = safe(alias);
            if (!safeAlias.isEmpty()) {
                values.add(safeAlias);
            }
        }
        return values;
    }

    private static String normalizeComparable(String value) {
        String rawValue = repairLegacyEncoding(safe(value));
        if (rawValue.isEmpty()) {
            return "";
        }
        String displayValue = AssetFieldNormalizer.normalizeLocationForDisplay(rawValue);
        String normalized = displayValue.isEmpty() ? rawValue : displayValue;
        return toKey(normalized);
    }

    private static String toKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized == null ? "" : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String repairLegacyEncoding(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("TÃƒÂ¡Ã‚ÂºÃ‚Â§ng", "TÃ¡ÂºÂ§ng")
                .replace("LÃƒÂ¡Ã‚ÂºÃ‚Â§u", "LÃ¡ÂºÂ§u");
    }

    private static final class LocationDescriptor {
        private final String key;
        private final String displayValue;
        private final List<String> aliases;
        private final Set<String> comparableValues = new LinkedHashSet<>();

        private LocationDescriptor(String key, String displayValue, List<String> aliases) {
            this.key = safe(key);
            this.displayValue = safe(displayValue);
            this.aliases = aliases == null ? Collections.<String>emptyList() : new ArrayList<>(aliases);
            addComparableValue(this.key);
            addComparableValue(this.displayValue);
            for (String alias : this.aliases) {
                addComparableValue(alias);
            }
        }

        private boolean matches(String comparableValue) {
            return comparableValues.contains(comparableValue);
        }

        private void addComparableValue(String value) {
            String comparable = toKey(value);
            if (!comparable.isEmpty()) {
                comparableValues.add(comparable);
            }
        }
    }
}
