package com.mycompany.cleancheck.ui;

import com.mycompany.cleancheck.database.repository.ScanSummary;

/** Turns two stored scans into a verdict and a plain-language explanation. UI-free. */
public record RegressionInsight(
        Verdict verdict,
        String headline,
        String detail,
        int scoreDelta,
        int issueDelta,
        int criticalDelta) {

    public enum Verdict { NO_DATA, FIRST_SCAN, IMPROVED, STABLE, REGRESSED }

    /** @param newest the latest scan, or null; @param previous the scan before it, or null */
    public static RegressionInsight compare(ScanSummary newest, ScanSummary previous) {
        if (newest == null) {
            return new RegressionInsight(Verdict.NO_DATA, "No scans yet",
                    "Run an analysis on this project. Each scan is stored so CleanCheck can compare it with the next one.",
                    0, 0, 0);
        }
        if (previous == null) {
            return new RegressionInsight(Verdict.FIRST_SCAN, "First scan saved",
                    "This scan is the starting point. After you fix something and analyze again, "
                            + "CleanCheck shows here whether quality moved up or down.",
                    0, 0, 0);
        }
        int scoreDelta = newest.score() - previous.score();
        int issueDelta = newest.totalIssues() - previous.totalIssues();
        int criticalDelta = newest.criticalCount() - previous.criticalCount();

        Verdict verdict;
        String headline;
        if (scoreDelta < 0 || criticalDelta > 0) {
            verdict = Verdict.REGRESSED;
            headline = scoreDelta < 0
                    ? "Quality dropped by " + (-scoreDelta) + (scoreDelta == -1 ? " point" : " points")
                    : "New critical issues appeared";
        } else if (scoreDelta > 0) {
            verdict = Verdict.IMPROVED;
            headline = "Quality improved by " + scoreDelta + (scoreDelta == 1 ? " point" : " points");
        } else {
            verdict = Verdict.STABLE;
            headline = "No change since the previous scan";
        }

        StringBuilder detail = new StringBuilder();
        detail.append(describe(issueDelta, "issue")).append(". ");
        detail.append("Critical issues: ").append(previous.criticalCount())
                .append(" \u2192 ").append(newest.criticalCount()).append(". ");
        detail.append(switch (verdict) {
            case REGRESSED -> "Review what changed since the previous scan before submitting.";
            case IMPROVED -> "Good progress, keep going.";
            default -> "Fix an issue from the Issues page and analyze again to see movement.";
        });
        return new RegressionInsight(verdict, headline, detail.toString(), scoreDelta, issueDelta, criticalDelta);
    }

    private static String describe(int delta, String noun) {
        if (delta == 0) {
            return "The number of " + noun + "s did not change";
        }
        int abs = Math.abs(delta);
        String word = abs == 1 ? noun : noun + "s";
        return delta < 0 ? abs + " fewer " + word : abs + " more " + word;
    }

    public static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }
}
