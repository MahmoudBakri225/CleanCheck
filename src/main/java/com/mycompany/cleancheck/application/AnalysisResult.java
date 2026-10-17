package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.report.RegressionSummary;
import com.mycompany.cleancheck.quality.SourceSize;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One finished analysis. {@code regression} is only set when the run compared the scan with a
 * stored baseline (CleanCheck.execute); plain analyzer runs leave it null.
 */
public record AnalysisResult(
        Path projectRoot,
        List<Path> javaFiles,
        List<Warning> warnings,
        RegressionSummary regression,
        int totalSourceLines
) {
    public AnalysisResult {
        javaFiles = List.copyOf(javaFiles);
        warnings = List.copyOf(warnings);
        if (totalSourceLines < 0) {
            throw new IllegalArgumentException("Total source line count cannot be negative.");
        }
    }

    public AnalysisResult(Path projectRoot, List<Path> javaFiles, List<Warning> warnings) {
        this(projectRoot, javaFiles, warnings, null, SourceSize.totalLines(javaFiles));
    }

    public AnalysisResult(Path projectRoot, List<Path> javaFiles, List<Warning> warnings,
            RegressionSummary regression) {
        this(projectRoot, javaFiles, warnings, regression, SourceSize.totalLines(javaFiles));
    }

    public AnalysisResult(Path projectRoot, List<Path> javaFiles, List<Warning> warnings, int totalSourceLines) {
        this(projectRoot, javaFiles, warnings, null, totalSourceLines);
    }

    public AnalysisResult withRegression(RegressionSummary summary) {
        return new AnalysisResult(projectRoot, javaFiles, warnings, summary, totalSourceLines);
    }

    /**
     * Adopts the baseline comparison of an earlier run, but only when that run saw exactly the same
     * project, findings, and source size. A report must never show regression numbers for code that
     * has changed since they were computed.
     */
    public AnalysisResult withRegressionFrom(AnalysisResult earlier) {
        if (earlier == null || earlier.regression() == null || !sameFindingsAs(earlier)
                || totalSourceLines != earlier.totalSourceLines) {
            return this;
        }
        return withRegression(earlier.regression());
    }

    public boolean sameFindingsAs(AnalysisResult other) {
        if (other == null || warnings.size() != other.warnings.size()) {
            return false;
        }
        Path mine = projectRoot == null ? null : projectRoot.toAbsolutePath().normalize();
        Path theirs = other.projectRoot == null ? null : other.projectRoot.toAbsolutePath().normalize();
        return Objects.equals(mine, theirs) && fingerprints(warnings).equals(fingerprints(other.warnings));
    }

    private record FindingIdentity(String fileName, int lineNumber, String ruleName,
            Severity severity, String message) {
    }

    private static Map<FindingIdentity, Integer> fingerprints(List<Warning> list) {
        Map<FindingIdentity, Integer> counts = new HashMap<>();
        for (Warning w : list) {
            FindingIdentity identity = new FindingIdentity(w.getFileName(), w.getLineNumber(),
                    w.getRuleName(), w.getSeverity(), w.getMessage());
            counts.merge(identity, 1, Integer::sum);
        }
        return counts;
    }
}
