package com.mycompany.cleancheck.database.service;

import com.mycompany.cleancheck.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Loads the stored issues of one scan from SQLite. */
final class IssueLoader {

    private IssueLoader() {
    }

    /*
     * =========================================================
     * LOAD ISSUES
     * =========================================================
     */

    static List<IssueData> loadIssues(
            int scanId
    ) {

        List<IssueData> issues =
                new ArrayList<>();

        String sql =
                "SELECT FileName, "
                        + "LineNumber, "
                        + "RuleName, "
                        + "Severity, "
                        + "Message, "
                        + "Suggestion "
                        + "FROM Issue "
                        + "WHERE ScanId = ? "
                        + "ORDER BY FileName, "
                        + "RuleName, "
                        + "LineNumber";

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(
                    1,
                    scanId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                while (resultSet.next()) {

                    IssueData issue =
                            new IssueData();

                    issue.fileName =
                            resultSet.getString(
                                    "FileName"
                            );

                    issue.lineNumber =
                            resultSet.getInt(
                                    "LineNumber"
                            );

                    issue.ruleName =
                            resultSet.getString(
                                    "RuleName"
                            );

                    issue.severity =
                            resultSet.getString(
                                    "Severity"
                            );

                    issue.message =
                            resultSet.getString(
                                    "Message"
                            );

                    issue.suggestion =
                            resultSet.getString(
                                    "Suggestion"
                            );

                    issue.fingerprint =
                            IssueMatcher.createFingerprint(
                                    issue.fileName,
                                    issue.ruleName,
                                    issue.message
                            );

                    issues.add(
                            issue
                    );
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Unable to load issue history for scan " + scanId, e);
        }

        return issues;
    }

    /*
     * =========================================================
     * COMPARISON RESULT
     * =========================================================
     */
}
