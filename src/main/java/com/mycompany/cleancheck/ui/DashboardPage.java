package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The Dashboard: overall quality score (ring + severity distribution), three severity cards with accent
 * borders and a plain-language meaning, and a scan speed / performance card.
 * <p>
 * The page owns its widgets. {@link DashboardController} only calls {@link #create()}, {@link #clear()} and
 * {@link #show}; all of them must be called on the JavaFX Application Thread (the GUI already does so from
 * {@code Platform.runLater}). Colours come from {@link GuiTheme} variables, so light and dark mode both work.
 */
final class DashboardPage {

    private static final double SPEED_BUDGET_SECONDS = 15.0;

    private final ScoreRing ring = new ScoreRing();
    private final VBox distribution = new VBox(10);

    private final Label criticalValue = new Label("0");
    private final Label warningValue = new Label("0");
    private final Label infoValue = new Label("0");
    private final ProgressBar criticalShare = new ProgressBar(0);
    private final ProgressBar warningShare = new ProgressBar(0);
    private final ProgressBar infoShare = new ProgressBar(0);
    private final Label criticalPercent = new Label();
    private final Label warningPercent = new Label();
    private final Label infoPercent = new Label();

    private final Label totalValue = new Label("0");
    private final Label timeValue = new Label("\u2014");
    private final Label speedVerdict = new Label();
    private final ProgressBar speedBar = new ProgressBar(0);
    private final Label filesValue = new Label("\u2014");
    private final Label filesRate = new Label("\u2014");
    private final Label issueRate = new Label("\u2014");

    VBox create() {
        // transparent ring: it sits inside our own card
        ring.setStyle("-fx-background-color: transparent;");
        ring.setPrefWidth(Region.USE_COMPUTED_SIZE);
        ring.setMinHeight(Region.USE_COMPUTED_SIZE);

        GridPane top = grid(16, 28, 24, 24, 24);
        top.add(scoreCard(), 0, 0);
        top.add(severityCard("Critical", "CRITICAL", criticalValue, criticalShare, criticalPercent,
                CRITICAL, TINT_CRITICAL, TINT_CRITICAL_BORDER, "High Impact"), 1, 0);
        top.add(severityCard("Warning", "WARNING", warningValue, warningShare, warningPercent,
                WARNING, TINT_WARNING, TINT_WARNING_BORDER, "Review Suggested"), 2, 0);
        top.add(severityCard("Info", "INFO", infoValue, infoShare, infoPercent,
                INFO, TINT_INFO, TINT_INFO_BORDER, "Improvement"), 3, 0);
        fill(top);

        GridPane bottom = grid(16, 66, 34);
        bottom.add(speedCard(), 0, 0);
        bottom.add(totalCard(), 1, 0);
        fill(bottom);

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Dashboard", "Quality score and severity breakdown for the latest scan."),
                top,
                bottom);
        clear();
        return page;
    }

    // ---------------------------------------------------------------------
    // Cards
    // ---------------------------------------------------------------------

    private VBox scoreCard() {
        Label heading = new Label("OVERALL QUALITY");
        heading.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label sub = new Label("Share of open findings by severity");
        sub.setStyle(muted(12));

        VBox box = new VBox(12, heading, ring, sub, distribution);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(20));
        box.setStyle(GuiTheme.cardStyle());
        return box;
    }

    private VBox severityCard(String title, String severity, Label value, ProgressBar share, Label percent,
            String accent, String tint, String tintBorder, String badgeText) {
        Region strip = new Region();
        strip.setMinHeight(5);
        strip.setPrefHeight(5);
        strip.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 5;");

        Label name = new Label(title.toUpperCase(java.util.Locale.ROOT));
        name.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label badge = badge(badgeText, "white", accent);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, name, spacer, badge);
        head.setAlignment(Pos.CENTER_LEFT);

        value.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 44px; -fx-font-weight: bold;");

        share.setMaxWidth(Double.MAX_VALUE);
        share.setPrefHeight(8);
        share.setStyle("-fx-accent: " + accent + ";");
        percent.setStyle(muted(12));

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox box = new VBox(10, strip, head, value,
                wrapped(RuleKnowledge.severityMeaning(severity), muted(13)), grow, share, percent);
        box.setPadding(new Insets(18));
        box.setMinHeight(300);
        box.setStyle("-fx-background-color: " + tint + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + tintBorder + ";"
                + "-fx-border-radius: 18; -fx-border-width: 1.5;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.07), 24, 0, 0, 8);");
        return box;
    }

    private VBox speedCard() {
        Label icon = circle("\u26A1", "rgba(245,158,11,0.16)", "#D97706", 40);
        Label name = new Label("SCAN SPEED & PERFORMANCE");
        name.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(12, icon, name, spacer, speedVerdict);
        head.setAlignment(Pos.CENTER_LEFT);

        timeValue.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 40px; -fx-font-weight: bold;");
        Label caption = new Label("analysis time");
        caption.setStyle(muted(13));
        HBox big = new HBox(10, timeValue, caption);
        big.setAlignment(Pos.BASELINE_LEFT);

        speedBar.setMaxWidth(Double.MAX_VALUE);
        speedBar.setPrefHeight(10);
        Label barLabel = new Label("Speed rating (scaled by number of files)");
        barLabel.setStyle(muted(12));

        HBox metrics = new HBox(14, metric("Files scanned", filesValue), metric("Throughput", filesRate),
                metric("Findings found", issueRate));
        for (var node : metrics.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        VBox box = new VBox(12, head, big, speedBar, barLabel, metrics);
        box.setPadding(new Insets(20));
        box.setStyle(GuiTheme.cardStyle());
        return box;
    }

    private static VBox metric(String title, Label value) {
        Label t = new Label(title);
        t.setStyle(muted(12));
        value.setStyle(strong(16));
        VBox box = new VBox(2, t, value);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: " + BG + "; -fx-background-radius: 12;");
        return box;
    }

    private VBox totalCard() {
        Label title = new Label("OPEN ISSUES");
        title.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        totalValue.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 40px; -fx-font-weight: bold;");
        VBox box = new VBox(8, title, totalValue,
                wrapped("Findings that are already fixed are not counted here.", muted(13)));
        box.setPadding(new Insets(20));
        box.setStyle(GuiTheme.cardStyle());
        return box;
    }

    // ---------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------

    /** Back to the empty state (no scan yet). */
    void clear() {
        ring.reset();
        distribution.getChildren().clear();
        distribution.getChildren().add(wrapped("Run a scan to see the breakdown.", muted(12)));
        setCounts(0, 0, 0, 0);
        timeValue.setText("\u2014");
        filesValue.setText("\u2014");
        filesRate.setText("\u2014");
        issueRate.setText("\u2014");
        speedBar.setProgress(0);
        styleBadge(speedVerdict, "No scan yet", MUTED);
    }

    /**
     * Shows the result of a scan.
     *
     * @param score   quality score 0..100, or a negative number when it is not known
     * @param total   open findings
     * @param files   number of files scanned, 0 when not known
     */
    void show(int score, int total, int critical, int warning, int info, double seconds, int files) {
        if (score >= 0) {
            ring.setScore(score);
        } else {
            ring.reset();
        }
        setCounts(total, critical, warning, info);

        distribution.getChildren().clear();
        if (total == 0) {
            distribution.getChildren().add(wrapped("No open findings. Nice and clean.", muted(12)));
        } else {
            distribution.getChildren().addAll(
                    barRow("Critical", fraction(critical, total), CRITICAL, String.valueOf(critical), 62),
                    barRow("Warning", fraction(warning, total), WARNING, String.valueOf(warning), 62),
                    barRow("Info", fraction(info, total), INFO, String.valueOf(info), 62));
        }

        timeValue.setText(String.format("%.1fs", seconds));
        double budget = Math.max(SPEED_BUDGET_SECONDS, Math.max(1, files) * 4.0);
        double rating = Math.max(0, Math.min(1, 1 - seconds / budget));
        speedBar.setProgress(rating);
        String color = seconds < budget * 0.35 ? SUCCESS
                : (seconds < budget * 0.75 ? WARNING : CRITICAL);
        speedBar.setStyle("-fx-accent: " + color + ";");
        String verdict = seconds < budget * 0.35 ? "Fast"
                : (seconds < budget * 0.75 ? "Normal" : "Slow");
        styleBadge(speedVerdict, verdict, color);

        double safeSeconds = Math.max(seconds, 0.05);
        filesValue.setText(files > 0 ? String.valueOf(files) : "\u2014");
        filesRate.setText(files > 0 ? String.format("%.2f files/s", files / safeSeconds) : "\u2014");
        issueRate.setText(String.valueOf(total));
    }

    private void setCounts(int total, int critical, int warning, int info) {
        totalValue.setText(String.valueOf(total));
        criticalValue.setText(String.valueOf(critical));
        warningValue.setText(String.valueOf(warning));
        infoValue.setText(String.valueOf(info));
        criticalShare.setProgress(fraction(critical, total));
        warningShare.setProgress(fraction(warning, total));
        infoShare.setProgress(fraction(info, total));
        criticalPercent.setText(percent(critical, total));
        warningPercent.setText(percent(warning, total));
        infoPercent.setText(percent(info, total));
    }

    private static double fraction(int part, int total) {
        return total <= 0 ? 0 : (double) part / total;
    }

    private static String percent(int part, int total) {
        return total <= 0 ? "of open issues" : Math.round(100.0 * part / total) + "% of open issues";
    }

    private static void styleBadge(Label label, String text, String color) {
        label.setText(text);
        label.setStyle("-fx-text-fill: white; -fx-background-color: " + color + ";"
                + "-fx-background-radius: 999; -fx-padding: 3 10 3 10;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;");
    }

    // ---------------------------------------------------------------------
    // Layout helpers
    // ---------------------------------------------------------------------

    private static GridPane grid(double gap, double... percents) {
        GridPane grid = new GridPane();
        grid.setHgap(gap);
        for (double percent : percents) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percent);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private static void fill(GridPane grid) {
        for (var node : grid.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }
            GridPane.setFillWidth(node, true);
            GridPane.setFillHeight(node, true);
        }
    }
}
