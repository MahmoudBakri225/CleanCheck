package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Path;

public class ProjectRepository {

    public int saveProject(String projectName, String projectPath) {
        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("Project path is required.");
        }
        String normalizedPath = Path.of(projectPath).toAbsolutePath().normalize().toString();
        String upsertSql = """
                INSERT INTO Project
                (
                    ProjectName,
                    ProjectPath,
                    CreatedAt
                )
                VALUES
                (
                    ?,
                    ?,
                    datetime('now')
                )
                ON CONFLICT(ProjectPath) DO UPDATE SET ProjectName = excluded.ProjectName
                RETURNING ProjectId
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(upsertSql)) {
            statement.setString(1, projectName);
            statement.setString(2, normalizedPath);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save project.", e);
        }

        throw new IllegalStateException("Database did not return an ID for the saved project.");
    }
}
