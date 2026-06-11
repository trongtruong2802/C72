package com.idocean.asset.storage;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class CsvWriterTest {

    @Test
    public void quote_escapesQuotes() {
        assertEquals("\"A\"\"B\"", CsvWriter.quote("A\"B"));
    }

    @Test
    public void writeQuotedLine_quotesEveryField() throws Exception {
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);
        CsvWriter.writeQuotedLine(writer, ',', "A,B", "He said \"Hi\"", "plain");
        writer.flush();

        assertEquals("\"A,B\",\"He said \"\"Hi\"\"\",\"plain\"" + System.lineSeparator(), output.toString());
    }

    @Test
    public void writeDelimitedLine_quotesOnlyWhenNeeded() throws Exception {
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);
        CsvWriter.writeDelimitedLine(writer, ';', "A;B", "plain", "He said \"Hi\"");
        writer.flush();

        assertEquals("\"A;B\";plain;\"He said \"\"Hi\"\"\"" + System.lineSeparator(), output.toString());
    }

    @Test
    public void writeUtf8BomAndExcelSeparatorHint_writesExcelFriendlyPreamble() throws Exception {
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);
        CsvWriter.writeUtf8Bom(writer);
        CsvWriter.writeExcelSeparatorHint(writer, ';');
        writer.flush();

        assertEquals("\uFEFFsep=;" + System.lineSeparator(), output.toString());
    }
}
