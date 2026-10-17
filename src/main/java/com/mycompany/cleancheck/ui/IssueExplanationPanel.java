package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Explains the selected finding in plain language: what is wrong, why it matters, how to fix it and a
 * before/after example. The text comes from {@link RuleKnowledge}; the rule's own message and suggestion
 * are shown first so the explanation is about this exact finding.
 */
final class IssueExplanationPanel {

    private static final String MONO = "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 12px;";

    private final VBox root = new VBox(14);

    IssueExplanationPanel() {
        clear();
    }

    VBox node() {
        return root;
    }

    void clear() {
        root.getChildren().setAll(
                wrapped("Select a finding in the table. CleanCheck will explain what is wrong, "
                        + "why it matters and how to fix it.", muted(13)));
    }

    void show(String rule, String severity, String message, String suggestion, boolean autoFix, boolean alreadyFixed) {
        RuleKnowledge.Entry entry = RuleKnowledge.forRule(rule);
        root.getChildren().clear();

        Label ruleName = new Label(entry.rule());
        ruleName.setStyle(strong(16));
        HBox title = new HBox(8, severityBadge(severity), ruleName, badge(entry.category(), PRIMARY, PRIMARY_SOFT));
        title.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(title);

        // 1. What is wrong
        VBox wrong = new VBox(6);
        if (message != null && !message.isBlank()) {
            wrong.getChildren().add(wrapped(message, strong(13)));
        }
        wrong.getChildren().add(wrapped(entry.problem(), "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"));
        root.getChildren().add(block("What is wrong", wrong));

        // 2. Why it matters
        VBox why = new VBox(6, wrapped(entry.why(), "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"));
        String meaning = RuleKnowledge.severityMeaning(severity);
        if (!meaning.isEmpty()) {
            why.getChildren().add(wrapped(meaning, muted(12)));
        }
        root.getChildren().add(block("Why it matters", why));

        // 3. How to fix it
        VBox how = new VBox(6);
        if (suggestion != null && !suggestion.isBlank()) {
            how.getChildren().add(wrapped("CleanCheck suggests: " + suggestion,
                    "-fx-text-fill: " + PRIMARY + "; -fx-font-size: 13px; -fx-font-weight: bold;"));
        }
        int n = 1;
        for (String step : entry.steps()) {
            how.getChildren().add(wrapped(n + ".  " + step, "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"));
            n++;
        }
        root.getChildren().add(block("How to fix it", how));

        // 4. Example
        if (!entry.badExample().isEmpty()) {
            VBox example = new VBox(8,
                    code("Before", entry.badExample(), TINT_CRITICAL, TINT_CRITICAL_BORDER, CRITICAL),
                    code("After", entry.goodExample(), "rgba(34,197,94,0.12)", "rgba(34,197,94,0.45)", "#16A34A"));
            root.getChildren().add(block("Example", example));
        }

        // 5. Who fixes it
        String who;
        String whoBg;
        String whoFg;
        if (alreadyFixed) {
            who = "Already fixed and verified";
            whoBg = "#DCFCE7";
            whoFg = "#16A34A";
        } else if (autoFix) {
            who = "Safe automatic fix available (backup + verification)";
            whoBg = "#DCFCE7";
            whoFg = "#16A34A";
        } else {
            who = "Needs your decision: CleanCheck will not change this automatically";
            whoBg = "#FEF3C7";
            whoFg = "#B45309";
        }
        root.getChildren().add(wrapped(who,
                "-fx-text-fill: " + whoFg + "; -fx-background-color: " + whoBg + ";"
                        + "-fx-background-radius: 10; -fx-padding: 8 12 8 12;"
                        + "-fx-font-size: 12px; -fx-font-weight: bold;"));
    }

    private static VBox block(String heading, VBox content) {
        Label h = new Label(heading.toUpperCase(java.util.Locale.ROOT));
        h.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox box = new VBox(6, h, content);
        return box;
    }

    private static VBox code(String caption, String text, String bg, String border, String captionColor) {
        Label c = new Label(caption);
        c.setStyle("-fx-text-fill: " + captionColor + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label body = wrapped(text, "-fx-text-fill: " + TEXT + ";" + MONO);
        VBox box = new VBox(4, c, body);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 10;");
        return box;
    }
}
