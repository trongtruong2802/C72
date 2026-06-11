package com.idocean.asset.utils;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AssetFieldNormalizerTest {

    @Test
    public void normalizeDepartmentForDisplay_keepsCanonicalValues() {
        assertEquals("Finance & Accountant", AssetFieldNormalizer.normalizeDepartmentForDisplay("Finance and Accountant"));
        assertEquals("IT", AssetFieldNormalizer.normalizeDepartmentForDisplay("IT"));
    }

    @Test
    public void normalizeDepartmentForDisplay_handlesBlankValues() {
        assertEquals("", AssetFieldNormalizer.normalizeDepartmentForDisplay("  "));
        assertEquals("", AssetFieldNormalizer.normalizeDepartmentForDisplay(null));
    }

    @Test
    public void normalizeLocationForDisplay_keepsCanonicalAliasMapping() {
        assertEquals("L\u1ea7u 5 - TT16", AssetFieldNormalizer.normalizeLocationForDisplay("Idoplex-5"));
        assertEquals("TT16", AssetFieldNormalizer.normalizeLocationForDisplay("Idoplex"));
    }

    @Test
    public void normalizeLocationForDisplay_handlesFloorAliasesAndBlankValues() {
        assertEquals("L\u1ea7u 2 - TT16", AssetFieldNormalizer.normalizeLocationForDisplay("Idoplex - 2"));
        assertEquals("T\u1ea7ng G - TT16", AssetFieldNormalizer.normalizeLocationForDisplay("Idoplex-G"));
        assertEquals("L\u1ea7u 5 - TT16", AssetFieldNormalizer.normalizeLocationForDisplay("Lau 5 - TT16"));
        assertEquals("T\u1ea7ng B - TT16", AssetFieldNormalizer.normalizeLocationForDisplay("Tang B - TT16"));
        assertEquals("", AssetFieldNormalizer.normalizeLocationForDisplay("  "));
    }

    @Test
    public void normalizeAssetTypeStatusAndCondition_trimValues() {
        assertEquals("Laptop", AssetFieldNormalizer.normalizeAssetTypeForDisplay("  Laptop  "));
        assertEquals("Dang su dung", AssetFieldNormalizer.normalizeInventoryStatusForDisplay("  Dang su dung  "));
        assertEquals("Tot", AssetFieldNormalizer.normalizeConditionForDisplay("  Tot  "));
    }

    @Test
    public void normalizeDisplayValues_keepsRawEncounterOrderAfterTrimming() {
        List<String> normalized = AssetFieldNormalizer.normalizeDisplayValues(
                new LinkedHashSet<>(Arrays.asList(" IT ", "", null, "HR", "IT", " QA "))
        );

        assertEquals(4, normalized.size());
        assertEquals("IT", normalized.get(0));
        assertEquals("HR", normalized.get(1));
        assertEquals("IT", normalized.get(2));
        assertEquals("QA", normalized.get(3));
    }
}
