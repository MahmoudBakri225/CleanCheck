package com.mycompany.cleancheck.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Provides a stable, project-root-relative SQLite connection. */
public final class DatabaseConnection {

    private static final String DATABASE_FILE = "cleancheck.db";
    private static final String DATABASE_PATH_PROPERTY = "cleancheck.database.path";

    public DatabaseConnection() {
    }

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
                throw new SQLException("Unable to configure CleanCheck SQLite connection.", configurationFailure);
            }
            return connection;

        } catch (Exception e) {
            throw new SQLException("Unable to prepare CleanCheck database directory.", e);
        }
    }
}
