package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.C_DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class C_IssueRepository {

    public void saveIssue(
            int scanId,
            String fileName,
            int lineNumber,
            String ruleName,
            String severity,
            String message,
            String suggestion) {

        String sql = """
                INSERT INTO Issue
                (
                    ScanId,
                    FileName,
                    LineNumber,
                    RuleName,
                    Severity,
                    Message,
                    Suggestion
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = C_DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, scanId);
            statement.setString(2, fileName);
            statement.setInt(3, lineNumber);
            statement.setString(4, ruleName);
            statement.setString(5, severity);
            statement.setString(6, message);
            statement.setString(7, suggestion);

            statement.executeUpdate();

        } catch (Exception e) {

            System.out.println("Failed to save issue!");
            e.printStackTrace();
        }
    }
}