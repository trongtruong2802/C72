package com.idocean.asset.data.io;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Helper write CSV/semicolon rows theo cach hien tai cua app.
 */
public final class CsvWriter {
    private CsvWriter() {
    }

    public static void writeUtf8Bom(BufferedWriter writer) throws IOException {
        if (writer == null) {
            return;
        }
        writer.write('\uFEFF');
    }

    public static void writeExcelSeparatorHint(BufferedWriter writer, char delimiter) throws IOException {
        if (writer == null) {
            return;
        }
        writer.write("sep=");
        writer.write(delimiter);
        writer.newLine();
    }

    public static String quote(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    public static String escapeDelimited(String value, char delimiter) {
        String safeValue = value == null ? "" : value;
        String escaped = safeValue.replace("\"", "\"\"");
        return escaped.indexOf(delimiter) >= 0 || escaped.contains("\"") || escaped.contains("\n")
                ? "\"" + escaped + "\""
                : escaped;
    }

    public static void writeQuotedLine(BufferedWriter writer, char delimiter, String... values) throws IOException {
        if (writer == null) {
            return;
        }
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(delimiter);
            }
            writer.write(quote(values[index]));
        }
        writer.newLine();
    }

    public static void writeDelimitedLine(BufferedWriter writer, char delimiter, String... values) throws IOException {
        if (writer == null) {
            return;
        }
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(delimiter);
            }
            writer.write(escapeDelimited(values[index], delimiter));
        }
        writer.newLine();
    }
}
