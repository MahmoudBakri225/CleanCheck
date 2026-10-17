package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.analysis.Severity;
import java.util.List;

/** Detected-issues table of the HTML report. */
final class IssuesSection {

    private IssuesSection() {
    }

    /*
     * =========================================================
     * DETECTED ISSUES
     * =========================================================
     */
    static void appendDetectedIssues(
            StringBuilder html,
            List<Warning> warnings
    ) {

        html.append("""
                <section class="section">
                    <h2>
                        Detected Issues
                    </h2>

                    <p class="subtitle">
                        Issues detected in the current scan
                    </p>

                    <table>
                        <thead>
                            <tr>
                                <th>File</th>
                                <th>Line</th>
                                <th>Rule</th>
                                <th>Severity</th>
                                <th>Message</th>
                                <th>Suggestion</th>
                            </tr>
                        </thead>

                        <tbody>
                """);

        for (Warning warning : warnings) {

            html.append("<tr>");

            html.append("<td>")
                    .append(
                            ReportFormat.escapeHtml(
                                    warning.getFileName()
                            )
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            warning.getLineNumber()
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            ReportFormat.escapeHtml(
                                    warning.getRuleName()
                            )
                    )
                    .append("</td>");

            String severity
                    = ReportFormat.getSeverityName(warning);

            String severityClass
                    = ReportFormat.getSeverityClass(
                            severity
                    );

            html.append("<td class=\"")
                    .append(severityClass)
                    .append("\">")
                    .append(
                            ReportFormat.escapeHtml(
                                    severity
                            )
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            ReportFormat.escapeHtml(
                                    warning.getMessage()
                            )
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            ReportFormat.escapeHtml(
                                    warning.getSuggestion()
                            )
                    )
                    .append("</td>");

            html.append("</tr>");
        }

        html.append("""
                        </tbody>
                    </table>
                </section>
                """);
    }
}
