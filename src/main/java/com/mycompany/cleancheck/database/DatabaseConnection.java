package com.mycompany.cleancheck.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Provides a stable, per-user SQLite connection that remains writable after installation.
 * Each call opens a short-lived connection with foreign keys and a busy timeout enabled.
 * Override path with -Dcleancheck.database.path=/absolute/path/cleancheck.db
 */
public final class DatabaseConnection {

    private static final String DATABASE_FILE = "cleancheck.db";
    private static final String DATABASE_PATH_PROPERTY = "cleancheck.database.path";

    public DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            String configuredPath = System.getProperty(DATABASE_PATH_PROPERTY);
            Path databaseFile = configuredPath == null || configuredPath.isBlank()
                    ? defaultDatabasePath()
                    : Path.of(configuredPath).toAbsolutePath().normalize();
            Path databaseDirectory = databaseFile.getParent();
            if (databaseDirectory != null) {
                Files.createDirectories(databaseDirectory);
            }
            // Never append "?..." to the path — on Windows that becomes part of the file name.
            String url = "jdbc:sqlite:" + databaseFile.toString().replace('\\', '/');
            Properties properties = new Properties();
            // Mild concurrency help without embedding query parameters in the path.
            properties.setProperty("busy_timeout", "5000");

            Connection connection = DriverManager.getConnection(url, properties);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
                // WAL improves concurrent readers; ignore if the filesystem rejects it.
                try {
                    statement.execute("PRAGMA journal_mode = WAL");
                } catch (SQLException walUnsupported) {
                    // Stay on the default journal mode.
                }
            } catch (SQLException configurationFailure) {
                try {
                    connection.close();
                } catch (SQLException closeFailure) {
                    configurationFailure.addSuppressed(closeFailure);
                }
                throw new SQLException("Unable to configure CleanCheck SQLite connection.", configurationFailure);
            }
            return connection;

        } catch (SQLException e) {
            throw e;
        } catch (java.io.IOException | RuntimeException e) {
            throw new SQLException("Unable to prepare CleanCheck database directory.", e);
        }
    }

    private static Path defaultDatabasePath() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            throw new IllegalStateException("The user home directory is unavailable.");
        }
        return Path.of(home, ".cleancheck", "database", DATABASE_FILE).toAbsolutePath().normalize();
    }
}
