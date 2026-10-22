package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.AnalysisResult;

import java.io.IOException;
import java.nio.file.Path;

/** Spreadsheet-friendly export (RFC 4180 quoting, UTF-8 with a BOM so Excel reads non-ASCII text). */
public final class CsvReportExporter {

    private CsvReportExporter() {
    }

    public static String toCsv(AnalysisResult result) {
        StringBuilder sb = new StringBuilder("file,line,rule,severity,message,suggestion\r\n");
        for (Warning w : result.warnings()) {
            sb.append(cell(w.getFileName())).append(',')
                    .append(w.getLineNumber()).append(',')
                    .append(cell(w.getRuleName())).append(',')
                    .append(w.getSeverity().name()).append(',')
                    .append(cell(w.getMessage())).append(',')
                    .append(cell(w.getSuggestion())).append("\r\n");
        }
        return sb.toString();
    }

    public static void write(AnalysisResult result, Path target) throws IOException {
        ReportOutputGuard.rejectSourceOverwrite(result, target);
        ReportFileWriter.writeUtf8(target, "\uFEFF" + toCsv(result));
    }

    public static String cell(String value) {
        if (value == null) {
            return "";
        }
        value = spreadsheetSafe(value);
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String spreadsheetSafe(String value) {
        int firstNonWhitespace = 0;
        while (firstNonWhitespace < value.length() && Character.isWhitespace(value.charAt(firstNonWhitespace))) {
            firstNonWhitespace++;
        }
        if (firstNonWhitespace == value.length()) {
            return value;
        }

        char first = value.charAt(firstNonWhitespace);
        return value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")
                || first == '=' || first == '+' || first == '-' || first == '@'
                ? "'" + value
                : value;
    }
}
