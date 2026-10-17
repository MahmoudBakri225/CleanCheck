
package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.C_DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class C_ScanHistoryRepository {

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
                     C_DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    projectId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                System.out.println();

                System.out.println(
                        "============================================================"
                );

                System.out.println(
                        "                    SCAN HISTORY"
                );

                System.out.println(
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

                    System.out.println(
                            "Scan ID       : "
                                    + scanId
                    );

                    System.out.println(
                            "Date          : "
                                    + scanDate
                    );

                    System.out.println(
                            "Quality Score : "
                                    + qualityScore
                                    + " / 100"
                    );

                    System.out.println(
                            "Total Issues  : "
                                    + totalIssues
                    );

                    System.out.println(
                            "Critical      : "
                                    + criticalCount
                    );

                    System.out.println(
                            "Warnings      : "
                                    + warningCount
                    );

                    System.out.println(
                            "Information   : "
                                    + infoCount
                    );

                    System.out.println(
                            "------------------------------------------------------------"
                    );
                }

                if (!found) {

                    System.out.println(
                            "No scan history found."
                    );
                }

                System.out.println(
                        "============================================================"
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to load scan history!"
            );

            e.printStackTrace();
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
                     C_DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    projectId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                System.out.println();

                System.out.println(
                        "============================================================"
                );

                System.out.println(
                        "                  QUALITY TREND"
                );

                System.out.println(
                        "                  LAST 10 SCANS"
                );

                System.out.println(
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

                    System.out.println(
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

                        System.out.println(
                                trends.get(i)
                        );
                    }
                }

                System.out.println(
                        "============================================================"
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to load quality trend!"
            );

            e.printStackTrace();
        }
    }
}

