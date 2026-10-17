package com.mycompany.cleancheck.report;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.AnalysisResult;
import com.mycompany.cleancheck.quality.QualityScore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HtmlReportGenerator {

    /*
     * =========================================================
     * GENERATE HTML REPORT
     * =========================================================
     */
    public String generate(
            String projectName,
            List<String> fileNames,
            List<Warning> finalWarnings,
            List<Warning> initialWarnings,
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

        String date = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        Path reportFile = Paths.get("reports", date, "html")
                .resolve(safeFileName(projectName) + "_Report.html");

        return generateTo(
                reportFile,
                projectName, fileNames, finalWarnings, initialWarnings,
                baselineScore, initialScore,
                regressionNewIssues, regressionResolvedIssues, regressionUnchangedIssues,
                fixNewIssues, fixResolvedIssues, fixUnchangedIssues,
                finalScore, projectId
        );
    }

    /** Project names come from folder names; keep the report file name safe on every OS (letters of any script are kept). */
    static String safeFileName(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return cleaned.isBlank() ? "CleanCheck" : cleaned;
    }

    /**
     * Writes the same report to an explicit file (used by the CLI and the GUI export).
     * A projectId of 0 or less skips the SQLite quality-history section, so the report
     * can be produced without a database.
     */
    public String generateTo(
            Path reportFile,
            String projectName,
            List<String> fileNames,
            List<Warning> finalWarnings,
            List<Warning> initialWarnings,
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
        // A database-backed run (projectId > 0) shows both comparison sections, as before.
        return render(
                reportFile, projectName, fileNames, finalWarnings, initialWarnings,
                baselineScore, initialScore,
                regressionNewIssues, regressionResolvedIssues, regressionUnchangedIssues,
                fixNewIssues, fixResolvedIssues, fixUnchangedIssues,
                finalScore, projectId,
                projectId > 0, projectId > 0
        );
    }

    private String render(
            Path reportFile,
            String projectName,
            List<String> fileNames,
            List<Warning> finalWarnings,
            List<Warning> initialWarnings,
            Integer baselineScore,
            int initialScore,
            int regressionNewIssues,
            int regressionResolvedIssues,
            int regressionUnchangedIssues,
            int fixNewIssues,
            int fixResolvedIssues,
            int fixUnchangedIssues,
            int finalScore,
            int projectId,
            boolean showRegression,
            boolean showFixVerification
    ) throws IOException {

        Path parent = reportFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }


        /*
         * =====================================================
         * COUNTS
         * =====================================================
         */
        int criticalCount
                = (int) finalWarnings.stream()
                        .filter(w
                                -> "CRITICAL".equals(
                                ReportFormat.getSeverityName(w)
                        )
                        )
                        .count();

        int warningCount
                = (int) finalWarnings.stream()
                        .filter(w
                                -> "WARNING".equals(
                                ReportFormat.getSeverityName(w)
                        )
                        )
                        .count();

        int infoCount
                = (int) finalWarnings.stream()
                        .filter(w
                                -> "INFO".equals(
                                ReportFormat.getSeverityName(w)
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
                ReportFormat.escapeHtml(projectName)
        );

        html.append("""
                    - CleanCheck Report
                    </title>
                    <style>
                """);

        html.append(ReportStyles.CSS);

        html.append("""
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
                ReportFormat.escapeHtml(projectName)
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
                    ReportFormat.escapeHtml(fileName)
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

        if (showRegression) {
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
                    ReportFormat.formatSignedValue(
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

        }

        if (showFixVerification) {
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
                    ReportFormat.formatSignedValue(
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
        }

        if (!showRegression && !showFixVerification) {
            // Export without a stored comparison: show an honest note instead of zero-filled cards.
            html.append("""
                    <section class="section">
                        <h2>Analysis Only</h2>
                        <p class="subtitle">
                            This report comes from a single scan. Regression results appear
                            when the scan was compared with a stored baseline (run Analyze
                            in CleanCheck first, then export); fix-verification results
                            appear after an automatic-fix run.
                        </p>
                    </section>
                    """);
        }

        /*
         * =====================================================
         * RISK HOTSPOTS
         * =====================================================
         */
        RiskHotspotsSection.appendRiskHotspots(
                html,
                finalWarnings
        );

        FixPrioritiesSection.append(html, finalWarnings);

        /*
         * =====================================================
         * SECURITY REVIEW
         * =====================================================
         */
        SecuritySection.appendSecurityReview(
                html,
                finalWarnings
        );

        /*
         * =====================================================
         * DETECTED ISSUES
         * =====================================================
         */
        IssuesSection.appendDetectedIssues(
                html,
                finalWarnings
        );

        /*
         * =====================================================
         * QUALITY HISTORY
         * =====================================================
         */
        if (projectId > 0) {
            QualityHistorySection.appendQualityHistory(
                    html,
                    projectId
            );
        }

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
        ReportFileWriter.writeUtf8(reportFile, html.toString());

        System.out.println();
        System.out.println(
                "HTML Report Generated:"
        );

        System.out.println(
                reportFile.toAbsolutePath()
        );

        return reportFile.toAbsolutePath().toString();
    }

    /**
     * One-call export of a finished analysis (CLI and GUI). It never touches the database; the
     * Regression section appears only when the analysis already carries a baseline comparison
     * ({@link AnalysisResult#regression()}). No fix verification. Returns the absolute path of the written report.
     */
    public String generateStandalone(AnalysisResult result, Path target) throws IOException {
        ReportOutputGuard.rejectSourceOverwrite(result, target);
        List<Warning> warnings = result.warnings();
        int score = new QualityScore().calculate(warnings, result.totalSourceLines());
        Path root = result.projectRoot();
        String name = root != null && root.getFileName() != null
                ? root.getFileName().toString()
                : "CleanCheck";
        List<String> files = result.javaFiles().stream()
                .map(f -> root == null
                        ? f.toString()
                        : root.relativize(f.toAbsolutePath().normalize()).toString().replace('\\', '/'))
                .toList();
        RegressionSummary regression = result.regression();
        boolean hasRegression = regression != null;
        return render(
                target, name, files, warnings, warnings,
                hasRegression ? regression.baselineScore() : null, score,
                hasRegression ? regression.newIssues() : 0,
                hasRegression ? regression.resolvedIssues() : 0,
                hasRegression ? regression.unchangedIssues() : 0,
                0, 0, warnings.size(),
                score, 0,
                hasRegression, false
        );
    }

    /*
     * =========================================================
     * BACKWARD COMPATIBILITY
     * =========================================================
     */
    public void generate(
            String projectName,
            List<Warning> warnings,
            int score
    ) throws IOException {

        generate(
                projectName,
                warnings.stream()
                        .map(
                                Warning::getFileName
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
