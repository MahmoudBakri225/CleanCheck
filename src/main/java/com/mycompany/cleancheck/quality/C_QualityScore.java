package com.mycompany.cleancheck.quality;

import com.mycompany.cleancheck.E_Severity;
import com.mycompany.cleancheck.C_Warning;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the overall quality score of a Java project based on the
 * warnings detected by CleanCheck rules.
 * Also provides helper methods to count issues by severity,
 * generate a rating, and print the most common problems.
 *
 * @author KA
 */
public class C_QualityScore {

    private static final int STARTING_SCORE = 100;
    private static final int CRITICAL_PENALTY = 5;
    private static final int WARNING_PENALTY = 2;
    private static final int INFO_PENALTY = 1;

    /**
     * Calculates the quality score starting from 100 and subtracting
     * penalties according to the severity of each warning.
     * The final score never goes below 0.
     *
     * @param warnings the list of all warnings found in the project
     * @return the calculated quality score (0 – 100)
     */
    public int calculate(List<C_Warning> warnings) {
        int score = STARTING_SCORE;
        for (C_Warning w : warnings) {
            if (w.getSeverity() == E_Severity.CRITICAL) {
                score -= CRITICAL_PENALTY;
            } else if (w.getSeverity() == E_Severity.WARNING) {
                score -= WARNING_PENALTY;
            } else if (w.getSeverity() == E_Severity.INFO) {
                score -= INFO_PENALTY;
            }
        }
        return Math.max(score, 0);
    }

    /**
     * Counts how many critical warnings exist in the given list.
     *
     * @param warnings the list of warnings
     * @return the number of critical issues
     */
    public int countCritical(List<C_Warning> warnings) {
        return (int) warnings.stream()
                .filter(w -> w.getSeverity() == E_Severity.CRITICAL)
                .count();
    }

    /**
     * Counts how many warning-level issues exist in the given list.
     *
     * @param warnings the list of warnings
     * @return the number of warning issues
     */
    public int countWarnings(List<C_Warning> warnings) {
        return (int) warnings.stream()
                .filter(w -> w.getSeverity() == E_Severity.WARNING)
                .count();
    }

    /**
     * Counts how many informational issues exist in the given list.
     *
     * @param warnings the list of warnings
     * @return the number of info issues
     */
    public int countInfo(List<C_Warning> warnings) {
        return (int) warnings.stream()
                .filter(w -> w.getSeverity() == E_Severity.INFO)
                .count();
    }

    /**
     * Converts a numeric score into a human-readable rating.
     *
     * @param score the quality score (0 – 100)
     * @return one of: EXCELLENT, GOOD, NEEDS IMPROVEMENT, or POOR
     */
    public String getRating(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 50) return "NEEDS IMPROVEMENT";
        return "POOR";
    }

    /**
     * Prints a summary of the most common problems found,
     * grouped by rule name and showing how many times each rule was triggered.
     *
     * @param warnings the list of all warnings found in the project
     */
    public void printMostCommonProblems(List<C_Warning> warnings) {
        Map<String, Integer> counts = new HashMap<>();
        for (C_Warning w : warnings) {
            counts.put(w.getRuleName(), counts.getOrDefault(w.getRuleName(), 0) + 1);
        }

        System.out.println("\nMost Common Problems");
        System.out.println("-------------------------");
        for (String rule : counts.keySet()) {
            System.out.println(rule + " : " + counts.get(rule));
        }
    }
}