package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.AnalysisResult;
import com.mycompany.cleancheck.quality.QualityScore;
import com.mycompany.cleancheck.security.SecurityCatalog;
import com.mycompany.cleancheck.rules.RuleEducation;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.mycompany.cleancheck.report.JsonReportExporter.quote;

/**
 * SARIF 2.1.0 export with richer tool metadata, artifacts, rule properties and results.
 * Suitable for GitHub Code Scanning and IDE SARIF viewers.
 */
public final class SarifReportExporter {

    private SarifReportExporter() {
    }

    public static String toSarif(AnalysisResult result) {
        List<Warning> warnings = result.warnings();
        QualityScore quality = new QualityScore();
        int score = quality.calculate(warnings, result.totalSourceLines());

        Map<String, String> rules = new TreeMap<>();
        for (Warning w : warnings) {
            rules.putIfAbsent(ruleId(w.getRuleName()), w.getRuleName());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"$schema\": \"https://json.schemastore.org/sarif-2.1.0.json\",\n");
        sb.append("  \"version\": \"2.1.0\",\n");
        sb.append("  \"runs\": [{\n");
        sb.append("    \"tool\": {\n");
        sb.append("      \"driver\": {\n");
        sb.append("        \"name\": \"CleanCheck\",\n");
        sb.append("        \"version\": \"1.0.0\",\n");
        sb.append("        \"rules\": [");

        boolean first = true;
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            sb.append(first ? "\n" : ",\n");
            first = false;
            sb.append("          {\n");
            sb.append("            \"id\": ").append(quote(rule.getKey())).append(",\n");
            sb.append("            \"name\": ").append(quote(rule.getValue())).append(",\n");
            sb.append("            \"shortDescription\": {\"text\": ").append(quote(rule.getValue())).append("},\n");
            sb.append("            \"fullDescription\": {\"text\": ")
                    .append(quote(RuleEducation.forRule(rule.getValue()))).append("},\n");
            sb.append("            \"defaultConfiguration\": {\"level\": \"warning\"}");
            SecurityCatalog.info(rule.getValue()).ifPresent(info -> {
                sb.append(",\n            \"help\": {\"text\": ")
                        .append(quote(info.attack() + " Fix: " + String.join(" ", info.steps()))).append("},\n");
                sb.append("            \"helpUri\": ").append(quote(info.cweUrl())).append(",\n");
                sb.append("            \"properties\": {\"tags\": [\"security\", ")
                        .append(quote("external/cwe/" + info.cwe().toLowerCase(Locale.ROOT)))
                        .append("], \"precision\": \"medium\"}");
            });
            sb.append("\n          }");
        }

        sb.append("\n        ]\n      }\n    },\n");

        // Artifacts (analyzed files)
        sb.append("    \"artifacts\": [");
        Path root = result.projectRoot();
        first = true;
        Map<String, Integer> artifactIndex = new LinkedHashMap<>();
        int idx = 0;
        for (Path file : result.javaFiles()) {
            String uri = root == null
                    ? file.toString().replace('\\', '/')
                    : root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
            artifactIndex.put(uri, idx++);
            sb.append(first ? "\n" : ",\n");
            first = false;
            sb.append("      {\"location\": {\"uri\": ").append(quote(sarifUri(uri)))
                    .append("}, \"sourceLanguage\": \"java\"}");
        }
        sb.append("\n    ],\n");

        sb.append("    \"results\": [");
        first = true;
        for (Warning w : warnings) {
            sb.append(first ? "\n" : ",\n");
            first = false;
            String file = w.getFileName() == null ? "" : w.getFileName().replace('\\', '/');
            Integer art = artifactIndex.get(file);
            sb.append("      {\n");
            sb.append("        \"ruleId\": ").append(quote(ruleId(w.getRuleName()))).append(",\n");
            sb.append("        \"level\": ").append(quote(sarifLevel(w.getSeverity()))).append(",\n");
            sb.append("        \"message\": {\"text\": ").append(quote(
                    w.getMessage() + (w.getSuggestion() == null || w.getSuggestion().isBlank()
                            ? "" : " Suggestion: " + w.getSuggestion()))).append("},\n");
            sb.append("        \"locations\": [{\n");
            sb.append("          \"physicalLocation\": {\n");
            if (art != null) {
                sb.append("            \"artifactLocation\": {\"index\": ").append(art)
                        .append(", \"uri\": ").append(quote(sarifUri(file))).append("},\n");
            } else {
                sb.append("            \"artifactLocation\": {\"uri\": ").append(quote(sarifUri(file))).append("},\n");
            }
            sb.append("            \"region\": {\"startLine\": ").append(Math.max(1, w.getLineNumber())).append("}\n");
            sb.append("          }\n");
            sb.append("        }],\n");
            sb.append("        \"properties\": {\"ruleName\": ").append(quote(w.getRuleName()))
                    .append(", \"severity\": ").append(quote(String.valueOf(w.getSeverity()))).append("}\n");
            sb.append("      }");
        }
        sb.append("\n    ],\n");
        sb.append("    \"properties\": {\n");
        sb.append("      \"cleancheckScore\": ").append(score).append(",\n");
        sb.append("      \"cleancheckRating\": ").append(quote(quality.getRating(score))).append(",\n");
        sb.append("      \"filesAnalyzed\": ").append(result.javaFiles().size()).append(",\n");
        sb.append("      \"issueCount\": ").append(warnings.size()).append("\n");
        sb.append("    }\n");
        sb.append("  }]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static void write(AnalysisResult result, Path target) throws IOException {
        ReportOutputGuard.rejectSourceOverwrite(result, target);
        ReportFileWriter.writeUtf8(target, toSarif(result));
    }

    private static String ruleId(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) {
            return "CC000";
        }
        return "CC/" + ruleName.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String sarifUri(String path) {
        try {
            return new URI(null, path.replace('\\', '/'), null).toASCIIString();
        } catch (java.net.URISyntaxException invalid) {
            return path.replace('\\', '/').replace(" ", "%20");
        }
    }

    private static String sarifLevel(Severity severity) {
        if (severity == null) {
            return "note";
        }
        return switch (severity) {
            case CRITICAL -> "error";
            case WARNING -> "warning";
            case INFO -> "note";
        };
    }
}
