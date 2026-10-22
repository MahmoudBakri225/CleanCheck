package com.mycompany.cleancheck.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

/** Explicit maintenance command for deleting a single project's scan history. */
public final class DatabaseHistoryReset {

    private DatabaseHistoryReset() {
    }

    /** Usage: DatabaseHistoryReset <project-id> --confirm */
    public static void main(String[] args) {
        if (args.length != 2 || !"--confirm".equals(args[1])) {
            System.err.println("Usage: DatabaseHistoryReset <project-id> --confirm");
            System.err.println("This permanently deletes that project's scans, issues, and baseline.");
            System.exit(2);
        }

        final int projectId;
        try {
            projectId = Integer.parseInt(args[0]);
            if (projectId <= 0) {
                throw new NumberFormatException("project ID must be positive");
            }
        } catch (NumberFormatException invalid) {
            System.err.println("Invalid project ID: " + args[0]);
            System.exit(2);
            return;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int baselines = execute(connection,
                        "DELETE FROM Baseline WHERE ProjectId = ?", projectId);
                int issues = execute(connection,
                        "DELETE FROM Issue WHERE ScanId IN (SELECT ScanId FROM Scan WHERE ProjectId = ?)", projectId);
                int scans = execute(connection,
                        "DELETE FROM Scan WHERE ProjectId = ?", projectId);
                connection.commit();
                System.out.printf("Project %d history deleted: %d baseline(s), %d scan(s), %d issue(s).%n",
                        projectId, baselines, scans, issues);
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        } catch (Exception failure) {
            System.err.println("Could not reset project " + projectId + " history: " + failure.getMessage());
            System.exit(1);
        }
    }

    private static int execute(Connection connection, String sql, int projectId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, projectId);
            return statement.executeUpdate();
        }
    }
}
