package com.mycompany.cleancheck.database.service;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.database.DatabaseConnection;
import com.mycompany.cleancheck.database.repository.IssueRepository;
import com.mycompany.cleancheck.database.repository.ScanRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Persists a completed scan and all findings as one database transaction. */
public final class ScanPersistenceService {

    private final ScanRepository scans;
    private final IssueRepository issues;

    public ScanPersistenceService() {
        this(new ScanRepository(), new IssueRepository());
    }

    ScanPersistenceService(ScanRepository scans, IssueRepository issues) {
        this.scans = scans;
        this.issues = issues;
    }

    public int save(int projectId, int qualityScore, int totalIssues, int criticalCount,
            int warningCount, int infoCount, List<Warning> findings) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int scanId = scans.saveScan(connection, projectId, qualityScore, totalIssues,
                        criticalCount, warningCount, infoCount);
                issues.saveIssues(connection, scanId, findings);
                connection.commit();
                return scanId;
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist scan and findings atomically.", e);
        }
    }

    private static void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
