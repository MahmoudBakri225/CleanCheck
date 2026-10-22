package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.AnalysisResult;
import com.mycompany.cleancheck.quality.QualityScore;
import com.mycompany.cleancheck.security.SecurityCatalog;
import com.mycompany.cleancheck.security.SecurityScore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Machine-readable twin of the HTML report (no external JSON library).
 * @author KA
 */
public final class JsonReportExporter {
    private JsonReportExporter() {
    }
    public static String toJson(AnalysisResult result) {
        QualityScore quality = new QualityScore();
        List<Warning> warnings = result.warnings();
        int score = quality.calculate(warnings, result.totalSourceLines());
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"projectRoot\": ").append(quote(result.projectRoot() == null
                ? null : result.projectRoot().toString())).append(",\n");
        sb.append("  \"filesAnalyzed\": ").append(result.javaFiles().size()).append(",\n");
        sb.append("  \"score\": ").append(score).append(",\n");
        sb.append("  \"rating\": ").append(quote(quality.getRating(score))).append(",\n");
        sb.append("  \"critical\": ").append(count(warnings, Severity.CRITICAL)).append(",\n");
        sb.append("  \"warning\": ").append(count(warnings, Severity.WARNING)).append(",\n");
        sb.append("  \"info\": ").append(count(warnings, Severity.INFO)).append(",\n");
        List<String> securitySeverities = new ArrayList<>();
        for (Warning w : warnings) {
            if (SecurityCatalog.isSecurity(w.getRuleName())) {
                securitySeverities.add(w.getSeverity().name());
            }
        }
        SecurityScore.Result security = SecurityScore.of(securitySeverities);
        sb.append("  \"security\": {\"preliminary\": true, \"score\": ").append(security.score())
                .append(", \"grade\": ").append(quote(security.grade()))
                .append(", \"findings\": ").append(security.total()).append("},\n");
        sb.append("  \"issues\": [");
        for (int i = 0; i < warnings.size(); i++) {
            Warning w = warnings.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"file\": ").append(quote(w.getFileName()));
            sb.append(", \"line\": ").append(w.getLineNumber());
            sb.append(", \"rule\": ").append(quote(w.getRuleName()));
            SecurityCatalog.info(w.getRuleName()).ifPresent(info -> sb
                    .append(", \"cwe\": ").append(quote(info.cwe()))
                    .append(", \"owasp\": ").append(quote(info.owasp())));
            sb.append(", \"severity\": ").append(quote(w.getSeverity().name()));
            sb.append(", \"message\": ").append(quote(w.getMessage()));
            sb.append(", \"suggestion\": ").append(quote(w.getSuggestion())).append("}");
        }
        sb.append(warnings.isEmpty() ? "]\n" : "\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }
    public static void write(AnalysisResult result, Path target) throws IOException {
        ReportOutputGuard.rejectSourceOverwrite(result, target);
        ReportFileWriter.writeUtf8(target, toJson(result));
    }
    private static long count(List<Warning> warnings, Severity severity) {
        return warnings.stream().filter(w -> w.getSeverity() == severity).count();
    }
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
