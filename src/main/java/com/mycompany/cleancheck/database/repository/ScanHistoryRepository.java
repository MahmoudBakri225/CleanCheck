package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.application.ConsoleOutput;

import com.mycompany.cleancheck.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ScanHistoryRepository {

    /*
     * =========================================================
     * PRINT SCAN HISTORY
     * =========================================================
     */

    public void printScanHistory(int projectId) {

        String sql = """
                SELECT
                    ScanId,
                    ScanDate,
                    QualityScore,
                    TotalIssues,
                    CriticalCount,
                    WarningCount,
                    InfoCount
                FROM Scan
                WHERE ProjectId = ?
                ORDER BY ScanId ASC
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    projectId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                ConsoleOutput.out().println();

                ConsoleOutput.out().println(
                        "============================================================"
                );

                ConsoleOutput.out().println(
                        "                    SCAN HISTORY"
                );

                ConsoleOutput.out().println(
                        "============================================================"
                );

                boolean found = false;

                while (resultSet.next()) {

                    found = true;

                    int scanId =
                            resultSet.getInt(
                                    "ScanId"
                            );

                    String scanDate =
                            resultSet.getString(
                                    "ScanDate"
                            );

                    int qualityScore =
                            resultSet.getInt(
                                    "QualityScore"
                            );

                    int totalIssues =
                            resultSet.getInt(
                                    "TotalIssues"
                            );

                    int criticalCount =
                            resultSet.getInt(
                                    "CriticalCount"
                            );

                    int warningCount =
                            resultSet.getInt(
                                    "WarningCount"
                            );

                    int infoCount =
                            resultSet.getInt(
                                    "InfoCount"
                            );

                    ConsoleOutput.out().println(
                            "Scan ID       : "
                                    + scanId
                    );

                    ConsoleOutput.out().println(
                            "Date          : "
                                    + scanDate
                    );

                    ConsoleOutput.out().println(
                            "Quality Score : "
                                    + qualityScore
                                    + " / 100"
                    );

                    ConsoleOutput.out().println(
                            "Total Issues  : "
                                    + totalIssues
                    );

                    ConsoleOutput.out().println(
                            "Critical      : "
                                    + criticalCount
                    );

                    ConsoleOutput.out().println(
                            "Warnings      : "
                                    + warningCount
                    );

                    ConsoleOutput.out().println(
                            "Information   : "
                                    + infoCount
                    );

                    ConsoleOutput.out().println(
                            "------------------------------------------------------------"
                    );
                }

                if (!found) {

                    ConsoleOutput.out().println(
                            "No scan history found."
                    );
                }

                ConsoleOutput.out().println(
                        "============================================================"
                );
            }

        } catch (Exception e) {

            ConsoleOutput.out().println(
                    "Failed to load scan history!"
            );

            java.util.logging.Logger.getLogger("CleanCheck").log(java.util.logging.Level.WARNING, "Operation failed", e);
        }
    }

    /*
     * =========================================================
     * PRINT QUALITY TREND
     * =========================================================
     *
     * Displays only the latest 10 scans.
     *
     * Database:
     *     Newest -> Oldest
     *
     * Console:
     *     Oldest -> Newest
     *
     * This makes the trend easier to read.
     * =========================================================
     */

    public void printQualityTrend(int projectId) {

        String sql = """
                SELECT
                    ScanId,
                    ScanDate,
                    QualityScore,
                    TotalIssues
                FROM Scan
                WHERE ProjectId = ?
                ORDER BY ScanId DESC
                LIMIT 10
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    projectId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                ConsoleOutput.out().println();

                ConsoleOutput.out().println(
                        "============================================================"
                );

                ConsoleOutput.out().println(
                        "                  QUALITY TREND"
                );

                ConsoleOutput.out().println(
                        "                  LAST 10 SCANS"
                );

                ConsoleOutput.out().println(
                        "============================================================"
                );

                boolean found = false;

                /*
                 * =================================================
                 * STORE RESULTS
                 * =================================================
                 *
                 * SQL returns:
                 *
                 * 129
                 * 128
                 * 127
                 * ...
                 *
                 * We store them first so we can print:
                 *
                 * 120
                 * 121
                 * 122
                 * ...
                 * 129
                 */

                List<String> trends =
                        new ArrayList<>();

                while (resultSet.next()) {

                    found = true;

                    int scanId =
                            resultSet.getInt(
                                    "ScanId"
                            );

                    String scanDate =
                            resultSet.getString(
                                    "ScanDate"
                            );

                    int qualityScore =
                            resultSet.getInt(
                                    "QualityScore"
                            );

                    int totalIssues =
                            resultSet.getInt(
                                    "TotalIssues"
                            );

                    trends.add(
                            "Scan "
                                    + scanId
                                    + " | "
                                    + scanDate
                                    + " | Score: "
                                    + qualityScore
                                    + " / 100"
                                    + " | Issues: "
                                    + totalIssues
                    );
                }

                /*
                 * =================================================
                 * PRINT RESULTS
                 * =================================================
                 */

                if (!found) {

                    ConsoleOutput.out().println(
                            "No quality trend available."
                    );

                } else {

                    /*
                     * Results were loaded newest -> oldest.
                     *
                     * Print them in reverse:
                     * oldest -> newest.
                     */

                    for (int i =
                            trends.size() - 1;
                            i >= 0;
                            i--) {

                        ConsoleOutput.out().println(
                                trends.get(i)
                        );
                    }
                }

                ConsoleOutput.out().println(
                        "============================================================"
                );
            }

        } catch (Exception e) {

            ConsoleOutput.out().println(
                    "Failed to load quality trend!"
            );

            java.util.logging.Logger.getLogger("CleanCheck").log(java.util.logging.Level.WARNING, "Operation failed", e);
        }
    }
}

