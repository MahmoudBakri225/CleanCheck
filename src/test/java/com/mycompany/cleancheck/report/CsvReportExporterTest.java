package com.mycompany.cleancheck.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CsvReportExporterTest {

    @Test
    void prefixesSpreadsheetFormulaCellsAsText() {
        assertEquals("'=1+1", CsvReportExporter.cell("=1+1"));
        assertEquals("'  @SUM(A1:A2)", CsvReportExporter.cell("  @SUM(A1:A2)"));
        assertEquals("'\t=1+1", CsvReportExporter.cell("\t=1+1"));
    }

    @Test
    void keepsOrdinaryCsvQuoting() {
        assertEquals("\"safe, value\"", CsvReportExporter.cell("safe, value"));
        assertEquals("\"a\"\"b\"", CsvReportExporter.cell("a\"b"));
    }
}
