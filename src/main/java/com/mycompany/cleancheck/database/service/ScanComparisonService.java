package com.mycompany.cleancheck.database.service;

import com.mycompany.cleancheck.application.ConsoleOutput;
import com.mycompany.cleancheck.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ScanComparisonService {

    public void compareLatestScans(int projectId) {

        String sql = """
                SELECT
                    ScanId,
                    QualityScore,
                    TotalIssues,
                    CriticalCount,
                    WarningCount,
                    InfoCount
                FROM Scan
                WHERE ProjectId = ?
                ORDER BY ScanId DESC
                LIMIT 2
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {

                    ConsoleOutput.out().println(
                            "No scans found for this project."
                    );

                    return;
                }

                int currentScanId =
                        resultSet.getInt("ScanId");

                int currentScore =
                        resultSet.getInt("QualityScore");

                int currentIssues =
                        resultSet.getInt("TotalIssues");

                int currentCritical =
                        resultSet.getInt("CriticalCount");

                int currentWarnings =
                        resultSet.getInt("WarningCount");

                int currentInfo =
                        resultSet.getInt("InfoCount");

                if (!resultSet.next()) {

                    ConsoleOutput.out().println(
                            "Only one scan exists."
                    );

                    ConsoleOutput.out().println(
                            "A previous scan is required for comparison."
                    );

                    return;
                }

                int previousScanId =
                        resultSet.getInt("ScanId");

                int previousScore =
                        resultSet.getInt("QualityScore");

                int previousIssues =
                        resultSet.getInt("TotalIssues");

                int previousCritical =
                        resultSet.getInt("CriticalCount");

                int previousWarnings =
                        resultSet.getInt("WarningCount");

                int previousInfo =
                        resultSet.getInt("InfoCount");

                int scoreDifference =
                        currentScore - previousScore;

                int issueDifference =
                        currentIssues - previousIssues;

                int criticalDifference =
                        currentCritical - previousCritical;

                int warningDifference =
                        currentWarnings - previousWarnings;

                int infoDifference =
                        currentInfo - previousInfo;

                ConsoleOutput.out().println();
                ConsoleOutput.out().println(
                        "============================================================"
                );
                ConsoleOutput.out().println(
                        "                  SCAN COMPARISON"
                );
                ConsoleOutput.out().println(
                        "============================================================"
                );

                ConsoleOutput.out().println(
                        "Previous Scan ID : "
                                + previousScanId
                );

                ConsoleOutput.out().println(
                        "Current Scan ID  : "
                                + currentScanId
                );

                ConsoleOutput.out().println(
                        "------------------------------------------------------------"
                );

                ConsoleOutput.out().println(
                        "Quality Score"
                );

                ConsoleOutput.out().println(
                        "Previous : "
                                + previousScore
                );

                ConsoleOutput.out().println(
                        "Current  : "
                                + currentScore
                );

                ConsoleOutput.out().println(
                        "Change   : "
                                + formatDifference(scoreDifference)
                );

                ConsoleOutput.out().println(
                        "------------------------------------------------------------"
                );

                ConsoleOutput.out().println(
                        "Total Issues"
                );

                ConsoleOutput.out().println(
                        "Previous : "
                                + previousIssues
                );

                ConsoleOutput.out().println(
                        "Current  : "
                                + currentIssues
                );

                ConsoleOutput.out().println(
                        "Change   : "
                                + formatDifference(issueDifference)
                );

                ConsoleOutput.out().println(
                        "------------------------------------------------------------"
                );

                ConsoleOutput.out().println(
                        "Critical"
                );

                ConsoleOutput.out().println(
                        "Previous : "
                                + previousCritical
                );

                ConsoleOutput.out().println(
                        "Current  : "
                                + currentCritical
                );

                ConsoleOutput.out().println(
                        "Change   : "
                                + formatDifference(criticalDifference)
                );

                ConsoleOutput.out().println(
                        "------------------------------------------------------------"
                );

                ConsoleOutput.out().println(
                        "Warnings"
                );

                ConsoleOutput.out().println(
                        "Previous : "
                                + previousWarnings
                );

                ConsoleOutput.out().println(
                        "Current  : "
                                + currentWarnings
                );

                ConsoleOutput.out().println(
                        "Change   : "
                                + formatDifference(warningDifference)
                );

                ConsoleOutput.out().println(
                        "------------------------------------------------------------"
                );

                ConsoleOutput.out().println(
                        "Information"
                );

                ConsoleOutput.out().println(
                        "Previous : "
                                + previousInfo
                );

                ConsoleOutput.out().println(
                        "Current  : "
                                + currentInfo
                );

                ConsoleOutput.out().println(
                        "Change   : "
                                + formatDifference(infoDifference)
                );

                ConsoleOutput.out().println(
                        "------------------------------------------------------------"
                );

                if (scoreDifference > 0) {

                    ConsoleOutput.out().println(
                            "Result: QUALITY SCORE IMPROVED"
                    );

                } else if (scoreDifference < 0) {

                    ConsoleOutput.out().println(
                            "Result: QUALITY SCORE DECREASED"
                    );

                } else {

                    ConsoleOutput.out().println(
                            "Result: QUALITY SCORE UNCHANGED"
                    );
                }

                ConsoleOutput.out().println(
                        "============================================================"
                );
            }

        } catch (Exception e) {
            java.util.logging.Logger.getLogger("CleanCheck").log(java.util.logging.Level.WARNING, "Operation failed", e);
            throw new IllegalStateException("Failed to compare scans.", e);
        }
    }

    private String formatDifference(int difference) {

        if (difference > 0) {
            return "+" + difference;
        }

        return String.valueOf(difference);
    }
}

