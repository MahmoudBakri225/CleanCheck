package com.mycompany.cleancheck.database.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.database.DatabaseConnection;
import com.mycompany.cleancheck.database.DatabaseInitializer;
import com.mycompany.cleancheck.database.repository.ProjectRepository;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanPersistenceServiceTest {

    private static final String DATABASE_PROPERTY = "cleancheck.database.path";
    private String previousDatabasePath;

    @AfterEach
    void restoreDatabasePath() {
        if (previousDatabasePath == null) {
            System.clearProperty(DATABASE_PROPERTY);
        } else {
            System.setProperty(DATABASE_PROPERTY, previousDatabasePath);
        }
    }

    @Test
    void scanAndFindingsRollBackTogetherIfAnyFindingCannotBeSaved(@TempDir Path temporaryDirectory)
            throws Exception {
        previousDatabasePath = System.getProperty(DATABASE_PROPERTY);
        System.setProperty(DATABASE_PROPERTY, temporaryDirectory.resolve("cleancheck.db").toString());
        DatabaseInitializer.initializeDatabase();
        int projectId = new ProjectRepository().saveProject("atomic", "atomic-project");
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TRIGGER reject_test_finding BEFORE INSERT ON Issue
                    WHEN NEW.RuleName = 'FAIL'
                    BEGIN SELECT RAISE(ABORT, 'forced test failure'); END
                    """);
        }

        List<Warning> findings = List.of(
                finding("First", "src/A.java"),
                finding("FAIL", "src/B.java"));
        assertThrows(IllegalStateException.class,
                () -> new ScanPersistenceService().save(projectId, 90, 2, 0, 2, 0, findings));

        assertEquals(0, countRows("Scan"));
        assertEquals(0, countRows("Issue"));
    }

    private static Warning finding(String rule, String file) {
        return new Warning(file, 1, rule, Severity.WARNING, "message", "suggestion");
    }

    private static int countRows(String table) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
