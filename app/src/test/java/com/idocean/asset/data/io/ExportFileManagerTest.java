package com.idocean.asset.data.io;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExportFileManagerTest {

    @Test
    public void sanitizeFileNameSegment_returnsFallbackForBlank() {
        assertEquals("khong-ro", ExportFileManager.sanitizeFileNameSegment("   "));
        assertEquals("khong-ro", ExportFileManager.sanitizeFileNameSegment(null));
    }

    @Test
    public void sanitizeFileNameSegment_stripsInvalidCharacters() {
        assertEquals("Phong ban A", ExportFileManager.sanitizeFileNameSegment("Phong/ban:A"));
    }

    @Test
    public void sanitizeFileName_replacesInvalidCharactersWithUnderscore() {
        assertEquals("ticket_123.csv", ExportFileManager.sanitizeFileName("ticket:123.csv"));
    }
}
