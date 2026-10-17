package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import com.mycompany.cleancheck.database.repository.ScanSummary;
import com.mycompany.cleancheck.database.repository.ScanSummaryRepository;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * The face of CleanCheck, redesigned as a SaaS landing page:
 * <ol>
 *   <li>a hero with the headline, two call-to-action buttons and the live "Time-Travel Safe Fix" sandbox,</li>
 *   <li>a bento grid of facts taken from the real rule set (rules, local security, report formats,
 *       the safety net around every fix, auto-fix coverage),</li>
 *   <li>the feature cards that open the other pages,</li>
 *   <li>the projects that were really scanned (latest score), which can be reopened or forgotten.</li>
 * </ol>
 * The constructor, {@link #create()} and {@link #refresh()} keep the signatures the GUI already uses, so
 * {@code CleanCheckGUI} needs no change. A project folder can be dropped anywhere on the hero card.
 * All of it runs on the JavaFX Application Thread.
 */
public final class HomePage {

    private static final double MAX_WIDTH = 1200;
    private static final String HERO_GRADIENT =
            "-fx-background-color: linear-gradient(to bottom right, #0A1024 0%, #0F2A66 42%, #0E7490 78%, #059669 100%);"
                    + "-fx-background-radius: 30;"
                    + "-fx-border-color: rgba(255,255,255,0.16); -fx-border-radius: 30;"
                    + "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.30), 40, 0, 0, 16);";

    private final Runnable onSelectProject;
    private final Supplier<List<Path>> recent;
    private final Consumer<Path> onOpen;
    private final Consumer<String> onNavigate;
    private final Consumer<Path> onForget;
    private final VBox recentCard = new VBox(8);

    public HomePage(Runnable onSelectProject, Supplier<List<Path>> recent, Consumer<Path> onOpen,
            Consumer<String> onNavigate, Consumer<Path> onForget) {
        this.onSelectProject = onSelectProject;
        this.recent = recent;
        this.onOpen = onOpen;
        this.onNavigate = onNavigate;
        this.onForget = onForget;
    }

    public VBox create() {
        VBox page = new VBox(28, hero(), homeLower(), recentSection(), footer());
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(28, 32, 36, 32));
        page.setStyle("-fx-background-color: " + BG + ";");
        for (Node node : page.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(MAX_WIDTH);
            }
        }
        return page;
    }

    // ---------------------------------------------------------------------
    // Hero
    // ---------------------------------------------------------------------

    private StackPane hero() {
        Label pill = new Label("JAVA CODE QUALITY COACH  \u00B7  100% OFFLINE");
        pill.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;"
                + "-fx-background-color: rgba(255,255,255,0.16); -fx-background-radius: 999; -fx-padding: 6 14 6 14;");
        HBox brand = new HBox(14, pill);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label line1 = new Label("Code quality,");
        Label line2 = new Label("without the risk.");
        String headline = "-fx-text-fill: white; -fx-font-size: 44px; -fx-font-weight: bold;";
        line1.setStyle(headline);
        line2.setStyle("-fx-text-fill: linear-gradient(to right, #67E8F9, #6EE7B7);"
                + "-fx-font-size: 44px; -fx-font-weight: bold;");
        VBox title = new VBox(0, line1, line2);
        Label subtitle = wrapped(
                "Find issues across your entire project, understand why they matter, and fix them with a "
                        + "built-in safety net: automatic backup, re-analysis, compile check, and instant rollback.",
                "-fx-text-fill: #CBD5E1; -fx-font-size: 16px;");

        Button select = new Button("Analyze a project");
        select.setStyle("-fx-background-color: white; -fx-text-fill: #0E7490; -fx-font-weight: bold;"
                + "-fx-font-size: 14px; -fx-background-radius: 14; -fx-padding: 14 28 14 28; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 16, 0, 0, 6);");
        select.setOnAction(e -> onSelectProject.run());
        lift(select);

        HBox actions = new HBox(12, select);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label dropHint = new Label("or drop a project folder anywhere on this card");
        dropHint.setStyle("-fx-text-fill: rgba(255,255,255,0.72); -fx-font-size: 13px;");

        HBox proof = proofStrip();

        VBox left = new VBox(20, brand, title, subtitle, actions, dropHint, proof);
        left.setAlignment(Pos.CENTER_LEFT);

        StackPane emblem = new StackPane(new CleanCheckEmblem());
        var content = split(50, 36, left, emblem);
        content.setPadding(new Insets(44, 44, 40, 44));
        content.setAlignment(Pos.CENTER);

        Circle glowA = decoration(190, 0.07);
        Circle glowB = decoration(130, 0.06);
        StackPane hero = new StackPane(glowA, glowB, content);
        hero.setStyle(HERO_GRADIENT);
        glowA.centerXProperty().bind(hero.widthProperty().subtract(70));
        glowA.setCenterY(30);
        glowB.setCenterX(40);
        glowB.centerYProperty().bind(hero.heightProperty().subtract(10));

        Rectangle clip = new Rectangle();
        clip.setArcWidth(60);
        clip.setArcHeight(60);
        clip.widthProperty().bind(hero.widthProperty());
        clip.heightProperty().bind(hero.heightProperty());
        hero.setClip(clip);

        hero.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        hero.setOnDragDropped(e -> {
            Dragboard board = e.getDragboard();
            boolean accepted = false;
            if (board.hasFiles()) {
                for (File file : board.getFiles()) {
                    if (file.isDirectory()) {
                        onOpen.accept(file.toPath());
                        accepted = true;
                        break;
                    }
                }
            }
            e.setDropCompleted(accepted);
            e.consume();
        });
        return hero;
    }

    private static Circle decoration(double radius, double opacity) {
        Circle circle = new Circle(radius, Color.rgb(255, 255, 255, opacity));
        circle.setManaged(false);
        circle.setMouseTransparent(true);
        return circle;
    }

    /** Four glass tiles with the real numbers of the tool, directly under the call-to-action buttons. */
    private static HBox proofStrip() {
        HBox row = new HBox(10,
                proofTile(String.valueOf(RuleKnowledge.ruleCount()), "rules"),
                proofTile(String.valueOf(RuleKnowledge.autoFixRules().size()), "verified auto-fixes"),
                proofTile("4", "report exports"),
                proofTile("0", "bytes to cloud"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static VBox proofTile(String value, String label) {
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: rgba(255,255,255,0.74); -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox tile = new VBox(1, v, l);
        tile.setPadding(new Insets(10, 14, 10, 14));
        tile.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-background-radius: 14;"
                + "-fx-border-color: rgba(255,255,255,0.20); -fx-border-radius: 14;");
        return tile;
    }

    /** Small "lift" on hover for the call-to-action buttons. */
    private static void lift(Button button) {
        ScaleTransition up = new ScaleTransition(Duration.millis(120), button);
        up.setToX(1.04);
        up.setToY(1.04);
        ScaleTransition down = new ScaleTransition(Duration.millis(120), button);
        down.setToX(1.0);
        down.setToY(1.0);
        button.setOnMouseEntered(e -> {
            down.stop();
            up.playFromStart();
        });
        button.setOnMouseExited(e -> {
            up.stop();
            down.playFromStart();
        });
    }

    // ---------------------------------------------------------------------
    // Bento grid (real numbers from the rule set)
    // ---------------------------------------------------------------------

    private GridPane bentoGrid() {
        int rules = (int) RuleKnowledge.ruleCount();
        int fixes = RuleKnowledge.autoFixRules().size();

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }

        cell(grid, bento("\u2630", "#2563EB", "rgba(37,99,235,0.14)", "AST RULES", String.valueOf(rules), "rules",
                "Stable, AST-based checks, grouped by what they protect. Each one explains the principle behind it.",
                categoryBreakdown()), 0, 0, 2);
        cell(grid, bento("\u26BF", "#0284C7", "rgba(14,165,233,0.16)", "LOCAL SECURITY", "100%", "offline",
                "Everything runs on this machine. Your code never leaves it.",
                chips("#0284C7", "rgba(14,165,233,0.16)", "Local SQLite", "No upload")), 2, 0, 1);
        cell(grid, bento("\u25A4", "#7C3AED", "rgba(124,58,237,0.14)", "REPORT FORMATS", "4", "formats",
                "HTML, JSON, CSV and SARIF from the Report action. Offline: nothing is uploaded to a server.",
                chips("#7C3AED", "rgba(124,58,237,0.14)", "HTML", "JSON", "SARIF", "CSV")), 3, 0, 1);
        cell(grid, safetyNet(), 0, 1, 3);
        cell(grid, bento("\u2714", "#16A34A", "rgba(22,163,74,0.14)", "AUTO-FIXES", String.valueOf(fixes),
                "of " + rules + " rules",
                "Only verified fixes are kept; the rest roll back.",
                progress("#16A34A", rules == 0 ? 0 : (double) fixes / rules)), 3, 1, 1);
        return grid;
    }

    private static void cell(GridPane grid, Region node, int column, int row, int span) {
        node.setMaxWidth(Double.MAX_VALUE);
        node.setMaxHeight(Double.MAX_VALUE);
        grid.add(node, column, row, span, 1);
        GridPane.setFillWidth(node, true);
        GridPane.setFillHeight(node, true);
    }

    /** All categories as one stacked bar plus a legend; the counts add up to the total number of rules. */
    private static VBox categoryBreakdown() {
        String[] colors = {"#2563EB", "#06B6D4", "#10B981", "#7C3AED", "#F59E0B", "#EC4899"};
        List<String> categories = RuleKnowledge.categories();
        int total = (int) Math.max(1, RuleKnowledge.ruleCount());

        GridPane bar = new GridPane();
        bar.setHgap(3);
        FlowPane legend = new FlowPane(14, 8);
        for (int i = 0; i < categories.size(); i++) {
            String color = colors[i % colors.length];
            int count = (int) RuleKnowledge.countInCategory(categories.get(i));

            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 * count / total);
            bar.getColumnConstraints().add(column);
            Region segment = new Region();
            segment.setMinHeight(14);
            segment.setPrefHeight(14);
            segment.setMaxWidth(Double.MAX_VALUE);
            segment.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 7;");
            bar.add(segment, i, 0);
            GridPane.setFillWidth(segment, true);

            Label name = new Label(categories.get(i) + "  " + count);
            name.setStyle(strong(12));
            HBox item = new HBox(6, new Circle(5, Color.web(color)), name);
            item.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(item);
        }
        return new VBox(12, bar, legend);
    }

    /** The unique selling point in one card: what happens around every automatic fix. */
    private VBox safetyNet() {
        Label icon = circle("\u21BA", "rgba(16,185,129,0.16)", "#059669", 42);
        Label name = new Label("ERROR PROTECTION  \u00B7  TIME-TRAVEL SAFE FIXES");
        name.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox top = new HBox(12, icon, name);
        top.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("Every fix has an undo button");
        heading.setStyle(strong(24));

        GridPane steps = cardRow(
                miniStep("1", "Backup", "A copy of the file is saved locally first."),
                miniStep("2", "Re-analysis", "The finding must go down and nothing new may appear."),
                miniStep("3", "Compile check", "The project must still compile with no new error."),
                miniStep("4", "Auto-rollback", "If any check fails, the original is restored."));

        Label live = new Label("Watch a fix happen, live  \u2192");
        live.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        live.setCursor(Cursor.HAND);
        live.setOnMouseClicked(e -> onNavigate.accept("Fix Verification"));

        VBox box = new VBox(14, top, heading, steps, live);
        box.setPadding(new Insets(20));
        box.setMinHeight(Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-background-radius: 20;"
                + "-fx-border-color: rgba(16,185,129,0.45); -fx-border-radius: 20;"
                + "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.16), 26, 0, 0, 10);");
        return box;
    }

    private static VBox miniStep(String number, String title, String text) {
        Label n = circle(number, PRIMARY_SOFT, PRIMARY, 28);
        Label t = new Label(title);
        t.setStyle(strong(14));
        HBox head = new HBox(8, n, t);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, head, wrapped(text, muted(12)));
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: " + BG + "; -fx-background-radius: 14;");
        return box;
    }

    /** Statistic card: icon, label, big number, one-line description and a visual (bars or chips) at the bottom. */
    private static VBox bento(String glyph, String accent, String accentSoft, String label, String value,
            String unit, String text, Node extra) {
        Label icon = circle(glyph, accentSoft, accent, 42);
        Label name = new Label(label);
        name.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox top = new HBox(12, icon, name);
        top.setAlignment(Pos.CENTER_LEFT);

        Label number = new Label(value);
        number.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 44px; -fx-font-weight: bold;");
        Label suffix = new Label(unit);
        suffix.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox big = new HBox(8, number, suffix);
        big.setAlignment(Pos.BASELINE_LEFT);

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox box = new VBox(10, top, big, wrapped(text, muted(13)), grow, extra);
        box.setPadding(new Insets(20));
        box.setMinHeight(Region.USE_PREF_SIZE);
        box.setStyle("-fx-background-color: " + CARD + ";"
                + "-fx-background-radius: 20;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 20;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.10), 26, 0, 0, 10);");
        return box;
    }

    private static HBox chips(String color, String background, String... labels) {
        HBox flow = new HBox(6);
        for (String text : labels) {
            Label chip = new Label(text);
            chip.setStyle("-fx-text-fill: " + color + "; -fx-background-color: " + background + ";"
                    + "-fx-background-radius: 999; -fx-padding: 4 10 4 10;"
                    + "-fx-font-size: 11px; -fx-font-weight: bold;");
            flow.getChildren().add(chip);
        }
        return flow;
    }

    /** Thin horizontal bar filled to the given fraction (emerald gradient). */
    private static StackPane progress(String color, double fraction) {
        Region track = new Region();
        track.setStyle("-fx-background-color: " + BORDER + "; -fx-background-radius: 8;");
        Region fill = new Region();
        fill.setMinWidth(0);
        fill.setStyle("-fx-background-color: linear-gradient(to right, " + color + ", #4ADE80);"
                + "-fx-background-radius: 8;");
        StackPane bar = new StackPane(track, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.setMinHeight(10);
        bar.setPrefHeight(10);
        bar.setMaxHeight(10);
        fill.maxWidthProperty().bind(bar.widthProperty().multiply(Math.max(0.04, Math.min(1.0, fraction))));
        return bar;
    }

    // ---------------------------------------------------------------------
    // Features (each card opens its page)
    // ---------------------------------------------------------------------


    /** Everything below the hero: pillars, workflow, and quick facts. */
    private VBox homeLower() {
        Label heading = new Label("Why teams use CleanCheck");
        heading.setStyle(strong(22));
        Label sub = wrapped(
                "One offline tool for quality, security hints, safe fixes and progress — built for students and small teams.",
                muted(13));

        HBox pillars = new HBox(14,
                pillar("#2563EB", "Issues", "Explain findings",
                        "Open Issues to read why a problem matters and how to fix it.", "Issues"),
                pillar("#059669", "Fix Verification", "Safe automatic fixes",
                        "See the backup → fix → verify → rollback contract before you change code.", "Fix Verification"),
                pillar("#7C3AED", "Security Review", "OWASP / CWE hints",
                        "Open Security Review for offline security findings linked to CWE ids.", "Security Review"),
                pillar("#D97706", "Regression", "Track progress",
                        "Open Regression after a second scan to see what got better or worse.", "Regression"));
        for (var n : pillars.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
            if (n instanceof Region r) {
                r.setMaxWidth(Double.MAX_VALUE);
            }
        }

        Label pathTitle = new Label("Committee demo path");
        pathTitle.setStyle(strong(16));
        HBox path = new HBox(10,
                pathStep("1", "Home / Scan"),
                pathStep("2", "Dashboard"),
                pathStep("3", "Issues + Fix"),
                pathStep("4", "Security"),
                pathStep("5", "Metrics"));
        path.setAlignment(Pos.CENTER_LEFT);
        VBox pathCard = new VBox(12, pathTitle, path,
                wrapped("Five minutes. Same project folder. No internet required.", muted(12)));
        pathCard.setPadding(new Insets(18, 20, 18, 20));
        pathCard.setStyle("-fx-background-color: linear-gradient(to right, #EFF6FF, #F5F3FF);"
                + "-fx-background-radius: 18; -fx-border-color: #C7D2FE; -fx-border-radius: 18;");

        return new VBox(20, new VBox(6, heading, sub), pillars, pathCard);
    }

    private VBox pillar(String color, String pageName, String action, String text, String page) {
        Label bar = new Label(" ");
        bar.setMinHeight(4);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        Label pageLbl = new Label(pageName);
        pageLbl.setStyle(strong(16));
        Label actionLbl = new Label(action);
        actionLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label d = wrapped(text, muted(12));
        Label go = new Label("Go to " + pageName + "  →");
        go.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox box = new VBox(8, bar, pageLbl, actionLbl, d, grow, go);
        box.setPadding(new Insets(16));
        box.setMinHeight(188);
        box.setStyle(GuiTheme.cardStyle());
        box.setOnMouseClicked(e -> onNavigate.accept(page));
        box.setOnMouseEntered(e -> box.setStyle(GuiTheme.cardStyle()
                + "-fx-border-color: " + color + ";"));
        box.setOnMouseExited(e -> box.setStyle(GuiTheme.cardStyle()));
        return box;
    }

    private static Label pathStep(String n, String text) {
        Label l = new Label(n + "  " + text);
        l.setStyle("-fx-background-color: white; -fx-text-fill: #1E3A8A; -fx-font-weight: bold;"
                + "-fx-font-size: 12px; -fx-background-radius: 999; -fx-padding: 8 14;"
                + "-fx-border-color: #93C5FD; -fx-border-radius: 999;"
                + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.12), 8, 0, 0, 2);");
        return l;
    }

    private VBox features() {

        Label heading = new Label("Built for learning and safe change");
        heading.setStyle(strong(20));
        Label sub = wrapped(
                "Six product pillars you can open in one click — each one is a real page in CleanCheck.",
                muted(13));

        var rowOne = cardRow(
                feature("!", "Explained findings",
                        "Every issue: why it matters, how to fix it, before/after example, and code context.", "Issues"),
                feature("✓", "Verified auto-fix",
                        "Backup → edit → re-analyze → compile check. Only proven fixes stay.",
                        "Fix Verification"),
                feature("⇄", "Regression tracking",
                        "After the next scan, see what improved, what regressed, and what is still open.", "Regression"));
        var rowTwo = cardRow(
                feature("◎", "Security review",
                        "OWASP-linked findings with CWE ids — preliminary, offline, no code uploaded.",
                        "Security Review"),
                feature("▦", "Risk hotspots",
                        "Files and rules ranked by severity weight so you know where to start.",
                        "Metrics"),
                feature("☰", "Class Mode",
                        "One folder of student projects → ranking, repeated mistakes, per-student detail.",
                        "Class Mode"));

        // Workflow strip under the cards
        Label flowTitle = new Label("Recommended demo path");
        flowTitle.setStyle(strong(15));
        HBox flow = new HBox(10,
                stepChip("1", "Analyze"),
                stepChip("2", "Issues + Fix"),
                stepChip("3", "Security"),
                stepChip("4", "Metrics"),
                stepChip("5", "Regression"));
        flow.setAlignment(Pos.CENTER_LEFT);
        VBox flowBox = new VBox(10, flowTitle, flow);
        flowBox.setPadding(new Insets(18));
        flowBox.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 16;"
                + "-fx-border-color: #BFDBFE; -fx-border-radius: 16;");

        return new VBox(16, new VBox(4, heading, sub), rowOne, rowTwo, flowBox);
    }

    private static Label stepChip(String n, String text) {
        Label l = new Label(n + "  " + text);
        l.setStyle("-fx-background-color: white; -fx-text-fill: #1E40AF; -fx-font-weight: bold;"
                + "-fx-font-size: 12px; -fx-background-radius: 999; -fx-padding: 8 14;"
                + "-fx-border-color: #93C5FD; -fx-border-radius: 999;");
        return l;
    }

    private VBox feature(String glyph, String title, String text, String targetPage) {
        Label icon = circle(glyph, PRIMARY_SOFT, PRIMARY, 44);
        Label t = new Label(title);
        t.setStyle(strong(16));
        Label go = new Label("Open  \u2192");
        go.setStyle("-fx-text-fill: " + PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);

        VBox box = new VBox(10, icon, t, wrapped(text, muted(13)), grow, go);
        box.setPadding(new Insets(20));
        box.setMinHeight(190);
        String base = GuiTheme.cardStyle();
        String hover = "-fx-background-color: " + CARD + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + PRIMARY + ";"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.25), 28, 0, 0, 10);";
        box.setStyle(base);
        box.setCursor(Cursor.HAND);
        box.setOnMouseEntered(e -> box.setStyle(hover));
        box.setOnMouseExited(e -> box.setStyle(base));
        box.setOnMouseClicked(e -> onNavigate.accept(targetPage));
        return box;
    }

    // ---------------------------------------------------------------------
    // Recent projects
    // ---------------------------------------------------------------------

    private VBox recentSection() {
        recentCard.setPadding(new Insets(22));
        recentCard.setStyle(GuiTheme.cardStyle());
        refresh();
        return recentCard;
    }

    /**
     * Rebuilds the "Recent projects" card. Only projects that were really scanned are listed, and the whole
     * card is hidden while there are none, so a fresh start shows nothing here.
     */
    public void refresh() {
        recentCard.getChildren().clear();
        Label heading = new Label("Recent projects");
        heading.setStyle(strong(20));
        recentCard.getChildren().add(heading);

        List<Path> projects = recent.get();
        int shown = 0;
        if (projects != null) {
            for (Path path : projects) {
                ScanSummary scan = latestScan(path);
                if (scan != null) {
                    recentCard.getChildren().add(row(path, scan));
                    shown++;
                }
            }
        }
        recentCard.setVisible(shown > 0);
        recentCard.setManaged(shown > 0);
    }

    private HBox row(Path path, ScanSummary scan) {
        String name = path.getFileName() == null ? path.toString() : path.getFileName().toString();
        Label folder = circle("\uD83D\uDCC1", PRIMARY_SOFT, PRIMARY, 40);
        Label title = new Label(name);
        title.setStyle(strong(14));
        Label location = new Label(shortPath(path.toAbsolutePath().toString(), 80));
        location.setStyle(muted(12));
        VBox text = new VBox(2, title, location);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(14, folder, text, spacer);
        Label counts = new Label(scan.criticalCount() + " critical  \u00B7  "
                + scan.warningCount() + " warnings  \u00B7  " + scan.infoCount() + " info");
        counts.setStyle(muted(12));
        row.getChildren().addAll(counts, badge(scan.score() + " / 100", "white", scoreColor(scan.score())));

        Button open = new Button("Open");
        open.setStyle(GuiTheme.primaryButton() + "-fx-padding: 8 20 8 20; -fx-font-size: 12px;");
        open.setOnAction(e -> onOpen.accept(path));
        Button forget = new Button("\u2715");
        forget.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MUTED + ";"
                + "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 6 8 6 8;");
        forget.setTooltip(new javafx.scene.control.Tooltip("Remove from this list"));
        forget.setOnAction(e -> {
            onForget.accept(path);
            refresh();
        });
        row.getChildren().addAll(open, forget);

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 4, 12, 4));
        row.setStyle("-fx-border-color: " + BORDER + "; -fx-border-width: 0 0 1 0;");
        return row;
    }

    private static String scoreColor(int score) {
        if (score >= 80) {
            return SUCCESS;
        }
        return score >= 50 ? WARNING : CRITICAL;
    }

    /** Latest stored scan of a project, or null when it was never scanned (or the database is unavailable). */
    private static ScanSummary latestScan(Path path) {
        try {
            ScanSummaryRepository repository = new ScanSummaryRepository();
            int projectId = repository.findProjectId(path);
            if (projectId < 0) {
                return null;
            }
            List<ScanSummary> scans = repository.findLatest(projectId, 1);
            return scans.isEmpty() ? null : scans.get(0);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Label footer() {
        Label l = new Label("CleanCheck  ·  Offline  ·  AST  ·  Mentored Review  ·  Safe Fix");
        l.setStyle(muted(12));
        l.setAlignment(Pos.CENTER);
        return l;
    }
}
