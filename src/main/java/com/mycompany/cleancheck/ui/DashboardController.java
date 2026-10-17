package com.mycompany.cleancheck.ui;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * Thin adapter between {@link CleanCheckGUI} and {@link DashboardPage}: it turns the scan output and the
 * issue list into plain numbers. Same public methods as before, so the GUI keeps compiling; the only
 * addition is the {@code filesScanned} overload that feeds the scan-speed card.
 */
final class DashboardController {

    private static final Pattern SCORE =
            Pattern.compile("Initial Score\\s*:\\s*(\\d+)\\s*/\\s*100", Pattern.CASE_INSENSITIVE);

    private final DashboardPage page = new DashboardPage();

    public VBox create() {
        return page.create();
    }

    public void clear(MetricsPage metricsPage) {
        page.clear();
        metricsPage.clear();
    }

    public void update(String output, double seconds, List<IssueRow> allIssues, Node rootPane) {
        update(output, seconds, allIssues, rootPane, 0);
    }

    public void update(String output, double seconds, List<IssueRow> allIssues, Node rootPane, int filesScanned) {
        int total = 0;
        for (IssueRow issue : allIssues) {
            if (!"Fixed".equalsIgnoreCase(issue.getStatus())) {
                total++;
            }
        }
        Matcher m = SCORE.matcher(output == null ? "" : output);
        int score = m.find() ? Integer.parseInt(m.group(1)) : -1;

        page.show(score, total,
                countSeverity(allIssues, "CRITICAL"),
                countSeverity(allIssues, "WARNING"),
                countSeverity(allIssues, "INFO"),
                seconds, filesScanned);
    }

    /** Only Open findings count: Fixed history must not inflate the dashboard. */
    private static int countSeverity(List<IssueRow> allIssues, String severity) {
        int count = 0;
        for (IssueRow issue : allIssues) {
            if (!"Fixed".equalsIgnoreCase(issue.getStatus()) && issue.getSeverity().equalsIgnoreCase(severity)) {
                count++;
            }
        }
        return count;
    }
}
