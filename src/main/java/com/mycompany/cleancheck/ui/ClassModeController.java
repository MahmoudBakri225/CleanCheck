package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.classmode.ClassHtmlReportExporter;
import com.mycompany.cleancheck.classmode.ClassMode;
import com.mycompany.cleancheck.classmode.ClassReport;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/** Class Mode page: rank student projects, show repeated mistakes, inspect findings, export the class report. */
final class ClassModeController {

    private static final String FOLDER = "\uD83D\uDCC1  ";
    private static final int MAX_DETAIL_ISSUES = 60;
    private static final int MAX_COMMON_RULES = 6;

    private final Supplier<Stage> stageSupplier;
    private final BiConsumer<String, String> status;

    private final VBox summaryBox = new VBox();
    private final VBox rankingBox = new VBox(8);
    private final VBox commonBox = new VBox(14);
    private final VBox detailBox = new VBox(12);
    private Label classFolderLabel;
    private Path classFolderPath;
    private List<ClassReport> reports = List.of();
    private ClassReport selected;

    ClassModeController(Supplier<Stage> stageSupplier, BiConsumer<String, String> status) {
        this.stageSupplier = stageSupplier;
        this.status = status;
    }

    public VBox createPage() {
        classFolderLabel = new Label("No class folder selected.");

        Button choose = button("Choose Class Folder", PRIMARY);
        choose.setOnAction(e -> chooseClassFolder());
        Button analyze = button("Analyze Class", SUCCESS);
        analyze.setOnAction(e -> analyzeClassFolder());
        Button export = button("Export HTML Report", "#7C3AED");
        export.setOnAction(e -> exportClassReport());

        render();
        return ClassModePage.create(classFolderLabel, choose, analyze, export,
                summaryBox, rankingBox, commonBox, detailBox);
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        if (reports.isEmpty()) {
            summaryBox.getChildren().clear();
            rankingBox.getChildren().setAll(emptyState("\u2637", "No class analyzed yet",
                    "Choose the class folder, then press Analyze Class."));
            commonBox.getChildren().setAll(wrapped("Repeated mistakes appear here after the analysis.", muted(13)));
            detailBox.getChildren().setAll(wrapped("Select a student to see the findings.", muted(13)));
            return;
        }
        ClassInsights insights = ClassInsights.of(reports);
        summaryBox.getChildren().setAll(cardRow(
                statCard("Students", String.valueOf(insights.students()), "projects analyzed", ACCENT),
                statCard("Class average", insights.average() + " / 100",
                        "best: " + insights.best().student() + " (" + insights.best().score() + ")", "#16A34A"),
                statCard("Need attention", String.valueOf(insights.needAttention()),
                        "score under " + ClassInsights.ATTENTION_SCORE + " or any critical issue",
                        insights.needAttention() == 0 ? "#16A34A" : WARNING),
                statCard("Critical issues", String.valueOf(insights.totalCritical()),
                        insights.totalIssues() + " findings in total",
                        insights.totalCritical() == 0 ? "#16A34A" : CRITICAL)));

        rankingBox.getChildren().clear();
        int rank = 1;
        for (ClassReport report : reports) {
            rankingBox.getChildren().add(rankRow(rank++, report));
        }

        commonBox.getChildren().clear();
        List<Map.Entry<String, Integer>> common = insights.commonRules();
        if (common.isEmpty()) {
            commonBox.getChildren().add(wrapped("No findings in the whole class. Excellent.", muted(13)));
        } else {
            int max = common.get(0).getValue();
            int shown = 0;
            for (Map.Entry<String, Integer> entry : common) {
                if (shown++ >= MAX_COMMON_RULES) {
                    break;
                }
                commonBox.getChildren().add(barRow(entry.getKey(), entry.getValue() / (double) max, WARNING,
                        entry.getValue() + (entry.getValue() == 1 ? " time" : " times"), 150));
            }
            RuleKnowledge.Entry top = RuleKnowledge.forRule(common.get(0).getKey());
            commonBox.getChildren().add(wrapped("Teach next: " + top.rule() + ". " + top.problem(),
                    "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"
                            + "-fx-background-color: " + PRIMARY_SOFT + "; -fx-background-radius: 10; -fx-padding: 10;"));
        }
        renderDetail();
    }

    private HBox rankRow(int rank, ClassReport report) {
        String medal = rank == 1 ? "#F59E0B" : rank == 2 ? "#94A3B8" : rank == 3 ? "#B45309" : null;
        Label place = medal != null
                ? circle(String.valueOf(rank), medal, "white", 32)
                : circle(String.valueOf(rank), PRIMARY_SOFT, PRIMARY, 32);

        Label name = new Label(report.student());
        name.setMinWidth(150);
        name.setPrefWidth(150);
        name.setStyle(strong(14));

        ProgressBar bar = new ProgressBar(Math.max(0, Math.min(100, report.score())) / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(10);
        String scoreColor = report.score() >= 85 ? "#16A34A" : (report.score() >= 70 ? WARNING : CRITICAL);
        bar.setStyle("-fx-accent: " + scoreColor + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label score = new Label(report.score() + " / 100");
        score.setMinWidth(70);
        score.setStyle("-fx-text-fill: " + scoreColor + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox counts = new HBox(6,
                badge("C " + report.critical(), "white", report.critical() > 0 ? CRITICAL : MUTED),
                badge("W " + report.warnings(), "white", report.warnings() > 0 ? WARNING : MUTED),
                badge("I " + report.info(), "white", report.info() > 0 ? INFO : MUTED));
        counts.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(12, place, name, bar, score, counts);
        if (ClassInsights.needsAttention(report)) {
            Label flag = badge("needs attention", "#B45309", "#FEF3C7");
            row.getChildren().add(flag);
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        boolean isSelected = report == selected;
        row.setStyle("-fx-background-radius: 12; -fx-cursor: hand;"
                + "-fx-background-color: " + (isSelected ? PRIMARY_SOFT : "transparent") + ";");
        row.setOnMouseClicked(e -> {
            selected = report;
            render();
        });
        return row;
    }

    private void renderDetail() {
        detailBox.getChildren().clear();
        if (selected == null) {
            detailBox.getChildren().add(wrapped("Select a student to see the findings.", muted(13)));
            return;
        }
        Label title = new Label(selected.student() + "  \u00B7  score " + selected.score()
                + " / 100  \u00B7  " + selected.totalIssues()
                + (selected.totalIssues() == 1 ? " issue" : " issues"));
        title.setStyle(strong(16));
        detailBox.getChildren().add(title);
        if (selected.issues().isEmpty()) {
            detailBox.getChildren().add(wrapped("No findings. This project is clean.", muted(13)));
            return;
        }
        int shown = 0;
        for (Warning w : selected.issues()) {
            if (shown++ >= MAX_DETAIL_ISSUES) {
                detailBox.getChildren().add(wrapped("\u2026 and " + (selected.issues().size() - MAX_DETAIL_ISSUES)
                        + " more findings.", muted(12)));
                break;
            }
            detailBox.getChildren().add(issueRow(w));
        }
    }

    private VBox issueRow(Warning w) {
        Label rule = new Label(w.getRuleName());
        rule.setStyle(strong(13));
        Label where = new Label(w.getFileName() + ":" + w.getLineNumber());
        where.setStyle(muted(12));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(8, severityBadge(w.getSeverity().name()), rule, spacer, where);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(4, head,
                wrapped(w.getMessage(), "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"),
                wrapped("Fix: " + w.getSuggestion(), muted(12)));
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + BORDER + ";");
        return box;
    }

    // ------------------------------------------------------------------ actions

    private void chooseClassFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Class Folder");
        Stage stage = stageSupplier.get();
        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            classFolderPath = chosen.toPath();
            classFolderLabel.setText(FOLDER + chosen.getAbsolutePath());
        }
    }

    private void analyzeClassFolder() {
        if (classFolderPath == null) {
            status.accept("Select a class folder first.", WARNING);
            return;
        }
        try {
            reports = ClassInsights.ranked(ClassMode.analyze(classFolderPath));
            selected = reports.isEmpty() ? null : reports.get(0);
            render();
            status.accept("Class analysis completed \u2014 " + reports.size() + " projects.", SUCCESS);
        } catch (Exception ex) {
            status.accept("Class analysis failed: " + ex.getMessage(), WARNING);
        }
    }

    private void exportClassReport() {
        if (classFolderPath == null || reports.isEmpty()) {
            status.accept("Analyze the class first.", WARNING);
            return;
        }
        try {
            String dateFolder = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            Path reportsDir = classFolderPath.resolve("reports").resolve(dateFolder);
            Files.createDirectories(reportsDir);
            Path output = reportsDir.resolve("CleanCheck_Class_Report.html");
            ClassHtmlReportExporter.export(output, reports, ClassMode.commonRules(reports));
            status.accept("Class HTML report exported: " + output, SUCCESS);
        } catch (Exception ex) {
            status.accept("Class report export failed: " + ex.getMessage(), WARNING);
        }
    }
}
