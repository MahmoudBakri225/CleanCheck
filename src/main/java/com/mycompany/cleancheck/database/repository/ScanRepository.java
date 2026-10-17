package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ScanRepository {

    public int saveScan(
            int projectId,
            int qualityScore,
            int totalIssues,
            int criticalCount,
            int warningCount,
            int infoCount) {

        try (Connection connection = DatabaseConnection.getConnection()) {
            return saveScan(connection, projectId, qualityScore, totalIssues,
                    criticalCount, warningCount, infoCount);

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save scan for project " + projectId + ".", e);
        }
    }

    public int saveScan(Connection connection, int projectId, int qualityScore, int totalIssues,
            int criticalCount, int warningCount, int infoCount) throws SQLException {
        String sql = """
                INSERT INTO Scan
                (
                    ProjectId, ScanDate, QualityScore, TotalIssues,
                    CriticalCount, WarningCount, InfoCount
                )
                VALUES (?, datetime('now'), ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, projectId);
            statement.setInt(2, qualityScore);
            statement.setInt(3, totalIssues);
            statement.setInt(4, criticalCount);
            statement.setInt(5, warningCount);
            statement.setInt(6, infoCount);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new IllegalStateException("Database did not return an ID for the saved scan.");
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
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getInt("ScanId");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load the latest scan for project " + projectId + ".", e);
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
                     DatabaseConnection.getConnection();

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

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load the previous scan for project " + projectId + ".", e);
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
        String upsertSql = """
                INSERT INTO Baseline (ProjectId, ScanId, CreatedAt)
                SELECT ?, ?, datetime('now')
                WHERE EXISTS (
                    SELECT 1 FROM Scan WHERE ScanId = ? AND ProjectId = ?
                )
                ON CONFLICT(ProjectId) DO UPDATE SET
                    ScanId = excluded.ScanId,
                    CreatedAt = excluded.CreatedAt
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
                statement.setInt(1, projectId);
                statement.setInt(2, scanId);
                statement.setInt(3, scanId);
                statement.setInt(4, projectId);
                return statement.executeUpdate() == 1;
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to set baseline for project " + projectId + ".", e);
        }
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
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, projectId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getInt("ScanId");
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load the baseline scan for project " + projectId + ".", e);
        }

        return -1;
    }
}

