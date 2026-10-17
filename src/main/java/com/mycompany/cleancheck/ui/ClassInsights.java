package com.mycompany.cleancheck.ui;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.classmode.ClassReport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Class-level numbers for a mentor: average, who needs help and which problems repeat. UI-free. */
public record ClassInsights(
        int students,
        int average,
        ClassReport best,
        ClassReport weakest,
        int needAttention,
        int totalIssues,
        int totalCritical,
        List<Map.Entry<String, Integer>> commonRules) {

    /** A score below this, or any critical issue, means the student needs attention. */
    public static final int ATTENTION_SCORE = 75;

    public static boolean needsAttention(ClassReport report) {
        return report.score() < ATTENTION_SCORE || report.critical() > 0;
    }

    /** Best first; ties broken by fewer critical issues, then by name. */
    public static List<ClassReport> ranked(List<ClassReport> reports) {
        List<ClassReport> sorted = new ArrayList<>(reports);
        sorted.sort(Comparator.comparingInt(ClassReport::score).reversed()
                .thenComparingInt(ClassReport::critical)
                .thenComparing(ClassReport::student));
        return sorted;
    }

    public static ClassInsights of(List<ClassReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return new ClassInsights(0, 0, null, null, 0, 0, 0, List.of());
        }
        List<ClassReport> ranked = ranked(reports);
        long scoreSum = 0;
        int attention = 0;
        int issues = 0;
        int critical = 0;
        Map<String, Integer> counts = new HashMap<>();
        for (ClassReport report : ranked) {
            scoreSum += report.score();
            issues += report.totalIssues();
            critical += report.critical();
            if (needsAttention(report)) {
                attention++;
            }
            for (Warning warning : report.issues()) {
                counts.merge(warning.getRuleName(), 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> common = new ArrayList<>(counts.entrySet());
        common.sort(Map.Entry.<String, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey()));
        return new ClassInsights(
                ranked.size(),
                (int) Math.round(scoreSum / (double) ranked.size()),
                ranked.get(0),
                ranked.get(ranked.size() - 1),
                attention,
                issues,
                critical,
                List.copyOf(common));
    }
}
