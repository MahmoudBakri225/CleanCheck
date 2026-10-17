package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import com.mycompany.cleancheck.database.repository.ScanSummary;
import com.mycompany.cleancheck.database.repository.ScanSummaryRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Compares the latest stored scan with the one before it and shows whether quality moved up or down,
 * plus the score trend of the last scans.
 */
public final class RegressionPage {

    private static final int TREND_SCANS = 8;

    private final Supplier<Path> projectPath;
    private final ScanSummaryRepository repository = new ScanSummaryRepository();
    private final VBox content = new VBox(20);

    public RegressionPage(Supplier<Path> projectPath) {
        this.projectPath = projectPath;
    }

    public VBox create() {
        Button refreshButton = button("\u21BB  Refresh", PRIMARY);
        refreshButton.setOnAction(e -> refresh());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(12, spacer, refreshButton);

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Regression", "Did your last changes make the code better or worse?"),
                top, content);
        refresh();
        return page;
    }

    public void refresh() {
        Path path = projectPath.get();
        if (path == null) {
            show(List.of(), "Select a project first.");
            return;
        }
        try {
            int projectId = repository.findProjectId(path);
            List<ScanSummary> scans = projectId < 0 ? List.of() : repository.findLatest(projectId, TREND_SCANS);
            show(scans, null);
        } catch (Exception ex) {
            show(List.of(), "Unable to load regression data: " + ex.getMessage());
        }
    }

    /** @param scans newest first */
    private void show(List<ScanSummary> scans, String problem) {
        ScanSummary newest = scans.isEmpty() ? null : scans.get(0);
        ScanSummary previous = scans.size() < 2 ? null : scans.get(1);
        RegressionInsight insight = RegressionInsight.compare(newest, previous);

        content.getChildren().clear();
        content.getChildren().add(banner(insight, problem));
        if (newest == null) {
            return;
        }
        if (previous != null) {
            content.getChildren().add(cardRow(
                    statCard("Previous score", previous.score() + " / 100",
                            "Scan #" + previous.scanId() + "  \u00B7  " + previous.date(), MUTED),
                    statCard("Latest score", newest.score() + " / 100",
                            "Scan #" + newest.scanId() + "  \u00B7  " + newest.date(), ACCENT),
                    statCard("Score change", RegressionInsight.signed(insight.scoreDelta()),
                            "points", deltaColor(insight.scoreDelta(), true)),
                    statCard("Issue change", RegressionInsight.signed(insight.issueDelta()),
                            "fewer is better", deltaColor(insight.issueDelta(), false))));
        } else {
            content.getChildren().add(cardRow(
                    statCard("Score", newest.score() + " / 100", "Scan #" + newest.scanId(), ACCENT),
                    statCard("Open issues", String.valueOf(newest.totalIssues()), "in the first scan", WARNING)));
        }
        content.getChildren().add(trend(scans));
        content.getChildren().add(sectionCard("How to read this", null, wrapped(
                "This page compares the latest scan with the scan right before it. "
                        + "The exported HTML report compares with the stored baseline instead, so its numbers can differ.",
                muted(13))));
    }

    private static VBox banner(RegressionInsight insight, String problem) {
        String bg;
        String border;
        String color;
        String icon;
        switch (insight.verdict()) {
            case IMPROVED -> {
                bg = "rgba(34,197,94,0.12)"; border = "rgba(34,197,94,0.45)"; color = "#16A34A"; icon = "\u2714";
            }
            case REGRESSED -> {
                bg = TINT_CRITICAL; border = TINT_CRITICAL_BORDER; color = CRITICAL; icon = "\u26A0";
            }
            case STABLE -> {
                bg = TINT_INFO; border = TINT_INFO_BORDER; color = INFO; icon = "=";
            }
            default -> {
                bg = TINT_INFO; border = TINT_INFO_BORDER; color = INFO; icon = "i";
            }
        }
        Label mark = circle(icon, color, "white", 40);
        Label headline = new Label(problem != null ? problem : insight.headline());
        headline.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        VBox text = new VBox(4, headline, wrapped(problem != null ? "" : insight.detail(),
                "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"));
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(16, mark, text);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(row);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 18;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 18;");
        return box;
    }

    private static VBox trend(List<ScanSummary> newestFirst) {
        List<ScanSummary> oldestFirst = new ArrayList<>(newestFirst);
        Collections.reverse(oldestFirst);
        VBox rows = new VBox(10);
        for (ScanSummary scan : oldestFirst) {
            String color = scan.score() >= 85 ? "#16A34A" : (scan.score() >= 70 ? WARNING : CRITICAL);
            rows.getChildren().add(barRow("Scan #" + scan.scanId(), scan.score() / 100.0, color,
                    scan.score() + "  \u00B7  " + scan.totalIssues() + " issues", 90));
        }
        return sectionCard("Score trend", "Oldest scan at the top. Green is 85 or more, orange 70 or more.", rows);
    }

    private static String deltaColor(int delta, boolean higherIsBetter) {
        if (delta == 0) {
            return MUTED;
        }
        boolean good = higherIsBetter ? delta > 0 : delta < 0;
        return good ? "#16A34A" : CRITICAL;
    }
}
