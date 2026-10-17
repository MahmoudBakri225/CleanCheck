
package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.C_DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class C_ScanRepository {

    public int saveScan(
            int projectId,
            int qualityScore,
            int totalIssues,
            int criticalCount,
            int warningCount,
            int infoCount) {

        String sql = """
                INSERT INTO Scan
                (
                    ProjectId,
                    ScanDate,
                    QualityScore,
                    TotalIssues,
                    CriticalCount,
                    WarningCount,
                    InfoCount
                )
                VALUES
                (
                    ?,
                    datetime('now'),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, projectId);
            statement.setInt(2, qualityScore);
            statement.setInt(3, totalIssues);
            statement.setInt(4, criticalCount);
            statement.setInt(5, warningCount);
            statement.setInt(6, infoCount);

            statement.executeUpdate();

            try (ResultSet resultSet =
                         statement.getGeneratedKeys()) {

                if (resultSet.next()) {

                    return resultSet.getInt(1);
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to save scan!"
            );

            e.printStackTrace();
        }

        return -1;
    }

    /*
     * =========================================================
     * GET LATEST SCAN
     * =========================================================
     */

    public int getLatestScanId(int projectId) {

        String sql = """
                SELECT ScanId
                FROM Scan
                WHERE ProjectId = ?
                ORDER BY ScanId DESC
                LIMIT 1
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getInt("ScanId");
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to get latest scan!"
            );

            e.printStackTrace();
        }

        return -1;
    }

    /*
     * =========================================================
     * GET PREVIOUS HISTORICAL SCAN
     * =========================================================
     */

    public int getPreviousScanId(
            int projectId,
            int currentScanId) {

        String sql = """
                SELECT ScanId
                FROM Scan
                WHERE ProjectId = ?
                  AND ScanId < ?
                ORDER BY ScanId DESC
                LIMIT 1
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);
            statement.setInt(2, currentScanId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getInt("ScanId");
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to get previous scan!"
            );

            e.printStackTrace();
        }

        return -1;
    }

    /*
     * =========================================================
     * SET BASELINE
     * =========================================================
     *
     * Sets a specific scan as the baseline for a project.
     *
     * If the project already has a baseline,
     * the existing baseline will be updated.
     */

    public boolean setBaseline(
            int projectId,
            int scanId) {

        String updateSql = """
                UPDATE Baseline
                SET ScanId = ?,
                    CreatedAt = datetime('now')
                WHERE ProjectId = ?
                """;

        String insertSql = """
                INSERT INTO Baseline
                (
                    ProjectId,
                    ScanId,
                    CreatedAt
                )
                VALUES
                (
                    ?,
                    ?,
                    datetime('now')
                )
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection()) {

            /*
             * First try to update an existing baseline.
             */

            try (PreparedStatement statement =
                         connection.prepareStatement(updateSql)) {

                statement.setInt(1, scanId);
                statement.setInt(2, projectId);

                int updatedRows =
                        statement.executeUpdate();

                if (updatedRows > 0) {

                    return true;
                }
            }

            /*
             * If no baseline exists,
             * create a new one.
             */

            try (PreparedStatement statement =
                         connection.prepareStatement(insertSql)) {

                statement.setInt(1, projectId);
                statement.setInt(2, scanId);

                statement.executeUpdate();

                return true;
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to set baseline!"
            );

            e.printStackTrace();
        }

        return false;
    }

    /*
     * =========================================================
     * GET BASELINE SCAN
     * =========================================================
     *
     * Returns the ScanId selected as the baseline.
     *
     * Returns -1 if no baseline exists.
     */

    public int getBaselineScanId(int projectId) {

        String sql = """
                SELECT ScanId
                FROM Baseline
                WHERE ProjectId = ?
                LIMIT 1
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getInt("ScanId");
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to get baseline scan!"
            );

            e.printStackTrace();
        }

        return -1;
    }
}

