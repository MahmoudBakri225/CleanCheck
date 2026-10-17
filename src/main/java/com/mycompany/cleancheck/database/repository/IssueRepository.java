package com.mycompany.cleancheck.database.repository;

import com.mycompany.cleancheck.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IssueRepository {

    public void saveIssue(
            int scanId,
            String fileName,
            int lineNumber,
            String ruleName,
            String severity,
            String message,
            String suggestion) {

        try (Connection connection = DatabaseConnection.getConnection()) {
            saveIssue(connection, scanId, fileName, lineNumber, ruleName, severity, message, suggestion);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save issue for scan " + scanId, e);
        }
    }

    public void saveIssue(Connection connection, int scanId, String fileName, int lineNumber,
            String ruleName, String severity, String message, String suggestion) throws SQLException {
        String sql = """
                INSERT INTO Issue
                (
                    ScanId, FileName, LineNumber, RuleName,
                    Severity, Message, Suggestion
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, scanId);
            statement.setString(2, fileName);
            statement.setInt(3, lineNumber);
            statement.setString(4, ruleName);
            statement.setString(5, severity);
            statement.setString(6, message);
            statement.setString(7, suggestion);

            statement.executeUpdate();
        }
    }
}
