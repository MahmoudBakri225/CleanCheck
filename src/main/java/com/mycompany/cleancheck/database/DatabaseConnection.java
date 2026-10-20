package com.mycompany.cleancheck.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides a stable, project-root-relative SQLite connection.
 *
 * <p>Callers <b>must</b> close the returned connection (prefer try-with-resources).
 * Each call opens a fresh connection. For the desktop UI, prefer short-lived
 * connections rather than holding one open for the whole application lifetime.
 *
 * <p>A full connection pool is intentionally not used: SQLite is file-based and
 * the tool opens connections only for short persistence operations.
 */
public final class DatabaseConnection {

    private static final String DATABASE_FILE = "cleancheck.db";
    private static final String DATABASE_PATH_PROPERTY = "cleancheck.database.path";

    private DatabaseConnection() {
        // utility class
    }

    /**
     * Opens a new SQLite connection with foreign keys enabled and a busy timeout.
     *
     * @return a live connection that the caller must close
     * @throws SQLException if the database directory cannot be prepared or the connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            String configuredPath = System.getProperty(DATABASE_PATH_PROPERTY);
            Path databaseFile = configuredPath == null || configuredPath.isBlank()
                    ? Paths.get("database", DATABASE_FILE).toAbsolutePath().normalize()
                    : Paths.get(configuredPath).toAbsolutePath().normalize();

            Path databaseDirectory = databaseFile.getParent();
            if (databaseDirectory != null) {
                Files.createDirectories(databaseDirectory);
            }

            String url = "jdbc:sqlite:" + databaseFile;
            Connection connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            } catch (SQLException configurationFailure) {
                try {
                    connection.close();
                } catch (SQLException closeFailure) {
                    configurationFailure.addSuppressed(closeFailure);
                }
                throw new SQLException(
                        "Unable to configure CleanCheck SQLite connection. "
                                + "Check that the database path is writable and not locked by another process.",
                        configurationFailure);
            }

            return connection;

        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException(
                    "Unable to prepare CleanCheck database directory. "
                            + "Ensure the working directory is writable.",
                    e);
        }
    }
}
