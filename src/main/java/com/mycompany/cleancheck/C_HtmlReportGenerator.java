package com.mycompany.cleancheck;

import com.mycompany.cleancheck.database.C_DatabaseConnection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class C_HtmlReportGenerator {

    /*
     * =========================================================
     * GENERATE HTML REPORT
     * =========================================================
     */
    public String generate(
            String projectName,
            List<String> fileNames,
            List<C_Warning> finalWarnings,
            List<C_Warning> initialWarnings,
            Integer baselineScore,
            int initialScore,
            int regressionNewIssues,
            int regressionResolvedIssues,
            int regressionUnchangedIssues,
            int fixNewIssues,
            int fixResolvedIssues,
            int fixUnchangedIssues,
            int finalScore,
            int projectId
    ) throws IOException {

        /*
         * =====================================================
         * REPORT DIRECTORY
         * =====================================================
         */
        String date
                = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd_HH-mm-ss"
                        )
                );

        Path reportDirectory
                = Paths.get(
                        "reports",
                        date,
                        "html"
                );

        Files.createDirectories(
                reportDirectory
        );

        Path reportFile
                = reportDirectory.resolve(
                        projectName + "_Report.html"
                );

        /*
         * =====================================================
         * COUNTS
         * =====================================================
         */
        int criticalCount
                = (int) finalWarnings.stream()
                        .filter(w
                                -> "CRITICAL".equals(
                                getSeverityName(w)
                        )
                        )
                        .count();

        int warningCount
                = (int) finalWarnings.stream()
                        .filter(w
                                -> "WARNING".equals(
                                getSeverityName(w)
                        )
                        )
                        .count();

        int infoCount
                = (int) finalWarnings.stream()
                        .filter(w
                                -> "INFO".equals(
                                getSeverityName(w)
                        )
                        )
                        .count();

        int totalIssues
                = finalWarnings.size();

        /*
         * =====================================================
         * REGRESSION COUNTS
         * =====================================================
         */
        int regressionNew
                = regressionNewIssues;

        int regressionResolved
                = regressionResolvedIssues;

        int regressionUnchanged
                = regressionUnchangedIssues;

        int regressionCurrentIssues
                = regressionNew
                + regressionUnchanged;

        int regressionScoreChange
                = baselineScore != null
                        ? finalScore - baselineScore
                        : 0;

        /*
         * =====================================================
         * FIX VERIFICATION COUNTS
         * =====================================================
         */
        int fixNew
                = fixNewIssues;

        int fixResolved
                = fixResolvedIssues;

        int fixUnchanged
                = fixUnchangedIssues;

        int fixInitialIssues
                = initialWarnings.size();

        /*
         * =====================================================
         * START HTML
         * =====================================================
         */
        StringBuilder html
                = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width,
                                   initial-scale=1.0">
                    <title>
                """);

        html.append(
                escapeHtml(projectName)
        );

        html.append("""
                    - CleanCheck Report
                    </title>
                    <style>
                        * {
                            box-sizing: border-box;
                        }
                        body {
                            margin: 0;
                            padding: 0;
                            font-family:
                                Arial,
                                Helvetica,
                                sans-serif;
                            background: #f4f6f8;
                            color: #222;
                        }
                        .container {
                            width: 95%;
                            max-width: 1400px;
                            margin: 30px auto;
                        }
                        .header {
                            background: #1f2937;
                            color: white;
                            padding: 30px;
                            border-radius: 12px;
                            margin-bottom: 25px;
                        }
                        .header h1 {
                            margin: 0 0 10px 0;
                        }
                        .header p {
                            margin: 5px 0;
                            opacity: 0.9;
                        }
                        .section {
                            background: white;
                            padding: 25px;
                            border-radius: 12px;
                            margin-bottom: 25px;
                            box-shadow:
                                0 2px 8px
                                rgba(0,0,0,0.08);
                        }
                        .section h2 {
                            margin-top: 0;
                            color: #1f2937;
                        }
                        .subtitle {
                            color: #6b7280;
                        }
                        .cards {
                            display: grid;
                            grid-template-columns:
                                repeat(
                                    auto-fit,
                                    minmax(180px, 1fr)
                                );
                            gap: 15px;
                        }
                        .card {
                            padding: 20px;
                            border-radius: 10px;
                            background: #f9fafb;
                            border:
                                1px solid #e5e7eb;
                        }
                        .card-title {
                            font-size: 14px;
                            color: #6b7280;
                        }
                        .card-value {
                            font-size: 28px;
                            font-weight: bold;
                            margin-top: 8px;
                        }
                        .score {
                            font-size: 40px;
                            font-weight: bold;
                        }
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 15px;
                        }
                        th,
                        td {
                            padding: 12px;
                            border-bottom:
                                1px solid #e5e7eb;
                            text-align: left;
                        }
                        th {
                            background: #f3f4f6;
                        }
                        tr:hover {
                            background: #f9fafb;
                        }
                        .critical {
                            color: #b91c1c;
                            font-weight: bold;
                        }
                        .warning {
                            color: #b45309;
                            font-weight: bold;
                        }
                        .info {
                            color: #2563eb;
                            font-weight: bold;
                        }
                        .success {
                            color: #15803d;
                            font-weight: bold;
                        }
                        .danger {
                            color: #b91c1c;
                            font-weight: bold;
                        }
                        .neutral {
                            color: #6b7280;
                            font-weight: bold;
                        }
                        .hotspot {
                            padding: 15px;
                            margin-bottom: 10px;
                            background: #f9fafb;
                            border:
                                1px solid #e5e7eb;
                            border-radius: 8px;
                        }
                        .hotspot-title {
                            font-weight: bold;
                        }
                        .risk {
                            float: right;
                            font-weight: bold;
                        }
                        .footer {
                            text-align: center;
                            color: #6b7280;
                            padding: 20px;
                        }
                    </style>
                </head>
                <body>
                <div class="container">
                """);

        /*
         * =====================================================
         * HEADER
         * =====================================================
         */
        html.append("""
                <div class="header">
                    <h1>
                """);

        html.append(
                escapeHtml(projectName)
        );

        html.append("""
                    </h1>
                    <p>
                        Static Code Quality Analysis Report
                    </p>
                    <p>
                        Generated by CleanCheck
                    </p>
                </div>
                """);

        /*
         * =====================================================
         * CURRENT QUALITY OVERVIEW
         * =====================================================
         */
        html.append("""
                <section class="section">
                    <h2>
                        Current Quality Overview
                    </h2>
                    <div class="cards">
                        <div class="card">
                            <div class="card-title">
                                Critical
                            </div>
                            <div class="card-value critical">
                """);

        html.append(criticalCount);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Warnings
                            </div>
                            <div class="card-value warning">
                """);

        html.append(warningCount);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Info
                            </div>
                            <div class="card-value info">
                """);

        html.append(infoCount);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Total Issues
                            </div>
                            <div class="card-value">
                """);

        html.append(totalIssues);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Quality Score
                            </div>
                            <div class="score">
                """);

        html.append(finalScore);

        html.append("""
                                / 100
                            </div>
                        </div>
                    </div>
                </section>
                """);

        /*
         * =====================================================
         * SCANNED FILES
         * =====================================================
         */
        html.append("""
                <section class="section">
                    <h2>
                        Scanned Files
                    </h2>
                    <table>
                        <thead>
                            <tr>
                                <th>File</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

        for (String fileName : fileNames) {

            html.append("""
                            <tr>
                                <td>
                """);

            html.append(
                    escapeHtml(fileName)
            );

            html.append("""
                                </td>
                            </tr>
                    """);
        }

        html.append("""
                        </tbody>
                    </table>
                </section>
                """);

        /*
         * =====================================================
         * REGRESSION ANALYSIS
         * =====================================================
         */
        html.append("""
                <section class="section">
                    <h2>
                        Regression Analysis
                    </h2>
                    <p class="subtitle">
                        Comparison between baseline and current scan
                    </p>
                    <div class="cards">
                        <div class="card">
                            <div class="card-title">
                                Baseline Score
                            </div>
                            <div class="card-value">
                """);

        if (baselineScore != null) {

            html.append(baselineScore);
            html.append(" / 100");

        } else {

            html.append("N/A");
        }

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Initial Score
                            </div>
                            <div class="card-value">
                """);

        html.append(initialScore);

        html.append("""
                                / 100
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Current Issues
                            </div>
                            <div class="card-value">
                """);

        html.append(regressionCurrentIssues);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                New Issues
                            </div>
                            <div class="card-value danger">
                """);

        html.append(regressionNew);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Score Change
                            </div>
                            <div class="card-value">
                """);

        html.append(
                formatSignedValue(
                        regressionScoreChange
                )
        );

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Resolved Issues
                            </div>
                            <div class="card-value success">
                """);

        html.append(regressionResolved);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Unchanged Issues
                            </div>
                            <div class="card-value neutral">
                """);

        html.append(regressionUnchanged);

        html.append("""
                            </div>
                        </div>
                    </div>
                </section>
                """);

        /*
         * =====================================================
         * FIX VERIFICATION
         * =====================================================
         */
        html.append("""
                <section class="section">
                    <h2>
                        Fix Verification
                    </h2>
                    <p class="subtitle">
                        Initial scan compared with final scan
                        after automatic fixes
                    </p>
                    <div class="cards">
                        <div class="card">
                            <div class="card-title">
                                Initial Score
                            </div>
                            <div class="card-value">
                """);

        html.append(initialScore);

        html.append("""
                                / 100
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Final Score
                            </div>
                            <div class="card-value">
                """);

        html.append(finalScore);

        html.append("""
                                / 100
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Initial Issues
                            </div>
                            <div class="card-value">
                """);

        html.append(fixInitialIssues);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Final Issues
                            </div>
                            <div class="card-value">
                """);

        html.append(totalIssues);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Score Change
                            </div>
                            <div class="card-value">
                """);

        html.append(
                formatSignedValue(
                        finalScore - initialScore
                )
        );

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                New
                            </div>
                            <div class="card-value danger">
                """);

        html.append(fixNew);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Resolved
                            </div>
                            <div class="card-value success">
                """);

        html.append(fixResolved);

        html.append("""
                            </div>
                        </div>
                        <div class="card">
                            <div class="card-title">
                                Unchanged
                            </div>
                            <div class="card-value neutral">
                """);

        html.append(fixUnchanged);

        html.append("""
                            </div>
                        </div>
                    </div>
                </section>
                """);

        /*
         * =====================================================
         * RISK HOTSPOTS
         * =====================================================
         */
        appendRiskHotspots(
                html,
                finalWarnings
        );

        /*
         * =====================================================
         * DETECTED ISSUES
         * =====================================================
         */
        appendDetectedIssues(
                html,
                finalWarnings
        );

        /*
         * =====================================================
         * QUALITY HISTORY
         * =====================================================
         */
        appendQualityHistory(
                html,
                projectId
        );

        /*
         * =====================================================
         * FOOTER
         * =====================================================
         */
        html.append("""
                <div class="footer">
                    CleanCheck -
                    Static Code Quality Analyzer
                </div>
                </div>
                </body>
                </html>
                """);

        /*
         * =====================================================
         * WRITE FILE
         * =====================================================
         */
        Files.writeString(
                reportFile,
                html.toString()
        );

        System.out.println();
        System.out.println(
                "HTML Report Generated:"
        );

        System.out.println(
                reportFile.toAbsolutePath()
        );

        return reportFile.toAbsolutePath().toString();
    }

    /*
     * =========================================================
     * RISK HOTSPOTS
     * =========================================================
     */
    private void appendRiskHotspots(
            StringBuilder html,
            List<C_Warning> warnings
    ) {

        Map<String, Long> fileCounts
                = warnings.stream()
                        .collect(
                                Collectors.groupingBy(
                                        C_Warning::getFileName,
                                        Collectors.counting()
                                )
                        );

        Map<String, Long> ruleCounts
                = warnings.stream()
                        .collect(
                                Collectors.groupingBy(
                                        C_Warning::getRuleName,
                                        Collectors.counting()
                                )
                        );

        html.append("""
                <section class="section">
                    <h2>
                        Risk Hotspots
                    </h2>
                    <p class="subtitle">
                        Files and rules with the highest
                        concentration of issues
                    </p>

                    <h3>
                        File Hotspots
                    </h3>
                """);

        fileCounts.entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Long>comparingByValue()
                                .reversed()
                )
                .forEach(entry -> {

                    int risk
                            = calculateRisk(
                                    entry.getKey(),
                                    warnings
                            );

                    html.append("""
                            <div class="hotspot">
                                <span class="hotspot-title">
                            """);

                    html.append(
                            escapeHtml(
                                    entry.getKey()
                            )
                    );

                    html.append("""
                                </span>

                                <span>
                            """);

                    html.append(entry.getValue());
                    html.append(" issues");

                    html.append("""
                                </span>

                                <span class="risk">
                                    Risk:
                            """);

                    html.append(risk);

                    html.append("""
                                </span>
                            </div>
                            """);
                });

        /*
         * =====================================================
         * RULE HOTSPOTS
         * =====================================================
         */
        html.append("""
                    <h3>
                        Rule Hotspots
                    </h3>

                    <table>
                        <thead>
                            <tr>
                                <th>Rule</th>
                                <th>Issues</th>
                                <th>Risk Score</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

        ruleCounts.keySet()
                .stream()
                .sorted(
                        (rule1, rule2) -> {

                            int risk1
                            = calculateRuleRisk(
                                    rule1,
                                    warnings
                            );

                            int risk2
                            = calculateRuleRisk(
                                    rule2,
                                    warnings
                            );

                            /*
                             * Sort by Risk Score DESC
                             */
                            if (risk1 != risk2) {

                                return Integer.compare(
                                        risk2,
                                        risk1
                                );
                            }

                            /*
                             * If risk is equal,
                             * sort by rule name
                             */
                            return rule1.compareTo(
                                    rule2
                            );
                        }
                )
                .forEach(ruleName -> {

                    long issueCount
                            = ruleCounts.get(
                                    ruleName
                            );

                    int risk
                            = calculateRuleRisk(
                                    ruleName,
                                    warnings
                            );

                    html.append("""
                            <tr>
                                <td>%s</td>
                                <td>%d</td>
                                <td>%d</td>
                            </tr>
                            """.formatted(
                            escapeHtml(ruleName),
                            issueCount,
                            risk
                    ));
                });

        html.append("""
                        </tbody>
                    </table>
                </section>
                """);
    }

    /*
     * =========================================================
     * FILE RISK
     * =========================================================
     */
    private int calculateRisk(
            String fileName,
            List<C_Warning> warnings
    ) {

        return warnings.stream()
                .filter(w
                        -> w.getFileName()
                        .equals(fileName)
                )
                .mapToInt(
                        this::getSeverityWeight
                )
                .sum();
    }

    /*
     * =========================================================
     * RULE RISK
     * =========================================================
     */
    private int calculateRuleRisk(
            String ruleName,
            List<C_Warning> warnings
    ) {

        return warnings.stream()
                .filter(w
                        -> w.getRuleName()
                        .equals(ruleName)
                )
                .mapToInt(
                        this::getSeverityWeight
                )
                .sum();
    }

    /*
     * =========================================================
     * SEVERITY WEIGHT
     * =========================================================
     */
    private int getSeverityWeight(
            C_Warning warning
    ) {

        String severity
                = getSeverityName(warning);

        if ("CRITICAL".equals(severity)) {
            return 5;
        }

        if ("WARNING".equals(severity)) {
            return 3;
        }

        return 1;
    }

    /*
     * =========================================================
     * SEVERITY NAME
     * =========================================================
     */
    private String getSeverityName(
            C_Warning warning
    ) {

        if (warning == null
                || warning.getSeverity() == null) {

            return "";
        }

        return warning
                .getSeverity()
                .toString()
                .toUpperCase();
    }

    /*
     * =========================================================
     * DETECTED ISSUES
     * =========================================================
     */
    private void appendDetectedIssues(
            StringBuilder html,
            List<C_Warning> warnings
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

        for (C_Warning warning : warnings) {

            html.append("<tr>");

            html.append("<td>")
                    .append(
                            escapeHtml(
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
                            escapeHtml(
                                    warning.getRuleName()
                            )
                    )
                    .append("</td>");

            String severity
                    = getSeverityName(warning);

            String severityClass
                    = getSeverityClass(
                            severity
                    );

            html.append("<td class=\"")
                    .append(severityClass)
                    .append("\">")
                    .append(
                            escapeHtml(
                                    severity
                            )
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            escapeHtml(
                                    warning.getMessage()
                            )
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            escapeHtml(
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

    /*
     * =========================================================
     * QUALITY HISTORY
     * =========================================================
     */
    private void appendQualityHistory(
            StringBuilder html,
            int projectId
    ) {

        html.append("""
            <section class="section">
                <h2>
                    Quality History
                </h2>

                <p class="subtitle">
                    Latest meaningful quality changes for this project
                </p>

                <table>
                    <thead>
                        <tr>
                            <th>Scan ID</th>
                            <th>Date</th>
                            <th>Quality Score</th>
                            <th>Total Issues</th>
                            <th>Critical</th>
                            <th>Warnings</th>
                            <th>Info</th>
                        </tr>
                    </thead>

                    <tbody>
            """);

        String sql = """
            WITH ordered AS (
                SELECT
                    ScanId,
                    ScanDate,
                    QualityScore,
                    TotalIssues,
                    CriticalCount,
                    WarningCount,
                    InfoCount,

                    LAG(QualityScore)
                        OVER (ORDER BY ScanId)
                        AS PrevQualityScore,

                    LAG(TotalIssues)
                        OVER (ORDER BY ScanId)
                        AS PrevTotalIssues,

                    LAG(CriticalCount)
                        OVER (ORDER BY ScanId)
                        AS PrevCriticalCount,

                    LAG(WarningCount)
                        OVER (ORDER BY ScanId)
                        AS PrevWarningCount,

                    LAG(InfoCount)
                        OVER (ORDER BY ScanId)
                        AS PrevInfoCount

                FROM Scan

                WHERE ProjectId = ?
            ),

            changes AS (
                SELECT
                    ScanId,
                    ScanDate,
                    QualityScore,
                    TotalIssues,
                    CriticalCount,
                    WarningCount,
                    InfoCount

                FROM ordered

                WHERE PrevQualityScore IS NULL

                   OR QualityScore <> PrevQualityScore

                   OR TotalIssues <> PrevTotalIssues

                   OR CriticalCount <> PrevCriticalCount

                   OR WarningCount <> PrevWarningCount

                   OR InfoCount <> PrevInfoCount
            ),

            latest_changes AS (
                SELECT *

                FROM changes

                ORDER BY ScanId DESC

                LIMIT 10
            )

            SELECT *

            FROM latest_changes

            ORDER BY ScanId ASC
            """;

        boolean found = false;

        try (
                Connection connection
                = C_DatabaseConnection.getConnection(); PreparedStatement statement
                = connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    projectId
            );

            try (
                    ResultSet resultSet
                    = statement.executeQuery()) {

                while (resultSet.next()) {

                    found = true;

                    int scanId
                            = resultSet.getInt(
                                    "ScanId"
                            );

                    String scanDate
                            = resultSet.getString(
                                    "ScanDate"
                            );

                    int qualityScore
                            = resultSet.getInt(
                                    "QualityScore"
                            );

                    int totalIssues
                            = resultSet.getInt(
                                    "TotalIssues"
                            );

                    int criticalCount
                            = resultSet.getInt(
                                    "CriticalCount"
                            );

                    int warningCount
                            = resultSet.getInt(
                                    "WarningCount"
                            );

                    int infoCount
                            = resultSet.getInt(
                                    "InfoCount"
                            );

                    html.append("<tr>");

                    html.append("<td>")
                            .append(scanId)
                            .append("</td>");

                    html.append("<td>")
                            .append(
                                    escapeHtml(
                                            scanDate
                                    )
                            )
                            .append("</td>");

                    html.append("<td>")
                            .append(qualityScore)
                            .append(" / 100")
                            .append("</td>");

                    html.append("<td>")
                            .append(totalIssues)
                            .append("</td>");

                    html.append("<td>")
                            .append(criticalCount)
                            .append("</td>");

                    html.append("<td>")
                            .append(warningCount)
                            .append("</td>");

                    html.append("<td>")
                            .append(infoCount)
                            .append("</td>");

                    html.append("</tr>");
                }
            }

        } catch (Exception e) {

            html.append("""
                <tr>
                    <td colspan="7">
                        Unable to load quality history.
                    </td>
                </tr>
                """);

            System.out.println(
                    "Unable to load quality history: "
                    + e.getMessage()
            );
        }

        if (!found) {

            html.append("""
                <tr>
                    <td colspan="7">
                        No historical scans found.
                    </td>
                </tr>
                """);
        }

        html.append("""
                    </tbody>
                </table>
            </section>
            """);
    }

    /*
     * =========================================================
     * SEVERITY CSS CLASS
     * =========================================================
     */
    private String getSeverityClass(
            String severity
    ) {

        if ("CRITICAL".equals(severity)) {
            return "critical";
        }

        if ("WARNING".equals(severity)) {
            return "warning";
        }

        return "info";
    }

    /*
     * =========================================================
     * SIGNED VALUE
     * =========================================================
     */
    private String formatSignedValue(
            int value
    ) {

        if (value > 0) {
            return "+" + value;
        }

        return String.valueOf(value);
    }

    /*
     * =========================================================
     * HTML ESCAPE
     * =========================================================
     */
    private String escapeHtml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /*
     * =========================================================
     * BACKWARD COMPATIBILITY
     * =========================================================
     */
    public void generate(
            String projectName,
            List<C_Warning> warnings,
            int score
    ) throws IOException {

        generate(
                projectName,
                warnings.stream()
                        .map(
                                C_Warning::getFileName
                        )
                        .distinct()
                        .toList(),
                warnings,
                warnings,
                null,
                score,
                0,
                0,
                0,
                0,
                0,
                warnings.size(),
                score,
                0
        );
    }
}
