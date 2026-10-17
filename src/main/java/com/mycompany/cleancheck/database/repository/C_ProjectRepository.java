package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.C_DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class C_ProjectRepository {

    public int saveProject(String projectName, String projectPath) {

        // First, check if the project already exists
        String selectSql = """
                SELECT ProjectId
                FROM Project
                WHERE ProjectPath = ?
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection();
             PreparedStatement selectStatement =
                     connection.prepareStatement(selectSql)) {

            selectStatement.setString(1, projectPath);

            try (ResultSet resultSet =
                         selectStatement.executeQuery()) {

                if (resultSet.next()) {

                    int existingProjectId =
                            resultSet.getInt("ProjectId");

                    System.out.println(
                            "Project already exists."
                    );

                    System.out.println(
                            "Using Project ID: "
                                    + existingProjectId
                    );

                    return existingProjectId;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to check existing project!"
            );

            e.printStackTrace();

            return -1;
        }

        // Project does not exist, so create it
        String insertSql = """
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
                """;

        try (Connection connection =
                     C_DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             insertSql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, projectName);
            statement.setString(2, projectPath);

            statement.executeUpdate();

            try (ResultSet resultSet =
                         statement.getGeneratedKeys()) {

                if (resultSet.next()) {

                    int projectId =
                            resultSet.getInt(1);

                    System.out.println(
                            "New project saved."
                    );

                    System.out.println(
                            "Project ID: "
                                    + projectId
                    );

                    return projectId;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to save project!"
            );

            e.printStackTrace();
        }

        return -1;
    }
}