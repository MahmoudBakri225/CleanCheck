package com.mycompany.cleancheck.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Catalogue of every rule with search, category chips and expandable cards.
 */
final class RulesPage {

    private RulesPage() {
    }

    static VBox create() {
        Label title = new Label("Rules");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label sub = new Label("Every quality and security check, with what it detects and whether it can auto-fix.");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
        sub.setWrapText(true);

        List<RuleKnowledge.Entry> all = RuleKnowledge.all();
        long total = RuleKnowledge.ruleCount();
        long auto = all.stream().filter(RuleKnowledge.Entry::autoFix).count();

        HBox stats = new HBox(12,
                statCard(String.valueOf(total), "Total rules", "#2563EB"),
                statCard(String.valueOf(auto), "Auto-fixable", "#059669"),
                statCard(String.valueOf(total - auto), "Manual only", "#D97706"),
                statCard(String.valueOf(RuleKnowledge.categories().size()), "Categories", "#7C3AED"));
        stats.setPadding(new Insets(4, 0, 8, 0));

        TextField search = new TextField();
        search.setPromptText("Search rules by name, category or keyword…");
        search.setStyle("-fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #E2E8F0;"
                + "-fx-padding: 10 14; -fx-font-size: 13px;");
        search.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(search, Priority.ALWAYS);

        FlowPane chips = new FlowPane(8, 8);
        chips.setPadding(new Insets(4, 0, 0, 0));
        String[] categories = {
            "All", RuleKnowledge.CORRECTNESS, RuleKnowledge.ERROR_HANDLING, RuleKnowledge.DESIGN,
            RuleKnowledge.MAINTAINABILITY, RuleKnowledge.READABILITY, RuleKnowledge.STYLE,
            RuleKnowledge.SECURITY
        };
        Label[] chipLabels = new Label[categories.length];
        final String[] selected = {"All"};

        VBox list = new VBox(12);
        Runnable refresh = () -> {
            list.getChildren().clear();
            String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String cat = selected[0];
            Map<String, VBox> byCat = new LinkedHashMap<>();
            for (RuleKnowledge.Entry e : all) {
                if (!"All".equals(cat) && !cat.equals(e.category())) {
                    continue;
                }
                if (!q.isEmpty()) {
                    String hay = (e.rule() + " " + e.category() + " " + e.problem()).toLowerCase();
                    if (!hay.contains(q)) {
                        continue;
                    }
                }
                byCat.computeIfAbsent(e.category(), c -> {
                    Label h = new Label(c);
                    h.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
                    VBox section = new VBox(10, h);
                    list.getChildren().add(section);
                    return section;
                }).getChildren().add(ruleCard(e));
            }
            if (list.getChildren().isEmpty()) {
                Label empty = new Label("No rules match this filter.");
                empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
                list.getChildren().add(empty);
            }
        };

        for (int i = 0; i < categories.length; i++) {
            String c = categories[i];
            Label chip = new Label(c);
            chip.setStyle(chipStyle(c.equals("All")));
            chip.setOnMouseClicked(ev -> {
                selected[0] = c;
                for (int j = 0; j < chipLabels.length; j++) {
                    chipLabels[j].setStyle(chipStyle(categories[j].equals(c)));
                }
                refresh.run();
            });
            chipLabels[i] = chip;
            chips.getChildren().add(chip);
        }
        search.textProperty().addListener((o, a, b) -> refresh.run());
        refresh.run();

        VBox page = new VBox(18, title, sub, stats, search, chips, list);
        page.setPadding(new Insets(8, 8, 24, 8));
        return page;
    }

    private static String chipStyle(boolean on) {
        if (on) {
            return "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 999;"
                    + "-fx-padding: 6 14; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;";
        }
        return "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 999;"
                + "-fx-padding: 6 14; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;";
    }

    private static VBox statCard(String value, String label, String color) {
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        VBox box = new VBox(2, v, l);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 16;");
        return box;
    }

    private static VBox ruleCard(RuleKnowledge.Entry e) {
        Label name = new Label(e.rule());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label cat = new Label(e.category());
        cat.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563EB;"
                + "-fx-background-color: #EFF6FF; -fx-background-radius: 999; -fx-padding: 3 10;");
        Label fix = new Label(e.autoFix() ? "Auto-fix" : "Manual");
        fix.setStyle(e.autoFix()
                ? "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #059669;"
                  + "-fx-background-color: #ECFDF5; -fx-background-radius: 999; -fx-padding: 3 10;"
                : "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #D97706;"
                  + "-fx-background-color: #FFFBEB; -fx-background-radius: 999; -fx-padding: 3 10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(10, name, spacer, cat, fix);
        head.setAlignment(Pos.CENTER_LEFT);

        Label problem = new Label(e.problem());
        problem.setWrapText(true);
        problem.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155;");

        Label whyTitle = new Label("Why it matters");
        whyTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        Label why = new Label(e.why());
        why.setWrapText(true);
        why.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        VBox stepsBox = new VBox(4);
        Label stepsTitle = new Label("How to fix");
        stepsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        stepsBox.getChildren().add(stepsTitle);
        int i = 1;
        for (String s : e.steps()) {
            Label step = new Label(i++ + ". " + s);
            step.setWrapText(true);
            step.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
            stepsBox.getChildren().add(step);
        }

        Label bad = new Label("Before\n" + e.badExample());
        bad.setWrapText(true);
        bad.setStyle("-fx-font-family: 'Consolas','Courier New',monospace; -fx-font-size: 11px;"
                + "-fx-text-fill: #991B1B; -fx-background-color: #FEF2F2; -fx-background-radius: 10;"
                + "-fx-padding: 10;");
        Label good = new Label("After\n" + e.goodExample());
        good.setWrapText(true);
        good.setStyle("-fx-font-family: 'Consolas','Courier New',monospace; -fx-font-size: 11px;"
                + "-fx-text-fill: #065F46; -fx-background-color: #ECFDF5; -fx-background-radius: 10;"
                + "-fx-padding: 10;");
        HBox examples = new HBox(10, bad, good);
        HBox.setHgrow(bad, Priority.ALWAYS);
        HBox.setHgrow(good, Priority.ALWAYS);
        bad.setMaxWidth(Double.MAX_VALUE);
        good.setMaxWidth(Double.MAX_VALUE);

        VBox detail = new VBox(10, whyTitle, why, stepsBox, examples);
        detail.setVisible(false);
        detail.setManaged(false);

        Label toggle = new Label("Show details ▾");
        toggle.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        toggle.setOnMouseClicked(ev -> {
            boolean show = !detail.isVisible();
            detail.setVisible(show);
            detail.setManaged(show);
            toggle.setText(show ? "Hide details ▴" : "Show details ▾");
        });

        VBox card = new VBox(10, head, problem, toggle, detail);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 16;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 16, 0, 0, 4);");
        return card;
    }
}
