package com.mycompany.cleancheck.report;

/**
 * Result of comparing the current scan with the project's stored baseline.
 * Carried by {@code AnalysisResult} so the HTML report can show the same numbers as the console.
 *
 * @param baselineScanId   id of the baseline scan in the database
 * @param baselineScore    quality score of the baseline scan, or null when it could not be read
 * @param newIssues        findings present now but not in the baseline
 * @param resolvedIssues   findings present in the baseline but gone now
 * @param unchangedIssues  findings present in both
 * @param movedIssues      unchanged findings that changed line number
 * @param changedIssues    unchanged findings that changed severity or message
 * @param newCriticalIssues new findings with CRITICAL severity
 */
public record RegressionSummary(
        int baselineScanId,
        Integer baselineScore,
        int newIssues,
        int resolvedIssues,
        int unchangedIssues,
        int movedIssues,
        int changedIssues,
        int newCriticalIssues) {
}
