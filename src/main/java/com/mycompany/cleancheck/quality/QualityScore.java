package com.mycompany.cleancheck.quality;

import com.mycompany.cleancheck.Severity;
import com.mycompany.cleancheck.Warning;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QualityScore {

    private static final int STARTING_SCORE = 100;
    private static final int CRITICAL_PENALTY = 5;
    private static final int WARNING_PENALTY = 2;
    private static final int INFO_PENALTY = 1;

    public int calculate(List<Warning> warnings) {
        int score = STARTING_SCORE;
        for (Warning w : warnings) {
            if (w.getSeverity() == Severity.CRITICAL) score -= CRITICAL_PENALTY;
            else if (w.getSeverity() == Severity.WARNING) score -= WARNING_PENALTY;
            else if (w.getSeverity() == Severity.INFO) score -= INFO_PENALTY;
        }
        return Math.max(score, 0);
    }

    public int countCritical(List<Warning> warnings) {
        return (int) warnings.stream().filter(w -> w.getSeverity() == Severity.CRITICAL).count();
    }

    public int countWarnings(List<Warning> warnings) {
        return (int) warnings.stream().filter(w -> w.getSeverity() == Severity.WARNING).count();
    }

    public int countInfo(List<Warning> warnings) {
        return (int) warnings.stream().filter(w -> w.getSeverity() == Severity.INFO).count();
    }

    public String getRating(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 50) return "NEEDS IMPROVEMENT";
        return "POOR";
    }

    public void printMostCommonProblems(List<Warning> warnings) {
        Map<String, Integer> counts = new HashMap<>();
        for (Warning w : warnings) {
            counts.put(w.getRuleName(), counts.getOrDefault(w.getRuleName(), 0) + 1);
        }

        System.out.println("\nMost Common Problems");
        System.out.println("-------------------------");
        for (String rule : counts.keySet()) {
            System.out.println(rule + " : " + counts.get(rule));
        }
    }
}


