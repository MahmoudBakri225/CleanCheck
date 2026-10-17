package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Quality history table of the HTML report (reads the SQLite history). */
final class QualityHistorySection {

    private QualityHistorySection() {
    }

    /*
     * =========================================================
     * QUALITY HISTORY
     * =========================================================
     */
    static void appendQualityHistory(
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
                = DatabaseConnection.getConnection(); PreparedStatement statement
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
                                    ReportFormat.escapeHtml(
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
}
