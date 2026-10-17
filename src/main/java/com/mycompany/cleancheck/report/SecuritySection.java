package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.security.SecurityCatalog;
import com.mycompany.cleancheck.security.SecurityScore;

import java.util.ArrayList;
import java.util.List;

/** "Security Review" part of the HTML report: grade, findings with CWE / OWASP, and the limits of the check. */
final class SecuritySection {

    private SecuritySection() {
    }

    static void appendSecurityReview(StringBuilder html, List<Warning> warnings) {
        List<Warning> findings = new ArrayList<>();
        for (Warning warning : warnings) {
            if (SecurityCatalog.isSecurity(warning.getRuleName())) {
                findings.add(warning);
            }
        }
        List<String> severities = new ArrayList<>();
        for (Warning warning : findings) {
            severities.add(ReportFormat.getSeverityName(warning));
        }
        SecurityScore.Result score = SecurityScore.of(severities);

        html.append("<section class=\"section\">\n");
        html.append("<h2>Security Review <small>(preliminary)</small></h2>\n");
        html.append("<p class=\"subtitle\">Pattern-based review of each file with the Java AST. ")
                .append("It does not follow data between files, so it can raise false alarms and can miss flaws ")
                .append("that cross classes. Use it as a list of places to check, not as a security guarantee.</p>\n");

        html.append("<div class=\"cards\">");
        card(html, "Security grade", score.grade() + " (" + score.score() + "/100)");
        card(html, "Critical", String.valueOf(score.critical()));
        card(html, "Warnings", String.valueOf(score.warning()));
        card(html, "Verdict", score.verdict());
        html.append("</div>\n");

        if (findings.isEmpty()) {
            html.append("<p>No known risky patterns were found by the ")
                    .append(SecurityCatalog.all().size())
                    .append(" security rules.</p>\n</section>\n");
            return;
        }

        html.append("<table><thead><tr><th>File</th><th>Line</th><th>Rule</th><th>CWE</th><th>OWASP Top 10</th>")
                .append("<th>Severity</th><th>Finding</th><th>Safe fix</th></tr></thead><tbody>\n");
        for (Warning warning : findings) {
            SecurityCatalog.Info info = SecurityCatalog.info(warning.getRuleName()).orElseThrow();
            String severity = ReportFormat.getSeverityName(warning);
            html.append("<tr>");
            cell(html, warning.getFileName(), "");
            cell(html, String.valueOf(warning.getLineNumber()), "");
            cell(html, warning.getRuleName(), "");
            html.append("<td><a href=\"").append(ReportFormat.escapeHtml(info.cweUrl()))
                    .append("\" target=\"_blank\" rel=\"noopener\">")
                    .append(ReportFormat.escapeHtml(info.cwe())).append("</a></td>");
            cell(html, info.owasp(), "");
            cell(html, severity, ReportFormat.getSeverityClass(severity));
            cell(html, warning.getMessage(), "");
            cell(html, warning.getSuggestion(), "");
            html.append("</tr>\n");
        }
        html.append("</tbody></table>\n</section>\n");
    }

    private static void card(StringBuilder html, String title, String value) {
        html.append("<div class=\"card\"><div class=\"card-title\">")
                .append(ReportFormat.escapeHtml(title))
                .append("</div><div class=\"card-value\">")
                .append(ReportFormat.escapeHtml(value))
                .append("</div></div>");
    }

    private static void cell(StringBuilder html, String text, String cssClass) {
        html.append(cssClass.isEmpty() ? "<td>" : "<td class=\"" + cssClass + "\">")
                .append(ReportFormat.escapeHtml(text))
                .append("</td>");
    }
}
