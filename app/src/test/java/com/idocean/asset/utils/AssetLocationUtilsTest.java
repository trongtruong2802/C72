package com.idocean.asset.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class AssetLocationUtilsTest {

    @Test
    public void normalizeLocationForDisplay_mapsLegacyIdoplexValuesToTt16Labels() {
        assertEquals("TT16", AssetLocationUtils.normalizeLocationForDisplay("Idoplex"));
        assertEquals("L\u1ea7u 2 - TT16", AssetLocationUtils.normalizeLocationForDisplay("Idoplex-2"));
        assertEquals("T\u1ea7ng G - TT16", AssetLocationUtils.normalizeLocationForDisplay("Idoplex - G"));
    }

    @Test
    public void normalizeLocationForDisplay_mapsUnexpectedTt17ToTt16() {
        assertEquals("L\u1ea7u 5 - TT16", AssetLocationUtils.normalizeLocationForDisplay("L\u1ea7u 5 - TT17"));
    }

    @Test
    public void resolveLocationKey_mapsKnownAliasesToCanonicalKeys() {
        assertEquals("TT16", AssetLocationUtils.resolveLocationKey("Idoplex"));
        assertEquals("TT16_F5", AssetLocationUtils.resolveLocationKey("Lau 5 - TT16"));
        assertEquals("TT16_F5", AssetLocationUtils.resolveLocationKey("Idoplex-5"));
        assertEquals("TT16_G", AssetLocationUtils.resolveLocationKey("Tang G - TT16"));
        assertEquals("WAREHOUSE", AssetLocationUtils.resolveLocationKey("Warehouse"));
    }

    @Test
    public void normalizeLocationForDisplay_keepsLegacyMojibakeLabels() {
        String basement = AssetLocationUtils.normalizeLocationForDisplay("TÃ¡ÂºÂ§ng B - TT16");
        String floor = AssetLocationUtils.normalizeLocationForDisplay("LÃ¡ÂºÂ§u 5 - TT16");
        assertTrue(basement != null && !basement.trim().isEmpty() && basement.contains("TT16"));
        assertTrue(floor != null && !floor.trim().isEmpty() && floor.contains("TT16"));
    }

    @Test
    public void resolveLocationQueryAliases_keepsLegacyAliasesForSync() {
        List<String> aliases = AssetLocationUtils.resolveLocationQueryAliases("L\u1ea7u 5 - TT16");

        assertTrue(aliases.contains("L\u1ea7u 5 - TT16"));
        assertTrue(aliases.contains("Idoplex - 5"));
        assertTrue(aliases.contains("Idoplex-5"));
        assertTrue(aliases.contains("L\u1ea7u 5 - TT17"));
    }

    @Test
    public void resolveLocationQueryAliases_supportsAsciiFloorLabels() {
        List<String> aliases = AssetLocationUtils.resolveLocationQueryAliases("Lau 5 - TT16");

        assertTrue(aliases.contains("L\u1ea7u 5 - TT16"));
        assertTrue(aliases.contains("Lau 5 - TT16"));
        assertTrue(aliases.contains("Idoplex - 5"));
        assertTrue(aliases.contains("Idoplex-5"));
    }

    @Test
    public void normalizeLocationForDisplay_acceptsCanonicalKeyInput() {
        assertEquals("L\u1ea7u 5 - TT16", AssetLocationUtils.normalizeLocationForDisplay("TT16_F5"));
        assertEquals("Warehouse", AssetLocationUtils.normalizeLocationForDisplay("WAREHOUSE"));
    }
}
