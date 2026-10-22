
package com.mycompany.cleancheck.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DatabaseInitializer {

    public static void initializeDatabase() throws SQLException {

        /*
         * =========================================================
         * PROJECT TABLE
         * =========================================================
         */

        String projectTable = """
                CREATE TABLE IF NOT EXISTS Project (
                    ProjectId INTEGER PRIMARY KEY AUTOINCREMENT,
                    ProjectName TEXT NOT NULL,
                    ProjectPath TEXT NOT NULL,
                    CreatedAt TEXT NOT NULL
                )
                """;

        /*
         * =========================================================
         * SCAN TABLE
         * =========================================================
         */

        String scanTable = """
                CREATE TABLE IF NOT EXISTS Scan (
                    ScanId INTEGER PRIMARY KEY AUTOINCREMENT,
                    ProjectId INTEGER NOT NULL,
                    ScanDate TEXT NOT NULL,
                    QualityScore INTEGER NOT NULL,
                    TotalIssues INTEGER NOT NULL,
                    CriticalCount INTEGER NOT NULL,
                    WarningCount INTEGER NOT NULL,
                    InfoCount INTEGER NOT NULL,

                    FOREIGN KEY (ProjectId)
                        REFERENCES Project(ProjectId)
                        ON DELETE CASCADE
                )
                """;

        /*
         * =========================================================
         * ISSUE TABLE
         * =========================================================
         */

        String issueTable = """
                CREATE TABLE IF NOT EXISTS Issue (
                    IssueId INTEGER PRIMARY KEY AUTOINCREMENT,
                    ScanId INTEGER NOT NULL,
                    FileName TEXT NOT NULL,
                    LineNumber INTEGER NOT NULL,
                    RuleName TEXT NOT NULL,
                    Severity TEXT NOT NULL,
                    Message TEXT NOT NULL,
                    Suggestion TEXT NOT NULL,

                    FOREIGN KEY (ScanId)
                        REFERENCES Scan(ScanId)
                        ON DELETE CASCADE
                )
                """;

        /*
         * =========================================================
         * BASELINE TABLE
         * =========================================================
         *
         * Stores the selected baseline scan for each project.
         *
         * One project can have one active baseline.
         *
         * Example:
         *
         * ProjectId = 3
         * ScanId    = 85
         *
         * means:
         *
         * Project 3 -> Baseline = Scan 85
         */

        String baselineTable = """
                CREATE TABLE IF NOT EXISTS Baseline (
                    BaselineId INTEGER PRIMARY KEY AUTOINCREMENT,
                    ProjectId INTEGER NOT NULL UNIQUE,
                    ScanId INTEGER NOT NULL,
                    CreatedAt TEXT NOT NULL,

                    FOREIGN KEY (ProjectId)
                        REFERENCES Project(ProjectId)
                        ON DELETE CASCADE,

                    FOREIGN KEY (ScanId)
                        REFERENCES Scan(ScanId)
                        ON DELETE CASCADE
                )
                """;

        /*
         * =========================================================
         * CREATE TABLES
         * =========================================================
         */

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            try {
                statement.execute(projectTable);
                statement.execute(scanTable);
                statement.execute(issueTable);
                statement.execute(baselineTable);
                mergeDuplicateProjectPaths(connection);
                statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_project_path ON Project(ProjectPath)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_scan_project_id ON Scan(ProjectId, ScanId DESC)");
                statement.execute("CREATE INDEX IF NOT EXISTS ix_issue_scan_id ON Issue(ScanId)");
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    /** Merges historical duplicate project rows before a unique path index is installed. */
    private static void mergeDuplicateProjectPaths(Connection connection) throws SQLException {
        record DuplicatePath(String projectPath, int keeperId) {
        }
        List<DuplicatePath> duplicatePaths = new ArrayList<>();
        String pathsSql = "SELECT ProjectPath, MIN(ProjectId) AS KeeperId FROM Project "
                + "GROUP BY ProjectPath HAVING COUNT(*) > 1";
        try (Statement statement = connection.createStatement(); ResultSet paths = statement.executeQuery(pathsSql)) {
            while (paths.next()) {
                duplicatePaths.add(new DuplicatePath(paths.getString("ProjectPath"), paths.getInt("KeeperId")));
            }
        }

        for (DuplicatePath duplicate : duplicatePaths) {
            List<Integer> duplicateIds = new ArrayList<>();
            try (var projectIds = connection.prepareStatement(
                    "SELECT ProjectId FROM Project WHERE ProjectPath = ? AND ProjectId <> ?")) {
                projectIds.setString(1, duplicate.projectPath());
                projectIds.setInt(2, duplicate.keeperId());
                try (ResultSet rows = projectIds.executeQuery()) {
                    while (rows.next()) {
                        duplicateIds.add(rows.getInt(1));
                    }
                }
            }
            for (int duplicateId : duplicateIds) {
                boolean keeperHasBaseline;
                try (var baseline = connection.prepareStatement(
                        "SELECT 1 FROM Baseline WHERE ProjectId = ?")) {
                    baseline.setInt(1, duplicate.keeperId());
                    try (ResultSet result = baseline.executeQuery()) {
                        keeperHasBaseline = result.next();
                    }
                }
                String baselineSql = keeperHasBaseline
                        ? "DELETE FROM Baseline WHERE ProjectId = ?"
                        : "UPDATE Baseline SET ProjectId = ? WHERE ProjectId = ?";
                try (var baseline = connection.prepareStatement(baselineSql)) {
                    if (keeperHasBaseline) {
                        baseline.setInt(1, duplicateId);
                    } else {
                        baseline.setInt(1, duplicate.keeperId());
                        baseline.setInt(2, duplicateId);
                    }
                    baseline.executeUpdate();
                }
                try (var scans = connection.prepareStatement("UPDATE Scan SET ProjectId = ? WHERE ProjectId = ?")) {
                    scans.setInt(1, duplicate.keeperId());
                    scans.setInt(2, duplicateId);
                    scans.executeUpdate();
                }
                try (var project = connection.prepareStatement("DELETE FROM Project WHERE ProjectId = ?")) {
                    project.setInt(1, duplicateId);
                    project.executeUpdate();
                }
            }
        }
    }
}

