package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Fix priorities" block for the HTML report — same risk weights as Metrics
 * (Critical×5, Warning×3, Info×1), ordered by file then by rule.
 */
final class FixPrioritiesSection {

    private FixPrioritiesSection() {
    }

    static void append(StringBuilder html, List<Warning> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }

        Map<String, Integer> fileRisk = new LinkedHashMap<>();
        Map<String, Integer> ruleRisk = new LinkedHashMap<>();
        for (Warning w : warnings) {
            int weight = ReportFormat.getSeverityWeight(w);
            fileRisk.merge(w.getFileName(), weight, Integer::sum);
            ruleRisk.merge(w.getRuleName(), weight, Integer::sum);
        }

        List<Map.Entry<String, Integer>> files = new ArrayList<>(fileRisk.entrySet());
        files.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));

        List<Map.Entry<String, Integer>> rules = new ArrayList<>(ruleRisk.entrySet());
        rules.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));

        html.append("""
                <section class="section">
                  <h2>Fix priorities</h2>
                  <p class="subtitle">
                    Where to start — same risk weights as Metrics (Critical ×5, Warning ×3, Info ×1).
                  </p>
                  <div class="grid-2">
                    <div>
                      <h3>Riskiest files</h3>
                      <table>
                        <thead><tr><th>#</th><th>File</th><th>Risk</th></tr></thead>
                        <tbody>
                """);

        int rank = 1;
        for (Map.Entry<String, Integer> e : files) {
            if (rank > 10) {
                break;
            }
            html.append("<tr><td>").append(rank++)
                    .append("</td><td>").append(ReportFormat.escapeHtml(e.getKey()))
                    .append("</td><td>").append(e.getValue())
                    .append("</td></tr>\n");
        }

        html.append("""
                        </tbody>
                      </table>
                    </div>
                    <div>
                      <h3>Riskiest rules</h3>
                      <table>
                        <thead><tr><th>#</th><th>Rule</th><th>Risk</th></tr></thead>
                        <tbody>
                """);

        rank = 1;
        for (Map.Entry<String, Integer> e : rules) {
            if (rank > 10) {
                break;
            }
            html.append("<tr><td>").append(rank++)
                    .append("</td><td>").append(ReportFormat.escapeHtml(e.getKey()))
                    .append("</td><td>").append(e.getValue())
                    .append("</td></tr>\n");
        }

        html.append("""
                        </tbody>
                      </table>
                    </div>
                  </div>
                </section>
                """);
    }
}
