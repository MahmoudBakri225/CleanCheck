package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import java.nio.file.Path;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Analysis entry screen: shows the selected project, explains the three steps and the safety model. */
public final class AnalyzePage {

    private final Runnable onAnalyze;
    private final Runnable onChoose;
    private final Supplier<Path> project;
    private final Label projectName = new Label();
    private final Label projectPath = new Label();
    private final Button start = button("Start Analysis", SUCCESS);

    public AnalyzePage(Runnable onAnalyze, Runnable onChoose, Supplier<Path> project) {
        this.onAnalyze = onAnalyze;
        this.onChoose = onChoose;
        this.project = project;
    }

    public VBox create() {
        projectName.setStyle(strong(18));
        projectPath.setStyle(muted(12));
        VBox who = new VBox(4, sectionLabel("Selected project"), projectName, projectPath);

        Button change = ghostButton("Choose project");
        change.setOnAction(e -> onChoose.run());
        start.setOnAction(e -> onAnalyze.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hero = new HBox(14, who, spacer, change, start);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(22));
        hero.setStyle(GuiTheme.cardStyle());

        var steps = cardRow(
                step("1", "Scan", "Reads every .java file in the project. No file is modified."),
                step("2", "Understand", "Open Issues: each finding is explained with the reason, the fix and an example."),
                step("3", "Fix and track", "Apply safe fixes, then compare with the previous scan in Regression.")
        );

        FlowPane checks = new FlowPane(10, 10);
        for (String category : RuleKnowledge.categories()) {
            int count = (int) RuleKnowledge.countInCategory(category);
            checks.getChildren().add(badge(category + "  \u00B7  " + count + " rules", PRIMARY, PRIMARY_SOFT));
        }
        VBox checksCard = sectionCard("What CleanCheck checks",
                RuleKnowledge.ruleCount() + " rules across these areas. See the Rules page for the full list.", checks);

        VBox safety = new VBox(8,
                safetyLine("Analyze is read-only: it never changes your source files."),
                safetyLine("Automatic fixes exist only for: " + String.join(", ", RuleKnowledge.autoFixRules()) + "."),
                safetyLine("Every fix makes a local backup first and is verified by analyzing again."),
                safetyLine("Anything that needs a design decision stays manual."));
        VBox safetyCard = sectionCard("Safety model", null, safety);

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Analyze", "Scan a project, then learn from what CleanCheck finds."),
                hero, steps, checksCard, safetyCard);
        refresh();
        return page;
    }

    /** Call whenever the page is shown: the selected project may have changed. */
    public void refresh() {
        Path path = project.get();
        if (path == null) {
            projectName.setText("No project selected");
            projectPath.setText("Choose a project folder to enable the scan.");
            start.setDisable(true);
        } else {
            Path fileName = path.getFileName();
            projectName.setText(fileName == null ? path.toString() : fileName.toString());
            projectPath.setText(shortPath(path.toAbsolutePath().toString(), 90));
            start.setDisable(false);
        }
    }

    private static VBox step(String number, String title, String text) {
        Label n = circle(number, PRIMARY_SOFT, PRIMARY, 34);
        Label t = new Label(title);
        t.setStyle(strong(15));
        HBox head = new HBox(10, n, t);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(10, head, wrapped(text, muted(13)));
        box.setPadding(new Insets(18));
        box.setStyle(GuiTheme.cardStyle());
        return box;
    }

    private static HBox safetyLine(String text) {
        Label tick = new Label("\u2713");
        tick.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 14px;");
        HBox row = new HBox(10, tick, wrapped(text, "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"));
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }
}
