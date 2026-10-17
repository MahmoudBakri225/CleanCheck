package com.mycompany.cleancheck.security;

import java.util.List;
import java.util.Locale;

/** A 0-100 security score and an A-F grade from the open security findings. Preliminary, like the review itself. */
public final class SecurityScore {

    public record Result(int score, String grade, int critical, int warning, int info) {

        public int total() {
            return critical + warning + info;
        }

        /** One honest sentence for the card under the grade. */
        public String verdict() {
            if (total() == 0) {
                return "No known risky patterns found";
            }
            if (critical > 0) {
                return critical + (critical == 1 ? " critical finding to fix first" : " critical findings to fix first");
            }
            return "No critical findings, review the warnings";
        }
    }

    // Weighted risk is squeezed into 0-100 with a curve, so more findings always lower the score
    // but it never drops to a flat 0: 8 critical findings and 20 critical findings still look different.
    private static final int CRITICAL_COST = 10;
    private static final int WARNING_COST = 4;
    private static final int INFO_COST = 1;
    private static final double HALF_SCORE_RISK = 25.0;

    private SecurityScore() {
    }

    /** @param severities severity names (CRITICAL, WARNING, INFO) of the open security findings */
    public static Result of(List<String> severities) {
        int critical = 0;
        int warning = 0;
        int info = 0;
        for (String severity : severities) {
            String value = severity == null ? "" : severity.trim().toUpperCase(Locale.ROOT);
            switch (value) {
                case "CRITICAL" -> critical++;
                case "WARNING" -> warning++;
                default -> info++;
            }
        }
        double risk = critical * CRITICAL_COST + warning * WARNING_COST + info * INFO_COST;
        int score = (int) Math.round(100.0 / (1.0 + risk / HALF_SCORE_RISK));
        return new Result(score, grade(score), critical, warning, info);
    }

    static String grade(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 75) {
            return "B";
        }
        if (score >= 60) {
            return "C";
        }
        if (score >= 40) {
            return "D";
        }
        return "F";
    }
}
