package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import com.mycompany.cleancheck.fix.FixEngine;
import com.mycompany.cleancheck.security.SecurityCatalog;
import com.mycompany.cleancheck.security.SecurityScore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Security Review: the security findings of the latest scan, each with its CWE and OWASP Top 10 category,
 * how it is exploited and the safe fix. It is a preliminary review (single-file pattern analysis), and the
 * page says so instead of promising security.
 */
public final class SecurityReviewPage {

    private final VBox content = new VBox(20);
    private final VBox detail = new VBox(12);

    public VBox create() {
        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Security Review",
                        "Common security mistakes in your code, each linked to a CWE weakness and the OWASP Top 10."),
                disclaimer(),
                content);
        clear();
        return page;
    }

    public void clear() {
        content.getChildren().setAll(sectionCard("No data yet", null,
                emptyState("\u25C8", "No security review yet",
                        "Run an analysis. The security findings appear here as soon as the scan finishes.")),
                coverageCard());
    }

    /** Rebuilds the page from the current findings. Fixed findings are ignored. */
    public void show(List<IssueRow> issues) {
        List<IssueRow> findings = new ArrayList<>();
        List<String> severities = new ArrayList<>();
        Set<String> rules = new HashSet<>();
        for (IssueRow issue : issues) {
            if ("Fixed".equalsIgnoreCase(issue.getStatus()) || !SecurityCatalog.isSecurity(issue.getRule())) {
                continue;
            }
            findings.add(issue);
            severities.add(issue.getSeverity());
            rules.add(issue.getRule());
        }

        if (findings.isEmpty()) {
            content.getChildren().setAll(sectionCard("No known risky patterns", null,
                    emptyState("\u2713", "Nothing suspicious in the latest scan",
                            "The " + SecurityCatalog.all().size() + " security rules found nothing. This is not a "
                                    + "guarantee: they only look for known patterns inside single files.")),
                    coverageCard());
            return;
        }

        SecurityScore.Result score = SecurityScore.of(severities);
        String gradeColor = score.score() >= 75 ? SUCCESS : score.score() >= 50 ? WARNING : CRITICAL;
        var stats = cardRow(
                statCard("Security grade", score.grade(), score.score() + " / 100  \u00B7  preliminary", gradeColor),
                statCard("Critical", String.valueOf(score.critical()), "fix these first", CRITICAL),
                statCard("Warnings", String.valueOf(score.warning()), "review and decide", WARNING),
                statCard("Weakness types", String.valueOf(rules.size()),
                        "of " + SecurityCatalog.all().size() + " checked", ACCENT));

        TableView<IssueRow> table = new TableView<>(FXCollections.observableArrayList(findings));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(tableStyle());
        double tableHeight = Math.min(380, 56 + 34 * findings.size());
        table.setPrefHeight(tableHeight);
        table.setMinHeight(tableHeight);
        table.setMaxHeight(tableHeight);
        TableColumn<IssueRow, String> severity = column("Severity", "severity", 80);
        TableColumn<IssueRow, String> rule = column("Rule", "rule", 150);
        TableColumn<IssueRow, String> cwe = new TableColumn<>("CWE");
        cwe.setCellValueFactory(c -> new SimpleStringProperty(
                SecurityCatalog.info(c.getValue().getRule()).map(SecurityCatalog.Info::cwe).orElse("")));
        cwe.setPrefWidth(80);
        TableColumn<IssueRow, String> owasp = new TableColumn<>("OWASP Top 10");
        owasp.setCellValueFactory(c -> new SimpleStringProperty(
                SecurityCatalog.info(c.getValue().getRule()).map(SecurityCatalog.Info::owasp).orElse("")));
        owasp.setPrefWidth(220);
        TableColumn<IssueRow, String> file = fileColumn("File", 200);
        TableColumn<IssueRow, String> line = column("Line", "line", 55);
        table.getColumns().add(severity);
        table.getColumns().add(rule);
        table.getColumns().add(cwe);
        table.getColumns().add(owasp);
        table.getColumns().add(file);
        table.getColumns().add(line);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> showDetail(now));

        VBox findingsCard = sectionCard("Findings",
                "Select one to see how it is exploited and how to fix it.", table);
        VBox detailCard = sectionCard("Details", null, detail);

        content.getChildren().setAll(stats, findingsCard, detailCard, coverageCard());
        table.getSelectionModel().select(0);
    }

    // ---------------------------------------------------------------------

    private void showDetail(IssueRow row) {
        detail.getChildren().clear();
        if (row == null) {
            detail.getChildren().add(wrapped("Select a finding in the table.", muted(13)));
            return;
        }
        var found = SecurityCatalog.info(row.getRule());
        if (found.isEmpty()) {
            detail.getChildren().add(wrapped(row.getMessage(), strong(13)));
            return;
        }
        SecurityCatalog.Info info = found.get();

        Label name = new Label(info.rule());
        name.setStyle(strong(16));
        HBox title = new HBox(8, severityBadge(row.getSeverity()), name,
                badge(info.cwe(), PRIMARY, PRIMARY_SOFT), badge(info.owasp(), PRIMARY, PRIMARY_SOFT));
        title.setAlignment(Pos.CENTER_LEFT);

        Label where = new Label(shortPath(row.getFile(), 90) + "  \u00B7  line " + row.getLine());
        where.setStyle(muted(12));

        detail.getChildren().addAll(
                title,
                where,
                wrapped(row.getMessage(), strong(13)),
                block("What the weakness is", info.cwe() + ": " + info.cweTitle() + ". " + info.problem()),
                block("How it can be exploited", info.attack()),
                block("How to fix it", numbered(info.steps())),
                block("Before", code(info.bad())),
                block("After", code(info.good())));
        if (FixEngine.supportsAutomaticFix(row.getRule(), row.getMessage())) {
            detail.getChildren().add(wrapped(
                    "A verified automatic fix exists for this finding. Open it in Issues and press Apply Fix.",
                    "-fx-text-fill: " + SUCCESS + "; -fx-font-size: 13px; -fx-font-weight: bold;"));
        } else {
            detail.getChildren().add(wrapped(
                    "No automatic fix: this change needs a decision from you.", muted(12)));
        }
    }

    private static VBox block(String heading, String text) {
        Label h = new Label(heading.toUpperCase(java.util.Locale.ROOT));
        h.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        return new VBox(4, h, wrapped(text, "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;"));
    }

    private static VBox block(String heading, javafx.scene.Node body) {
        Label h = new Label(heading.toUpperCase(java.util.Locale.ROOT));
        h.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        return new VBox(4, h, body);
    }

    private static String numbered(List<String> steps) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            text.append(i + 1).append(".  ").append(steps.get(i));
            if (i < steps.size() - 1) {
                text.append('\n');
            }
        }
        return text.toString();
    }

    private static Label code(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-font-family: 'Consolas', 'Menlo', 'Monospaced'; -fx-font-size: 12px;"
                + "-fx-text-fill: " + TEXT + "; -fx-background-color: " + PRIMARY_SOFT + ";"
                + "-fx-background-radius: 10; -fx-padding: 10 12 10 12;");
        return label;
    }

    /** Plain statement of what this review is, so nobody reads a good grade as a promise. */
    private static VBox disclaimer() {
        Label text = wrapped(
                "Preliminary security review. CleanCheck analyzes one file at a time, so it can raise false "
                        + "alarms and can miss flaws that cross classes. Treat a finding as a place to check, and a "
                        + "clean result as \"nothing known found\", not as proof the code is secure.",
                "-fx-text-fill: " + TEXT + "; -fx-font-size: 13px;");
        VBox box = new VBox(text);
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle("-fx-background-color: " + TINT_WARNING + ";"
                + "-fx-border-color: " + TINT_WARNING_BORDER + ";"
                + "-fx-background-radius: 12; -fx-border-radius: 12;");
        return box;
    }

    private static VBox coverageCard() {
        FlowPane checks = new FlowPane(10, 10);
        for (SecurityCatalog.Info info : SecurityCatalog.all()) {
            checks.getChildren().add(badge(info.rule() + "  \u00B7  " + info.cwe(), PRIMARY, PRIMARY_SOFT));
        }
        VBox limits = new VBox(6,
                wrapped("\u2022  Looks at one file at a time. Data that travels between classes or files is not followed.", muted(13)),
                wrapped("\u2022  Pattern checks can raise false alarms. Read each finding before you act.", muted(13)),
                wrapped("\u2022  Does not check the libraries you depend on for known vulnerable versions.", muted(13)),
                wrapped("\u2022  Secret values are never printed in findings or reports.", muted(13)));
        VBox box = new VBox(14, checks, limits);
        return sectionCard("What this review checks",
                SecurityCatalog.all().size() + " weakness types, based on the OWASP Top 10 (2021).", box);
    }
}
