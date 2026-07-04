package com.idocean.asset.data.io;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CsvReaderTest {

    @Test
    public void resolveDelimiter_prefersSemicolonWhenMoreSemicolons() {
        assertEquals(';', CsvReader.resolveDelimiter("a;b;c;d"));
    }

    @Test
    public void parseDelimitedLine_handlesQuotedDelimiterAndEscapedQuotes() {
        List<String> values = CsvReader.parseDelimitedLine("\"A;B\";\"He said \"\"Hi\"\"\";plain", ';');
        assertEquals(Arrays.asList("A;B", "He said \"Hi\"", "plain"), values);
    }

    @Test
    public void buildHeaderIndex_normalizesHeaders() {
        Map<String, Integer> index = CsvReader.buildHeaderIndex(Arrays.asList("code", "department", "Location"));
        assertTrue(index.containsKey("code"));
        assertTrue(index.containsKey("department"));
        assertTrue(index.containsKey("location"));
        assertEquals(Integer.valueOf(0), index.get("code"));
        assertEquals(Integer.valueOf(1), index.get("department"));
        assertEquals(Integer.valueOf(2), index.get("location"));
    }

    @Test
    public void getValue_returnsEmptyWhenMissing() {
        Map<String, Integer> index = CsvReader.buildHeaderIndex(Arrays.asList("code", "tid"));
        List<String> row = Arrays.asList("A001", "E280");
        assertEquals("A001", CsvReader.getValue(index, row, "code"));
        assertEquals("", CsvReader.getValue(index, row, "asset_name"));
    }

    @Test
    public void normalizeHeader_removesDiacriticsAndPunctuation() {
        assertEquals("phong_ban", CsvReader.normalizeHeader("  Phòng-ban  "));
        assertEquals("asset_name", CsvReader.normalizeHeader("\uFEFFAsset Name"));
    }
}
