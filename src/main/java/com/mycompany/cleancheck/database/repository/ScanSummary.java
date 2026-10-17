package com.mycompany.cleancheck.database.repository;

/**
 * Lightweight read-only view of one stored scan.
 *
 * @author KA
 */
public record ScanSummary(
        int scanId,
        String date,
        int score,
        int totalIssues,
        int criticalCount,
        int warningCount,
        int infoCount) {

}
