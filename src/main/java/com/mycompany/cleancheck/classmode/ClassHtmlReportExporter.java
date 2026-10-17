package com.mycompany.cleancheck.classmode;

import com.mycompany.cleancheck.analysis.Warning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Builds a detailed HTML report for class mode. */
public final class ClassHtmlReportExporter {

    private ClassHtmlReportExporter() {
    }

    public static Path export(
            Path output,
            List<ClassReport> reports,
            Map<String, Integer> commonRules
    ) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html><head><meta charset='UTF-8'>
                <title>CleanCheck Class Report</title>
                <style>
                body{font-family:Segoe UI,Arial,sans-serif;margin:0;background:#f1f5f9;color:#0f172a}
                .wrap{max-width:1100px;margin:0 auto;padding:32px}
                h1{margin:0 0 8px 0} h2{margin-top:32px}
                .sub{color:#64748b;margin-bottom:24px}
                .card{background:#fff;border:1px solid #e2e8f0;border-radius:14px;padding:18px;margin-bottom:18px;
                box-shadow:0 8px 24px rgba(15,23,42,.06)}
                table{border-collapse:collapse;width:100%;background:#fff}
                th,td{padding:10px 12px;border-bottom:1px solid #e2e8f0;text-align:left;font-size:14px}
                th{background:#0f172a;color:#fff}
                .good{color:#16a34a;font-weight:700} .bad{color:#dc2626;font-weight:700}
                .sev-CRITICAL{color:#dc2626;font-weight:700}
                .sev-WARNING{color:#ea580c;font-weight:700}
                .sev-INFO{color:#2563eb;font-weight:700}
                .pill{display:inline-block;padding:2px 8px;border-radius:999px;background:#e2e8f0;font-size:12px}
                </style></head><body><div class='wrap'>
                """);
        html.append("<h1>CleanCheck — Class Report</h1>")
                .append("<p class='sub'>Ranking and detailed findings per student project</p>");

        html.append("<div class='card'><h2 style='margin-top:0'>Ranking</h2><table><tr>")
                .append("<th>#</th><th>Student</th><th>Score</th><th>Total</th>")
                .append("<th>Critical</th><th>Warnings</th><th>Info</th></tr>");
        int rank = 1;
        for (ClassReport r : reports) {
            html.append("<tr><td>").append(rank++).append("</td><td>")
                    .append(escape(r.student())).append("</td><td class='")
                    .append(r.score() >= 75 ? "good" : "bad").append("'>")
                    .append(r.score()).append(" / 100</td><td>")
                    .append(r.totalIssues()).append("</td><td>")
                    .append(r.critical()).append("</td><td>")
                    .append(r.warnings()).append("</td><td>")
                    .append(r.info()).append("</td></tr>");
        }
        html.append("</table></div>");

        html.append("<div class='card'><h2 style='margin-top:0'>Most Common Rules</h2>")
                .append("<table><tr><th>Rule</th><th>Occurrences</th></tr>");
        commonRules.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> html.append("<tr><td>").append(escape(e.getKey()))
                        .append("</td><td>").append(e.getValue()).append("</td></tr>"));
        html.append("</table></div>");

        for (ClassReport r : reports) {
            html.append("<div class='card'><h2 style='margin-top:0'>")
                    .append(escape(r.student()))
                    .append(" <span class='pill'>").append(r.score()).append(" / 100</span></h2>");
            if (r.issues().isEmpty()) {
                html.append("<p class='sub'>No issues found.</p></div>");
                continue;
            }
            html.append("<table><tr><th>File</th><th>Line</th><th>Severity</th>")
                    .append("<th>Rule</th><th>Message</th><th>Suggestion</th></tr>");
            for (Warning w : r.issues()) {
                html.append("<tr><td>").append(escape(w.getFileName())).append("</td><td>")
                        .append(w.getLineNumber()).append("</td><td class='sev-")
                        .append(w.getSeverity()).append("'>").append(w.getSeverity())
                        .append("</td><td>").append(escape(w.getRuleName())).append("</td><td>")
                        .append(escape(w.getMessage())).append("</td><td>")
                        .append(escape(w.getSuggestion())).append("</td></tr>");
            }
            html.append("</table></div>");
        }

        html.append("</div></body></html>");
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, html.toString());
        return output;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
