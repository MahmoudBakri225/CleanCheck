package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.DatabaseConnection;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read queries used by the regression and quality-history screens.
 *
 * @author KA
 */
public class ScanSummaryRepository {

    private static final String FIND_PROJECT
            = "SELECT ProjectId FROM Project WHERE ProjectPath = ? LIMIT 1";
    private static final String FIND_LATEST
            = "SELECT ScanId, ScanDate, QualityScore, TotalIssues, CriticalCount, "
            + "WarningCount, InfoCount FROM Scan WHERE ProjectId = ? "
            + "ORDER BY ScanId DESC LIMIT ?";

    public ScanSummaryRepository() {
    }

    /** Returns the stored project id for a folder, or -1 when it was never scanned. */
    public int findProjectId(Path path) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_PROJECT)) {
            statement.setString(1, path.toAbsolutePath().normalize().toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : -1;
            }
        }
    }

    /** Returns up to {@code limit} scans of a project, newest first. */
    public List<ScanSummary> findLatest(int projectId, int limit) throws SQLException {
        List<ScanSummary> scans = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(FIND_LATEST)) {
            statement.setInt(1, projectId);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    scans.add(new ScanSummary(
                            rs.getInt("ScanId"),
                            rs.getString("ScanDate"),
                            rs.getInt("QualityScore"),
                            rs.getInt("TotalIssues"),
                            rs.getInt("CriticalCount"),
                            rs.getInt("WarningCount"),
                            rs.getInt("InfoCount")));
                }
            }
        }
        return scans;
    }
}
