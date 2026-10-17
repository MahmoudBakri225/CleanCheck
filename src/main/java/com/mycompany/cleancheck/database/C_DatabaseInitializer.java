
package com.mycompany.cleancheck.database;

import java.sql.Connection;
import java.sql.Statement;

public class C_DatabaseInitializer {

    public static void initializeDatabase() {

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
                        REFERENCES Project(ProjectId),

                    FOREIGN KEY (ScanId)
                        REFERENCES Scan(ScanId)
                )
                """;

        /*
         * =========================================================
         * CREATE TABLES
         * =========================================================
         */

        try (Connection connection =
                     C_DatabaseConnection.getConnection();

             Statement statement =
                     connection.createStatement()) {

            statement.execute(projectTable);

            statement.execute(scanTable);

            statement.execute(issueTable);

            statement.execute(baselineTable);

            System.out.println(
                    "Database tables created successfully!"
            );

        } catch (Exception e) {

            System.out.println(
                    "Database tables creation failed!"
            );

            e.printStackTrace();
        }
    }
}

