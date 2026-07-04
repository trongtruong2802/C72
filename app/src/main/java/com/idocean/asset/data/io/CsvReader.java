package com.idocean.asset.data.io;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Helper doc CSV/semicolon theo dinh dang hien tai cua app.
 */
public final class CsvReader {
    private CsvReader() {
    }

    public static char resolveDelimiter(String firstLine) {
        if (firstLine == null || firstLine.isEmpty()) {
            return ',';
        }
        int commaCount = firstLine.length() - firstLine.replace(",", "").length();
        int semicolonCount = firstLine.length() - firstLine.replace(";", "").length();
        return semicolonCount > commaCount ? ';' : ',';
    }

    public static List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> columns = new ArrayList<>();
        if (line == null) {
            return columns;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == delimiter && !inQuotes) {
                columns.add(cleanCell(current.toString()));
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        columns.add(cleanCell(current.toString()));
        return columns;
    }

    public static Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> indexMap = new LinkedHashMap<>();
        if (headers == null) {
            return indexMap;
        }
        for (int index = 0; index < headers.size(); index++) {
            String normalized = normalizeHeader(headers.get(index));
            if (!normalized.isEmpty() && !indexMap.containsKey(normalized)) {
                indexMap.put(normalized, index);
            }
        }
        return indexMap;
    }

    public static String getValue(Map<String, Integer> headerIndex, List<String> row, String key) {
        if (headerIndex == null || row == null) {
            return "";
        }
        Integer index = headerIndex.get(normalizeHeader(key));
        if (index == null || index < 0 || index >= row.size()) {
            return "";
        }
        return cleanCell(row.get(index));
    }

    public static String normalizeHeader(String value) {
        String cleaned = cleanCell(value)
                .replace('\u00A0', ' ')
                .replace('\u2026', ' ');
        String normalized = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static String cleanCell(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("\uFEFF")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }
}
