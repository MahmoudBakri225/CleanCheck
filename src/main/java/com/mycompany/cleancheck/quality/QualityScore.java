package com.mycompany.cleancheck.quality;

import com.mycompany.cleancheck.application.ConsoleOutput;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Quality score from 100 down to 0.
 * Penalties are normalized per 100 lines of source so a small sample and a large
 * codebase can be compared more fairly (same issue density → similar score).
 */
public class QualityScore {

    private static final int STARTING_SCORE = 100;
    private static final int CRITICAL_PENALTY = 5;
    private static final int WARNING_PENALTY = 2;
    private static final int INFO_PENALTY = 1;

    /**
     * Density-aware score. {@code totalSourceLines} should be the sum of lines in
     * analyzed {@code .java} files (comments and blanks still count as size).
     */
    public int calculate(List<Warning> warnings, int totalSourceLines) {
        int rawPenalty = 0;
        for (Warning warning : warnings) {
            rawPenalty += switch (warning.getSeverity()) {
                case CRITICAL -> CRITICAL_PENALTY;
                case WARNING -> WARNING_PENALTY;
                case INFO -> INFO_PENALTY;
            };
        }
        double hundredsOfLines = Math.max(1.0, totalSourceLines / 100.0);
        int score = (int) Math.round(STARTING_SCORE - (rawPenalty / hundredsOfLines));
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Fallback when line count is unknown: treat the project as 100 lines
     * (one unit), which matches the historic absolute penalty scale.
     */
    public int calculate(List<Warning> warnings) {
        return calculate(warnings, 100);
    }

    public int countCritical(List<Warning> warnings) {
        return countBySeverity(warnings, Severity.CRITICAL);
    }

    public int countWarnings(List<Warning> warnings) {
        return countBySeverity(warnings, Severity.WARNING);
    }

    public int countInfo(List<Warning> warnings) {
        return countBySeverity(warnings, Severity.INFO);
    }

    private int countBySeverity(List<Warning> warnings, Severity severity) {
        int count = 0;
        for (Warning warning : warnings) {
            if (warning.getSeverity() == severity) {
                count++;
            }
        }
        return count;
    }

    public String getRating(int score) {
        if (score >= 90) {
            return "Excellent";
        }
        if (score >= 75) {
            return "Good";
        }
        if (score >= 50) {
            return "Fair";
        }
        return "Poor";
    }

    public void printMostCommonProblems(List<Warning> warnings) {
        Map<String, Integer> counts = new TreeMap<>();
        for (Warning warning : warnings) {
            counts.merge(warning.getRuleName(), 1, Integer::sum);
        }

        ConsoleOutput.out().println("Most common problems:");
        if (counts.isEmpty()) {
            ConsoleOutput.out().println("  none");
            return;
        }

        counts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> ConsoleOutput.out().println("  " + e.getKey() + " : " + e.getValue()));
    }
}
