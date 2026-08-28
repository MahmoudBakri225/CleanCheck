package com.mycompany.cleancheck;

import com.mycompany.cleancheck.quality.C_QualityScore;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates an HTML report containing the results of
 * the CleanCheck code-quality analysis.
 *
 * The report is stored inside a reports folder organized
 * by the current date.
 */
public class C_HtmlReportGenerator {

    /**
     * Generates an HTML report for the analyzed project.
     *
     * The report is stored as:
     *
     * reports/
     *     YYYY-MM-DD/
     *         File1_File2.html
     *
     * @param projectName name of the analyzed project
     * @param fileNames names of the analyzed Java files
     * @param warnings all warnings detected during analysis
     * @return path of the generated HTML report
     * @throws IOException if the report cannot be created
     */
    public String generate(
            String projectName,
            List<String> fileNames,
            List<C_Warning> warnings)
            throws IOException {

        C_QualityScore qualityScore =
                new C_QualityScore();

        int score =
                qualityScore.calculate(warnings);

        String date =
                LocalDate.now().toString();

        // Create reports/YYYY-MM-DD
        Path reportsFolder =
                Path.of(
                        "reports",
                        date
                );

        Files.createDirectories(
                reportsFolder
        );

        // Create report name from Java file names
        StringBuilder reportName =
                new StringBuilder();

        for (int i = 0;
                i < fileNames.size();
                i++) {

            String name =
                    fileNames.get(i);

            if (name.toLowerCase()
                    .endsWith(".java")) {

                name = name.substring(
                        0,
                        name.length() - 5
                );
            }

            if (i > 0) {
                reportName.append("_");
            }

            reportName.append(name);
        }

        if (reportName.length() == 0) {
            reportName.append(
                    "CleanCheck_Report"
            );
        }

        reportName.append(".html");

        Path reportPath =
                reportsFolder.resolve(
                        reportName.toString()
                );

        StringBuilder html =
                new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html>
                <head>

                    <meta charset="UTF-8">

                    <title>CleanCheck Report</title>

                    <style>
                    
                        body {
                            font-family: Arial, sans-serif;
                            margin: 40px;
                            background: #f5f5f5;
                        }
                    
                        .container {
                            background: white;
                            padding: 30px;
                            border-radius: 10px;
                        }
                    
                        h1 {
                            text-align: center;
                        }
                    
                        .summary {
                            display: flex;
                            gap: 20px;
                            margin: 25px 0;
                        }
                    
                        .card {
                            padding: 20px;
                            border: 1px solid #ddd;
                            border-radius: 8px;
                            flex: 1;
                            text-align: center;
                        }
                    
                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 25px;
                        }
                    
                        th, td {
                            border: 1px solid #ddd;
                            padding: 10px;
                            text-align: left;
                        }
                    
                        th {
                            background: #eee;
                        }
                    
                        /* Severity Colors */
                    
                        .info {
                            color: #155724;
                            background-color: #d4edda;
                            padding: 5px 10px;
                            border-radius: 5px;
                            font-weight: bold;
                        }
                    
                        .warning {
                            color: #856404;
                            background-color: #fff3cd;
                            padding: 5px 10px;
                            border-radius: 5px;
                            font-weight: bold;
                        }
                    
                        .critical {
                            color: #721c24;
                            background-color: #f8d7da;
                            padding: 5px 10px;
                            border-radius: 5px;
                            font-weight: bold;
                        }
                    
                    </style>

                </head>

                <body>

                <div class="container">

                    <h1>CleanCheck Report</h1>

                    <p>
                        <b>Project:</b>
                """);

        html.append(projectName);

        html.append("""
                    </p>

                    <p>
                        <b>Date:</b>
                """);

        html.append(date);

        html.append("""
                    </p>

                    <div class="summary">

                        <div class="card">

                            <h2>
                """);

        html.append(score);

        html.append("""
                            / 100
                            </h2>

                            <p>Quality Score</p>

                        </div>

                        <div class="card">

                            <h2>
                """);

        html.append(warnings.size());

        html.append("""
                            </h2>

                            <p>Total Issues</p>

                        </div>

                        <div class="card">

                            <h2>
                """);

        html.append(fileNames.size());

        html.append("""
                            </h2>

                            <p>Files Checked</p>

                        </div>

                    </div>

                    <h2>Detected Issues</h2>

                    <table>

                        <tr>
                            <th>File</th>
                            <th>Line</th>
                            <th>Severity</th>
                            <th>Rule</th>
                            <th>Message</th>
                            <th>Suggestion</th>
                        </tr>
                """);

        for (C_Warning warning : warnings) {

            html.append("<tr>");

            html.append("<td>")
                    .append(
                            warning.getFileName()
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            warning.getLineNumber()
                    )
                    .append("</td>");

          String severity =
        warning.getSeverity().toString();

String severityClass =
        severity.toLowerCase();

html.append("<td>")
        .append("<span class=\"")
        .append(severityClass)
        .append("\">")
        .append(severity)
        .append("</span>")
        .append("</td>");

            html.append("<td>")
                    .append(
                            warning.getRuleName()
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            warning.getMessage()
                    )
                    .append("</td>");

            html.append("<td>")
                    .append(
                            warning.getSuggestion()
                    )
                    .append("</td>");

            html.append("</tr>");
        }

        html.append("""
                    </table>

                </div>

                </body>
                </html>
                """);

        try (FileWriter writer =
                new FileWriter(
                        reportPath.toFile()
                )) {

            writer.write(
                    html.toString()
            );
        }

        return reportPath.toString();
    }
}