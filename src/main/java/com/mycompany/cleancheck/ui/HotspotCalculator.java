package com.mycompany.cleancheck.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Builds the "riskiest files / rules" tables from the open findings. No JavaFX involved. */
public final class HotspotCalculator {

    private HotspotCalculator() {
    }

    public static List<HotspotRow> byFile(List<IssueRow> issues) {
        return group(issues, IssueRow::getFile);
    }

    public static List<HotspotRow> byRule(List<IssueRow> issues) {
        return group(issues, IssueRow::getRule);
    }

    private static List<HotspotRow> group(List<IssueRow> issues, Function<IssueRow, String> key) {
        Map<String, int[]> totals = new HashMap<>();
        for (IssueRow issue : issues) {
            if ("Fixed".equalsIgnoreCase(issue.getStatus())) {
                continue;
            }
            int[] t = totals.computeIfAbsent(key.apply(issue), k -> new int[3]);
            int weight = IssueAdvisor.riskWeight(issue.getSeverity());
            t[0]++;
            t[1] += weight;
            t[2] = Math.max(t[2], weight);
        }
        List<HotspotRow> rows = new ArrayList<>();
        totals.forEach((name, t) -> rows.add(new HotspotRow(name, t[0], t[1], t[2])));
        // The worst severity decides first: fifty Info findings must never outrank one SQL injection.
        rows.sort(Comparator.comparingInt(HotspotRow::getTopWeight).reversed()
                .thenComparing(Comparator.comparingInt((HotspotRow r) -> Integer.parseInt(r.getRiskScore())).reversed())
                .thenComparing(HotspotRow::getName));
        return rows;
    }
}
