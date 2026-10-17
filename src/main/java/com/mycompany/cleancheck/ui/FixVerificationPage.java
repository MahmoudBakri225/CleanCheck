package com.mycompany.cleancheck.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Explains the safe-fix product story in plain language for demos and the committee.
 */
final class FixVerificationPage {

    private FixVerificationPage() {
    }

    static VBox create() {
        Label title = new Label("Fix Verification");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label sub = new Label(
                "CleanCheck can change source files — but only after a safety checklist passes. "
                        + "This page is the rulebook of that checklist.");
        sub.setWrapText(true);
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");

        VBox page = new VBox(22,
                new VBox(8, title, sub),
                whatItIs(),
                howItWorks(),
                exampleCard(),
                whichRules(),
                bottomNotes());
        page.setPadding(new Insets(8, 4, 32, 4));
        return page;
    }

    private static VBox whatItIs() {
        Label h = new Label("What this feature is");
        h.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label p = new Label(
                "On the Issues page, some findings show Apply Verified Fix. "
                        + "That button is the only place CleanCheck rewrites a file. "
                        + "A normal Scan never modifies your project.");
        p.setWrapText(true);
        p.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");
        VBox box = new VBox(10, h, p);
        box.setPadding(new Insets(18));
        box.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 18;"
                + "-fx-border-color: #BFDBFE; -fx-border-radius: 18;");
        return box;
    }

    private static VBox howItWorks() {
        Label h = new Label("What happens when you click Apply Verified Fix");
        h.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        VBox steps = new VBox(12,
                step("1", "Backup", "A copy of the file is saved under .cleancheck-backups."),
                step("2", "Apply the change", "Only the reported rule is rewritten (for example == becomes equals)."),
                step("3", "Re-analyze", "The same rules run again on the project."),
                step("4", "Compile check", "If a JDK is available, javac must still succeed."),
                step("5", "Keep or roll back",
                        "If the issue is gone and nothing worse appears, status becomes Fixed. "
                                + "Otherwise the backup is restored. You can also press Rollback Last Fix."));
        VBox box = new VBox(14, h, steps);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 20;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 20;");
        return box;
    }

    private static HBox step(String n, String title, String body) {
        Label num = new Label(n);
        num.setMinSize(36, 36);
        num.setAlignment(Pos.CENTER);
        num.setStyle("-fx-background-color: #0F172A; -fx-text-fill: white; -fx-background-radius: 999;"
                + "-fx-font-weight: bold; -fx-font-size: 14px;");
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label b = new Label(body);
        b.setWrapText(true);
        b.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        VBox text = new VBox(4, t, b);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(14, num, text);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private static VBox exampleCard() {
        Label h = new Label("Live example used in demos");
        h.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label d = new Label("String Comparison on the test project — same transformation Fix Engine applies.");
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        d.setWrapText(true);

        Label beforeTitle = new Label("Before (bug)");
        beforeTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #B91C1C;");
        Label before = new Label("if (role == \"admin\") { ... }");
        before.setStyle("-fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 13px;"
                + "-fx-text-fill: #7F1D1D; -fx-background-color: #FEF2F2; -fx-background-radius: 12;"
                + "-fx-padding: 12;");
        before.setMaxWidth(Double.MAX_VALUE);

        Label afterTitle = new Label("After (verified fix)");
        afterTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #047857;");
        Label after = new Label("if (\"admin\".equals(role)) { ... }");
        after.setStyle("-fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 13px;"
                + "-fx-text-fill: #064E3B; -fx-background-color: #ECFDF5; -fx-background-radius: 12;"
                + "-fx-padding: 12;");
        after.setMaxWidth(Double.MAX_VALUE);

        HBox pair = new HBox(12, new VBox(6, beforeTitle, before), new VBox(6, afterTitle, after));
        HBox.setHgrow(pair.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(pair.getChildren().get(1), Priority.ALWAYS);

        VBox box = new VBox(12, h, d, pair);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 20;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 20;");
        return box;
    }

    private static VBox whichRules() {
        Label h = new Label("Rules allowed to auto-fix");
        h.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label d = new Label(
                "Only mechanical, low-risk edits. Security issues (secrets, injection, weak crypto) stay manual "
                        + "because they need a human decision.");
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");
        FlowPane flow = new FlowPane(8, 8);
        for (String r : new String[]{
                "Data Type", "String Comparison", "Encapsulation", "Unused Variable", "Magic Number",
                "System Out", "Naming Convention", "Missing Package", "Unused Import", "Information Leak"
        }) {
            Label chip = new Label(r);
            chip.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-font-weight: bold;"
                    + "-fx-font-size: 12px; -fx-background-radius: 999; -fx-padding: 7 14;");
            flow.getChildren().add(chip);
        }
        VBox box = new VBox(12, h, d, flow);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 20;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 20;");
        return box;
    }

    private static HBox bottomNotes() {
        VBox a = note("Where you use it", "Issues page → select a finding → Apply Verified Fix.");
        VBox b = note("What you tell the committee",
                "We never auto-edit on scan. Fix is explicit, backed up, re-scanned, and reversible.");
        VBox c = note("Demo tip",
                "Use System Out or String Comparison on CleanCheck-TestProject — fast and visible.");
        HBox row = new HBox(12, a, b, c);
        for (var n : row.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
            if (n instanceof Region r) {
                r.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return row;
    }

    private static VBox note(String title, String text) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label d = new Label(text);
        d.setWrapText(true);
        d.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        VBox box = new VBox(6, t, d);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 16;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 16;");
        return box;
    }
}
