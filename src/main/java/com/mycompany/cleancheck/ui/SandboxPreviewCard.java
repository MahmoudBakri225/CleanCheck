package com.mycompany.cleancheck.ui;

import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * "Time-Travel Safe Fixes": a live, self-playing demo of what CleanCheck does with every automatic fix.
 * <ol>
 *   <li>a local backup is created,</li>
 *   <li>the fix is applied (shown here as a before/after diff),</li>
 *   <li>the project is re-analysed (the finding must go down, nothing new may appear),</li>
 *   <li>the project is compiled (no new compile error),</li>
 * </ol>
 * and the user can jump <b>Before</b> / <b>After</b> at any moment or roll back. The steps mirror
 * {@code VerifiedFix} exactly; this card only <i>illustrates</i> them with a fixed example, so it never
 * touches a file and is correct before any project is chosen.
 * <p>
 * Everything runs on the JavaFX Application Thread (a {@link Timeline}), so there is nothing to marshal.
 */
final class SandboxPreviewCard extends VBox {

    private static final List<String> BEFORE = List.of(
            "public boolean isAdmin(String role) {",
            "    if (role == \"admin\") {",
            "        return true;",
            "    }",
            "    return false;",
            "}");
    private static final List<String> AFTER = List.of(
            "public boolean isAdmin(String role) {",
            "    if (\"admin\".equals(role)) {",
            "        return true;",
            "    }",
            "    return false;",
            "}");
    private static final int CHANGED_LINE = 1; // zero based
    private static final String[] STEP_NAMES = {"Backup", "Sandbox fix", "Re-analysis", "Compile check"};

    private final VBox codeBox = new VBox(2);
    private final Label[] steps = new Label[STEP_NAMES.length];
    private final Label verdict = new Label();
    private final ToggleButton beforeButton = new ToggleButton("\u23EA  Before");
    private final ToggleButton afterButton = new ToggleButton("After  \u23E9");
    private Timeline timeline;

    SandboxPreviewCard() {
        super(14);
        setPadding(new Insets(18));
        setMinWidth(380);
        setStyle("-fx-background-color: rgba(8,15,40,0.66);"
                + "-fx-background-radius: 22;"
                + "-fx-border-color: rgba(255,255,255,0.20); -fx-border-radius: 22;"
                + "-fx-effect: dropshadow(gaussian, rgba(2,6,23,0.55), 34, 0, 0, 14);");

        getChildren().addAll(titleBar(), ruleLine(), controls(), codeFrame(), stepRow(), verdict);

        verdict.setWrapText(true);
        verdict.setMinHeight(Region.USE_PREF_SIZE);
        verdict.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup group = new ToggleGroup();
        beforeButton.setToggleGroup(group);
        afterButton.setToggleGroup(group);
        group.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null) {            // never allow "nothing selected"
                old.setSelected(true);
                return;
            }
            render(now == afterButton);
            styleSegments();
        });
        beforeButton.setSelected(true);
        render(false);
        styleSegments();
        resetSteps();

        // Play whenever the page appears, stop when it is removed (no background work while hidden).
        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                stop();
            } else {
                play();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Building blocks
    // ---------------------------------------------------------------------

    private HBox titleBar() {
        HBox dots = new HBox(6, dot("#F87171"), dot("#FBBF24"), dot("#4ADE80"));
        dots.setAlignment(Pos.CENTER_LEFT);
        Label file = new Label("Employee.java  \u2014  sandbox");
        file.setStyle("-fx-text-fill: rgba(255,255,255,0.78); -fx-font-size: 12px; -fx-font-family: 'Consolas', 'Monospaced';");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label safe = new Label("SAFE MODE");
        safe.setStyle("-fx-text-fill: #4ADE80; -fx-background-color: rgba(34,197,94,0.18); -fx-background-radius: 999;"
                + "-fx-padding: 3 10 3 10; -fx-font-size: 10px; -fx-font-weight: bold;");
        HBox bar = new HBox(12, dots, file, spacer, safe);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private static Circle dot(String color) {
        return new Circle(5, Color.web(color));
    }

    private static Label ruleLine() {
        Label l = new Label("StringComparison  \u00B7  line 2  \u00B7  strings compared with ==");
        l.setStyle("-fx-text-fill: #FDE68A; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private HBox controls() {
        HBox segment = new HBox(0, beforeButton, afterButton);
        segment.setAlignment(Pos.CENTER_LEFT);

        Button replay = new Button("\u25B6  Replay");
        replay.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white; -fx-font-size: 12px;"
                + "-fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        replay.setOnAction(e -> play());

        Button rollback = new Button("\u21A9  Rollback");
        rollback.setStyle("-fx-background-color: transparent; -fx-text-fill: #FCA5A5; -fx-font-size: 12px;"
                + "-fx-font-weight: bold; -fx-border-color: rgba(252,165,165,0.55); -fx-border-radius: 999;"
                + "-fx-background-radius: 999; -fx-padding: 5 14 5 14; -fx-cursor: hand;");
        rollback.setOnAction(e -> rollback());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, segment, spacer, replay, rollback);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox codeFrame() {
        codeBox.setPadding(new Insets(12));
        codeBox.setStyle("-fx-background-color: #0B1220; -fx-background-radius: 14;"
                + "-fx-border-color: rgba(255,255,255,0.10); -fx-border-radius: 14;");
        return new VBox(codeBox);
    }

    private FlowPane stepRow() {
        FlowPane flow = new FlowPane(8, 6);
        for (int i = 0; i < steps.length; i++) {
            steps[i] = new Label();
            flow.getChildren().add(steps[i]);
        }
        return flow;
    }

    // ---------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------

    private void render(boolean after) {
        codeBox.getChildren().clear();
        List<String> lines = after ? AFTER : BEFORE;
        JavaSyntax.State state = new JavaSyntax.State();
        for (int i = 0; i < lines.size(); i++) {
            boolean changed = i == CHANGED_LINE;

            Label marker = new Label(changed ? (after ? "+" : "\u2212") : " ");
            marker.setMinWidth(12);
            marker.setStyle("-fx-text-fill: " + (after ? "#4ADE80" : "#F87171") + "; -fx-font-weight: bold;"
                    + "-fx-font-family: 'Consolas', 'Monospaced';");
            Label number = new Label(String.valueOf(i + 1));
            number.setMinWidth(16);
            number.setAlignment(Pos.CENTER_RIGHT);
            number.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-family: 'Consolas', 'Monospaced';");

            TextFlow flow = new TextFlow();
            for (JavaSyntax.Token token : JavaSyntax.tokenize(lines.get(i), state)) {
                Text text = new Text(token.text());
                text.setFont(Font.font("Consolas", 12.5));
                text.setFill(colour(token.kind()));
                flow.getChildren().add(text);
            }

            HBox row = new HBox(8, marker, number, flow);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 6, 2, 4));
            if (changed) {
                row.setStyle("-fx-background-radius: 6; -fx-background-color: "
                        + (after ? "rgba(34,197,94,0.20)" : "rgba(239,68,68,0.22)") + ";");
            }
            codeBox.getChildren().add(row);
        }
    }

    private static Color colour(JavaSyntax.Kind kind) {
        return switch (kind) {
            case KEYWORD -> Color.web("#C084FC");
            case STRING -> Color.web("#4ADE80");
            case COMMENT -> Color.web("#94A3B8");
            case NUMBER -> Color.web("#FBBF24");
            case ANNOTATION -> Color.web("#F9A8D4");
            default -> Color.web("#E2E8F0");
        };
    }

    private void styleSegments() {
        String base = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 14 6 14;";
        String on = "-fx-background-color: white; -fx-text-fill: #1D4ED8;";
        String off = "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: rgba(255,255,255,0.80);";
        beforeButton.setStyle(base + (beforeButton.isSelected() ? on : off)
                + "-fx-background-radius: 999 0 0 999;");
        afterButton.setStyle(base + (afterButton.isSelected() ? on : off)
                + "-fx-background-radius: 0 999 999 0;");
    }

    private void setStep(int index, boolean done) {
        steps[index].setText((done ? "\u2714  " : "\u25CB  ") + STEP_NAMES[index]);
        steps[index].setStyle(done
                ? "-fx-text-fill: #4ADE80; -fx-background-color: rgba(34,197,94,0.18); -fx-background-radius: 999;"
                        + "-fx-padding: 4 12 4 12; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: rgba(255,255,255,0.58); -fx-background-color: rgba(255,255,255,0.08);"
                        + "-fx-background-radius: 999; -fx-padding: 4 12 4 12; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    private void resetSteps() {
        for (int i = 0; i < steps.length; i++) {
            setStep(i, false);
        }
    }

    private void say(String text, String color) {
        verdict.setText(text);
        verdict.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    // ---------------------------------------------------------------------
    // Behaviour
    // ---------------------------------------------------------------------

    /** Runs the whole safe-fix story once: backup, fix, re-check, compile, verdict. */
    void play() {
        stop();
        resetSteps();
        beforeButton.setSelected(true);
        say("Working in a sandbox \u2014 your real files are not touched.", "rgba(255,255,255,0.72)");
        timeline = new Timeline(
                new KeyFrame(Duration.millis(700), e -> {
                    setStep(0, true);
                    say("Backup saved locally in .cleancheck-backups", "rgba(255,255,255,0.72)");
                }),
                new KeyFrame(Duration.millis(1500), e -> {
                    setStep(1, true);
                    afterButton.setSelected(true);
                    say("Fix applied in the sandbox \u2014 compare Before / After any time.", "rgba(255,255,255,0.72)");
                }),
                new KeyFrame(Duration.millis(2300), e -> {
                    setStep(2, true);
                    say("Re-analysis: finding 1 \u2192 0, no new findings.", "rgba(255,255,255,0.72)");
                }),
                new KeyFrame(Duration.millis(3100), e -> {
                    setStep(3, true);
                    say("\u2714 Safe to keep  \u00B7  compile OK  \u00B7  rollback ready", "#4ADE80");
                }));
        timeline.play();
    }

    private void rollback() {
        stop();
        beforeButton.setSelected(true);
        resetSteps();
        say("\u21A9 Rolled back \u2014 the original file was restored from the backup.", "#FCA5A5");
    }

    private void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }
}
