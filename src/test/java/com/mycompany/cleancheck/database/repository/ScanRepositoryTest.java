package com.mycompany.cleancheck.database.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.cleancheck.database.DatabaseInitializer;
import com.mycompany.cleancheck.database.DatabaseConnection;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScanRepositoryTest {

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
    void baselineCanOnlyBeSetToAScanOwnedByTheProject(@TempDir Path temporaryDirectory) throws Exception {
        useDatabase(temporaryDirectory.resolve("cleancheck.db"));
        DatabaseInitializer.initializeDatabase();

        ProjectRepository projects = new ProjectRepository();
        int firstProject = projects.saveProject("first", temporaryDirectory.resolve("first").toString());
        int secondProject = projects.saveProject("second", temporaryDirectory.resolve("second").toString());
        ScanRepository scans = new ScanRepository();
        int scan = scans.saveScan(firstProject, 95, 2, 0, 1, 1);

        assertEquals(-1, scans.getLatestScanId(secondProject));
        assertFalse(scans.setBaseline(secondProject, scan));
        assertEquals(-1, scans.getBaselineScanId(secondProject));
        assertTrue(scans.setBaseline(firstProject, scan));
        assertEquals(scan, scans.getBaselineScanId(firstProject));
    }

    @Test
    void concurrentBaselineWritesRemainValidAndUnique(@TempDir Path temporaryDirectory) throws Exception {
        useDatabase(temporaryDirectory.resolve("cleancheck.db"));
        DatabaseInitializer.initializeDatabase();

        ProjectRepository projects = new ProjectRepository();
        int project = projects.saveProject("project", temporaryDirectory.resolve("project").toString());
        ScanRepository scans = new ScanRepository();
        int firstScan = scans.saveScan(project, 90, 0, 0, 0, 0);
        int secondScan = scans.saveScan(project, 91, 0, 0, 0, 0);

        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> writes = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                int scan = index % 2 == 0 ? firstScan : secondScan;
                writes.add(() -> scans.setBaseline(project, scan));
            }
            List<Future<Boolean>> results = workers.invokeAll(writes);
            for (Future<Boolean> result : results) {
                assertTrue(result.get());
            }
        } finally {
            workers.shutdownNow();
        }

        int baseline = scans.getBaselineScanId(project);
        assertTrue(baseline == firstScan || baseline == secondScan);
    }

    @Test
    void concurrentRegistrationOfTheSamePathReturnsOneProject(@TempDir Path temporaryDirectory) throws Exception {
        useDatabase(temporaryDirectory.resolve("cleancheck.db"));
        DatabaseInitializer.initializeDatabase();
        ProjectRepository projects = new ProjectRepository();
        String projectPath = temporaryDirectory.resolve("sample").toString();

        ExecutorService workers = Executors.newFixedThreadPool(8);
        List<Integer> ids;
        try {
            List<Callable<Integer>> registrations = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                registrations.add(() -> projects.saveProject("sample", projectPath));
            }
            List<Future<Integer>> results = workers.invokeAll(registrations);
            ids = new ArrayList<>();
            for (Future<Integer> result : results) {
                ids.add(result.get());
            }
        } finally {
            workers.shutdownNow();
        }

        assertTrue(ids.stream().allMatch(id -> id.equals(ids.getFirst())));
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM Project")) {
            assertTrue(rows.next());
            assertEquals(1, rows.getInt(1));
        }
    }

    @Test
    void initializationMergesLegacyDuplicateProjectsWithoutLosingScansOrBaseline(
            @TempDir Path temporaryDirectory) throws Exception {
        useDatabase(temporaryDirectory.resolve("legacy.db"));
        int keeper;
        int duplicate;
        int baselineScan;
        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE Project (ProjectId INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "ProjectName TEXT NOT NULL, ProjectPath TEXT NOT NULL, CreatedAt TEXT NOT NULL)");
            statement.execute("CREATE TABLE Scan (ScanId INTEGER PRIMARY KEY AUTOINCREMENT, ProjectId INTEGER NOT NULL, "
                    + "ScanDate TEXT NOT NULL, QualityScore INTEGER NOT NULL, TotalIssues INTEGER NOT NULL, "
                    + "CriticalCount INTEGER NOT NULL, WarningCount INTEGER NOT NULL, InfoCount INTEGER NOT NULL, "
                    + "FOREIGN KEY(ProjectId) REFERENCES Project(ProjectId))");
            statement.execute("CREATE TABLE Issue (IssueId INTEGER PRIMARY KEY AUTOINCREMENT, ScanId INTEGER NOT NULL, "
                    + "FileName TEXT NOT NULL, LineNumber INTEGER NOT NULL, RuleName TEXT NOT NULL, Severity TEXT NOT NULL, "
                    + "Message TEXT NOT NULL, Suggestion TEXT NOT NULL, FOREIGN KEY(ScanId) REFERENCES Scan(ScanId))");
            statement.execute("CREATE TABLE Baseline (BaselineId INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "ProjectId INTEGER NOT NULL UNIQUE, ScanId INTEGER NOT NULL, CreatedAt TEXT NOT NULL, "
                    + "FOREIGN KEY(ProjectId) REFERENCES Project(ProjectId), FOREIGN KEY(ScanId) REFERENCES Scan(ScanId))");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insertProject = connection.prepareStatement(
                    "INSERT INTO Project(ProjectName,ProjectPath,CreatedAt) VALUES(?,?,datetime('now'))",
                    Statement.RETURN_GENERATED_KEYS)) {
                insertProject.setString(1, "keeper");
                insertProject.setString(2, "same-path");
                insertProject.executeUpdate();
                try (ResultSet keys = insertProject.getGeneratedKeys()) {
                    keys.next();
                    keeper = keys.getInt(1);
                }
                insertProject.setString(1, "duplicate");
                insertProject.setString(2, "same-path");
                insertProject.executeUpdate();
                try (ResultSet keys = insertProject.getGeneratedKeys()) {
                    keys.next();
                    duplicate = keys.getInt(1);
                }
            }
            try (PreparedStatement insertScan = connection.prepareStatement(
                    "INSERT INTO Scan(ProjectId,ScanDate,QualityScore,TotalIssues,CriticalCount,WarningCount,InfoCount) "
                            + "VALUES(?,datetime('now'),90,0,0,0,0)", Statement.RETURN_GENERATED_KEYS)) {
                insertScan.setInt(1, keeper);
                insertScan.executeUpdate();
                insertScan.setInt(1, duplicate);
                insertScan.executeUpdate();
                try (ResultSet keys = insertScan.getGeneratedKeys()) {
                    keys.next();
                    baselineScan = keys.getInt(1);
                }
            }
            try (PreparedStatement baseline = connection.prepareStatement(
                    "INSERT INTO Baseline(ProjectId,ScanId,CreatedAt) VALUES(?,?,datetime('now'))")) {
                baseline.setInt(1, duplicate);
                baseline.setInt(2, baselineScan);
                baseline.executeUpdate();
            }
            connection.commit();
        }

        DatabaseInitializer.initializeDatabase();

        assertEquals(baselineScan, new ScanRepository().getBaselineScanId(keeper));
        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet projects = statement.executeQuery("SELECT COUNT(*) FROM Project")) {
                assertTrue(projects.next());
                assertEquals(1, projects.getInt(1));
            }
            try (ResultSet scans = statement.executeQuery("SELECT COUNT(*) FROM Scan WHERE ProjectId = " + keeper)) {
                assertTrue(scans.next());
                assertEquals(2, scans.getInt(1));
            }
        }
    }

    @Test
    void databaseFailuresAreNotReturnedAsMissingProjectIds(@TempDir Path temporaryDirectory) throws Exception {
        Path directoryAtDatabaseFilePath = Files.createDirectory(temporaryDirectory.resolve("not-a-database"));
        useDatabase(directoryAtDatabaseFilePath);

        assertThrows(IllegalStateException.class,
                () -> new ProjectRepository().saveProject("project", "project-path"));
    }

    @Test
    void foreignKeysRejectScansForProjectsThatDoNotExist(@TempDir Path temporaryDirectory) throws Exception {
        useDatabase(temporaryDirectory.resolve("cleancheck.db"));
        DatabaseInitializer.initializeDatabase();

        assertThrows(IllegalStateException.class,
                () -> new ScanRepository().saveScan(404, 90, 0, 0, 0, 0));
    }

    private void useDatabase(Path databasePath) {
        previousDatabasePath = System.getProperty(DATABASE_PROPERTY);
        System.setProperty(DATABASE_PROPERTY, databasePath.toString());
    }
}
