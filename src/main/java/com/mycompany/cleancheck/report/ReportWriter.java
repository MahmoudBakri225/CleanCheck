package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.application.AnalysisResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Writes a report in the format implied by the file extension: .json, .sarif, .csv, otherwise HTML. */
public final class ReportWriter {

    private ReportWriter() {
    }

    /** @return the path actually written (the HTML generator may adjust it) */
    public static String write(AnalysisResult analysis, Path target) throws IOException {
        String name = target.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            JsonReportExporter.write(analysis, target);
        } else if (name.endsWith(".sarif")) {
            SarifReportExporter.write(analysis, target);
        } else if (name.endsWith(".csv")) {
            CsvReportExporter.write(analysis, target);
        } else {
            return new HtmlReportGenerator().generateStandalone(analysis, target);
        }
        return target.toString();
    }
}
