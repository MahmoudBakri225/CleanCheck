package com.mycompany.cleancheck.database.service;

import com.mycompany.cleancheck.database.C_DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class C_ScanComparisonService {

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
                     C_DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {

                    System.out.println(
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

                    System.out.println(
                            "Only one scan exists."
                    );

                    System.out.println(
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

                System.out.println();
                System.out.println(
                        "============================================================"
                );
                System.out.println(
                        "                  SCAN COMPARISON"
                );
                System.out.println(
                        "============================================================"
                );

                System.out.println(
                        "Previous Scan ID : "
                                + previousScanId
                );

                System.out.println(
                        "Current Scan ID  : "
                                + currentScanId
                );

                System.out.println(
                        "------------------------------------------------------------"
                );

                System.out.println(
                        "Quality Score"
                );

                System.out.println(
                        "Previous : "
                                + previousScore
                );

                System.out.println(
                        "Current  : "
                                + currentScore
                );

                System.out.println(
                        "Change   : "
                                + formatDifference(scoreDifference)
                );

                System.out.println(
                        "------------------------------------------------------------"
                );

                System.out.println(
                        "Total Issues"
                );

                System.out.println(
                        "Previous : "
                                + previousIssues
                );

                System.out.println(
                        "Current  : "
                                + currentIssues
                );

                System.out.println(
                        "Change   : "
                                + formatDifference(issueDifference)
                );

                System.out.println(
                        "------------------------------------------------------------"
                );

                System.out.println(
                        "Critical"
                );

                System.out.println(
                        "Previous : "
                                + previousCritical
                );

                System.out.println(
                        "Current  : "
                                + currentCritical
                );

                System.out.println(
                        "Change   : "
                                + formatDifference(criticalDifference)
                );

                System.out.println(
                        "------------------------------------------------------------"
                );

                System.out.println(
                        "Warnings"
                );

                System.out.println(
                        "Previous : "
                                + previousWarnings
                );

                System.out.println(
                        "Current  : "
                                + currentWarnings
                );

                System.out.println(
                        "Change   : "
                                + formatDifference(warningDifference)
                );

                System.out.println(
                        "------------------------------------------------------------"
                );

                System.out.println(
                        "Information"
                );

                System.out.println(
                        "Previous : "
                                + previousInfo
                );

                System.out.println(
                        "Current  : "
                                + currentInfo
                );

                System.out.println(
                        "Change   : "
                                + formatDifference(infoDifference)
                );

                System.out.println(
                        "------------------------------------------------------------"
                );

                if (scoreDifference > 0) {

                    System.out.println(
                            "Result: QUALITY SCORE IMPROVED"
                    );

                } else if (scoreDifference < 0) {

                    System.out.println(
                            "Result: QUALITY SCORE DECREASED"
                    );

                } else {

                    System.out.println(
                            "Result: QUALITY SCORE UNCHANGED"
                    );
                }

                System.out.println(
                        "============================================================"
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to compare scans!"
            );

            e.printStackTrace();
        }
    }

    private String formatDifference(int difference) {

        if (difference > 0) {
            return "+" + difference;
        }

        return String.valueOf(difference);
    }
}