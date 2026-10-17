package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Metrics: how many open findings there are, which files and rules carry the most risk, and where to start.
 * Risk uses the same weights as the quality score (critical 5, warning 3, info 1).
 */
public final class MetricsPage {

    private static final int TOP = 8;

    private final VBox content = new VBox(20);

    public VBox create() {
        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Metrics", "Where the problems are concentrated, so you know where to start."),
                content);
        clear();
        return page;
    }

    public void clear() {
        content.getChildren().setAll(sectionCard("No data yet", null,
                emptyState("\u25A5", "Nothing to measure",
                        "Run an analysis. The metrics appear here as soon as the scan finishes.")));
    }

    /** Rebuilds every card from the current findings. Fixed findings are ignored. */
    public void show(List<IssueRow> issues) {
        int critical = 0;
        int warnings = 0;
        int info = 0;
        for (IssueRow issue : issues) {
            if ("Fixed".equalsIgnoreCase(issue.getStatus())) {
                continue;
            }
            String severity = issue.getSeverity() == null ? "" : issue.getSeverity().toUpperCase(java.util.Locale.ROOT);
            switch (severity) {
                case "CRITICAL" -> critical++;
                case "WARNING" -> warnings++;
                default -> info++;
            }
        }
        int open = critical + warnings + info;
        if (open == 0) {
            content.getChildren().setAll(sectionCard("All clear", null,
                    emptyState("\u2713", "No open findings", "Nothing to improve in the latest scan.")));
            return;
        }

        var stats = cardRow(
                statCard("Open issues", String.valueOf(open), "still to fix", ACCENT),
                statCard("Critical", String.valueOf(critical), "likely bugs, fix first", CRITICAL),
                statCard("Warnings", String.valueOf(warnings), "design and maintenance risk", WARNING),
                statCard("Info", String.valueOf(info), "polish", INFO));

        List<HotspotRow> files = HotspotCalculator.byFile(issues);
        List<HotspotRow> rules = HotspotCalculator.byRule(issues);

        VBox fileCard = sectionCard("Riskiest files",
                "Longer bar = more risk. Start with the top file.", barList(files, true));
        VBox ruleCard = sectionCard("Riskiest rules",
                "Ranked by the worst finding first, then by total risk.", barList(rules, false));
        HBox.setHgrow(fileCard, Priority.ALWAYS);
        HBox.setHgrow(ruleCard, Priority.ALWAYS);
        var lists = cardRow(fileCard, ruleCard);

        content.getChildren().setAll(stats, lists, startHere(rules));
    }

    private static VBox barList(List<HotspotRow> rows, boolean files) {
        VBox box = new VBox(12);
        int max = 1;
        for (HotspotRow row : rows) {
            max = Math.max(max, parse(row.getRiskScore()));
        }
        int shown = 0;
        for (HotspotRow row : rows) {
            if (shown++ >= TOP) {
                break;
            }
            int risk = parse(row.getRiskScore());
            String color = row.getTopWeight() >= 5 ? CRITICAL : (row.getTopWeight() >= 3 ? WARNING : INFO);
            String issues = row.getIssues() + (row.getIssues().equals("1") ? " issue" : " issues");
            box.getChildren().add(barRow(files ? fileLabel(row.getName()) : row.getName(), risk / (double) max, color,
                    issues + "  \u00B7  risk " + risk, 170));
        }
        return box;
    }

    private static VBox startHere(List<HotspotRow> rules) {
        VBox box = new VBox(12);
        int shown = 0;
        for (HotspotRow row : rules) {
            if (shown++ >= 3) {
                break;
            }
            RuleKnowledge.Entry entry = RuleKnowledge.forRule(row.getName());
            Label title = new Label(shown + ".  " + entry.rule() + "  (" + row.getIssues() + ")");
            title.setStyle(strong(14));
            VBox item = new VBox(4, title,
                    wrapped(entry.problem(), "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"),
                    wrapped("First step: " + entry.steps().get(0), muted(12)));
            item.setPadding(new Insets(0, 0, 4, 0));
            box.getChildren().add(item);
        }
        return sectionCard("Where to start", "The three rules with the worst findings, and what to do about them.", box);
    }

    private static int parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
