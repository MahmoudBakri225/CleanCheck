package com.mycompany.cleancheck;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CleanCheck - Java Static Code Quality Analyzer
 *
 * Layout: dark header + project strip on top, sidebar on the left, one page at
 * a time in the centre (Dashboard, Scan Results, Issues, Metrics, Rules,
 * About).
 */
public class C_CleanCheckGUI extends Application {

    // =========================================================
    // PALETTE
    // =========================================================
    private static final String BG = "#F1F5F9";
    private static final String CARD = "#FFFFFF";
    private static final String HEADER_BG = "#0F172A";
    private static final String PRIMARY = "#2563EB";
    private static final String TEXT = "#0F172A";
    private static final String MUTED = "#64748B";
    private static final String BORDER = "#E2E8F0";
    private static final String CRITICAL = "#DC2626";
    private static final String WARNING = "#F97316";
    private static final String INFO = "#2563EB";
    private static final String SUCCESS = "#16A34A";

    private static final String FOLDER = "\uD83D\uDCC1  ";

    // =========================================================
    // STATE
    // =========================================================
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private StackPane pageHost;

    private Path projectPath;
    private Label projectLabel;
    private Label statusLabel;

    // dashboard
    private final Label scoreValue = new Label("\u2014");
    private final Label totalValue = new Label("0");
    private final Label criticalValue = new Label("0");
    private final Label warningValue = new Label("0");
    private final Label infoValue = new Label("0");
    private final Label timeValue = new Label("\u2014");
    private final ProgressBar scoreBar = new ProgressBar(0);

    // issues
    private final ObservableList<IssueRow> allIssues
            = FXCollections.observableArrayList();

    private FilteredList<IssueRow> filteredIssues;

    private TableView<IssueRow> issuesTable;

    private ComboBox<String> severityFilter;

    private final Label detailFile = new Label("-");
    private final Label detailLine = new Label("-");
    private final Label detailRule = new Label("-");
    private final Label detailSeverity = new Label("-");

    private TextArea suggestionArea;
    private TextArea codePreviewArea;

    private Button applyFixButton;

    // metrics + raw output
    private TableView<HotspotRow> fileHotspotsTable;
    private TableView<HotspotRow> ruleHotspotsTable;

    private TextArea outputArea;

    // =========================================================
    // START
    // =========================================================
    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();

        root.setStyle(
                "-fx-background-color: " + BG + ";"
                + "-fx-font-family: 'Segoe UI', 'System';"
        );

        root.setTop(createHeader(stage));
        root.setLeft(createSidebar());

        pageHost = new StackPane();

        root.setCenter(pageHost);

        pages.put(
                "Dashboard",
                scroll(createDashboardPage())
        );

        pages.put(
                "Scan Results",
                scroll(createResultsPage())
        );

        pages.put(
                "Issues",
                scroll(createIssuesPage())
        );

        pages.put(
                "Metrics",
                scroll(createMetricsPage())
        );

        pages.put(
                "Rules",
                scroll(createRulesPage())
        );

        pages.put(
                "About",
                scroll(createAboutPage())
        );

        showPage("Dashboard");

        Scene scene
                = new Scene(
                        root,
                        1350,
                        850
                );

        stage.setTitle(
                "CleanCheck \u2014 Code Quality Analysis"
        );

        stage.setScene(scene);

        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setMaximized(true);

        stage.show();
    }

    // =========================================================
    // HEADER
    // =========================================================
    private VBox createHeader(Stage stage) {

        Label logo = new Label("CC");

        logo.setMinSize(52, 52);
        logo.setPrefSize(52, 52);

        logo.setAlignment(Pos.CENTER);

        logo.setStyle(
                "-fx-background-color: white;"
                + "-fx-background-radius: 12;"
                + "-fx-text-fill: " + HEADER_BG + ";"
                + "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
        );

        Label title
                = new Label("CleanCheck");

        title.setStyle(
                "-fx-text-fill: white;"
                + "-fx-font-size: 24px;"
                + "-fx-font-weight: bold;"
        );

        Label subtitle
                = new Label(
                        "Java Static Code Quality Analyzer"
                );

        subtitle.setStyle(
                "-fx-text-fill: #94A3B8;"
                + "-fx-font-size: 13px;"
        );

        VBox titleBox
                = new VBox(
                        2,
                        title,
                        subtitle
                );

        Region spacer
                = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        Button chooseButton
                = button(
                        FOLDER + "Choose Project",
                        "#3B82F6"
                );

        chooseButton.setOnAction(
                e -> chooseProject(stage)
        );

        Button startButton
                = button(
                        "\u25B6  Start Analysis",
                        "#22C55E"
                );

        startButton.setOnAction(
                e -> startAnalysis()
        );

        HBox topBar
                = new HBox(
                        16,
                        logo,
                        titleBox,
                        spacer,
                        chooseButton,
                        startButton
                );

        topBar.setAlignment(
                Pos.CENTER_LEFT
        );

        topBar.setPadding(
                new Insets(
                        16,
                        28,
                        16,
                        28
                )
        );

        topBar.setStyle(
                "-fx-background-color: " + HEADER_BG + ";"
        );

        projectLabel
                = new Label(
                        FOLDER + "No project selected"
                );

        projectLabel.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 13px;"
        );

        statusLabel
                = new Label();

        setStatus(
                "Ready",
                SUCCESS
        );

        Region strip
                = new Region();

        HBox.setHgrow(
                strip,
                Priority.ALWAYS
        );

        HBox statusStrip
                = new HBox(
                        16,
                        projectLabel,
                        strip,
                        statusLabel
                );

        statusStrip.setAlignment(
                Pos.CENTER_LEFT
        );

        statusStrip.setPadding(
                new Insets(
                        12,
                        28,
                        12,
                        28
                )
        );

        statusStrip.setStyle(
                "-fx-background-color: white;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 0 1 0;"
        );

        return new VBox(
                topBar,
                statusStrip
        );
    }

    private void setStatus(
            String text,
            String color
    ) {

        statusLabel.setText(text);

        statusLabel.setStyle(
                "-fx-text-fill: " + color + ";"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
        );
    }

    // =========================================================
    // SIDEBAR
    // =========================================================
    private VBox createSidebar() {

        String[][] items = {
            {"Dashboard", "\u2302"},
            {"Scan Results", "\u25A4"},
            {"Issues", "\u26A0"},
            {"Metrics", "\u25A5"},
            {"Rules", "\u2630"},
            {"About", "\u24D8"}
        };

        VBox bar
                = new VBox(6);

        bar.setPrefWidth(220);
        bar.setMinWidth(220);

        bar.setPadding(
                new Insets(
                        20,
                        14,
                        20,
                        14
                )
        );

        bar.setStyle(
                "-fx-background-color: white;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 1 0 0;"
        );

        for (String[] item : items) {

            Button b
                    = new Button(
                            item[1]
                            + "    "
                            + item[0]
                    );

            b.setMaxWidth(
                    Double.MAX_VALUE
            );

            b.setAlignment(
                    Pos.CENTER_LEFT
            );

            styleNav(
                    b,
                    false
            );

            b.setOnAction(
                    e -> showPage(item[0])
            );

            navButtons.put(
                    item[0],
                    b
            );

            bar.getChildren().add(b);
        }

        Region spacer
                = new Region();

        VBox.setVgrow(
                spacer,
                Priority.ALWAYS
        );

        Label version
                = new Label(
                        "CleanCheck 1.2.0"
                );

        version.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 12px;"
        );

        Label powered
                = new Label(
                        "Powered by JavaFX"
                );

        powered.setStyle(
                "-fx-text-fill: " + MUTED + ";"
                + "-fx-font-size: 12px;"
        );

        bar.getChildren().addAll(
                spacer,
                version,
                powered
        );

        return bar;
    }

    private void styleNav(
            Button b,
            boolean selected
    ) {

        b.setStyle(
                "-fx-background-radius: 10;"
                + "-fx-padding: 12 14 12 14;"
                + "-fx-font-size: 14px;"
                + "-fx-cursor: hand;"
                + (selected
                        ? "-fx-background-color: #E8F0FE;"
                        + "-fx-text-fill: " + PRIMARY + ";"
                        + "-fx-font-weight: bold;"
                        : "-fx-background-color: transparent;"
                        + "-fx-text-fill: " + TEXT + ";")
        );
    }

    private void showPage(
            String name
    ) {

        pageHost
                .getChildren()
                .setAll(
                        pages.get(name)
                );

        navButtons.forEach(
                (key, b)
                -> styleNav(
                        b,
                        key.equals(name)
                )
        );
    }

    // =========================================================
    // DASHBOARD
    // =========================================================
    private VBox createDashboardPage() {

        HBox cards
                = new HBox(
                        16,
                        scoreCard(),
                        statCard(
                                "\u2630",
                                "#94A3B8",
                                "Total Issues",
                                totalValue,
                                TEXT,
                                CARD,
                                BORDER
                        ),
                        statCard(
                                "!",
                                CRITICAL,
                                "Critical",
                                criticalValue,
                                CRITICAL,
                                "#FEF2F2",
                                "#FECACA"
                        ),
                        statCard(
                                "!",
                                WARNING,
                                "Warning",
                                warningValue,
                                WARNING,
                                "#FFF7ED",
                                "#FED7AA"
                        ),
                        statCard(
                                "i",
                                INFO,
                                "Info",
                                infoValue,
                                INFO,
                                "#EFF6FF",
                                "#BFDBFE"
                        ),
                        statCard(
                                "\u23F1",
                                "#CBD5E1",
                                "Analysis Time",
                                timeValue,
                                TEXT,
                                CARD,
                                BORDER
                        )
                );

        VBox page
                = pageBox();

        page.getChildren().addAll(
                titleBlock(
                        "Dashboard",
                        "Overview of the latest static analysis"
                ),
                cards
        );

        return page;
    }

    private VBox scoreCard() {

        scoreValue.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 44px;"
                + "-fx-font-weight: bold;"
        );

        Label outOf
                = new Label("/ 100");

        outOf.setStyle(
                "-fx-text-fill: " + MUTED + ";"
                + "-fx-font-size: 22px;"
        );

        HBox valueRow
                = new HBox(
                        6,
                        scoreValue,
                        outOf
                );

        valueRow.setAlignment(
                Pos.BASELINE_LEFT
        );

        Label label
                = new Label(
                        "Quality Score"
                );

        label.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 15px;"
        );

        HBox top
                = new HBox(
                        16,
                        circle(
                                "\u2714",
                                "#DBEAFE",
                                "#1E3A8A",
                                72
                        ),
                        new VBox(
                                2,
                                label,
                                valueRow
                        )
                );

        top.setAlignment(
                Pos.CENTER_LEFT
        );

        scoreBar.setMaxWidth(
                Double.MAX_VALUE
        );

        scoreBar.setPrefHeight(10);

        Region grow
                = new Region();

        VBox.setVgrow(
                grow,
                Priority.ALWAYS
        );

        VBox box
                = new VBox(
                        14,
                        top,
                        grow,
                        scoreBar
                );

        box.setPadding(
                new Insets(20)
        );

        box.setMinHeight(170);
        box.setPrefWidth(340);

        box.setStyle(
                cardStyle(
                        CARD,
                        BORDER
                )
        );

        HBox.setHgrow(
                box,
                Priority.ALWAYS
        );

        box.setMaxWidth(
                Double.MAX_VALUE
        );

        return box;
    }

    private VBox statCard(
            String icon,
            String iconBg,
            String title,
            Label value,
            String valueColor,
            String cardBg,
            String cardBorder
    ) {

        value.setStyle(
                "-fx-text-fill: " + valueColor + ";"
                + "-fx-font-size: 38px;"
                + "-fx-font-weight: bold;"
        );

        Label name
                = new Label(title);

        name.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 15px;"
        );

        VBox box
                = new VBox(
                        10,
                        circle(
                                icon,
                                iconBg,
                                "white",
                                46
                        ),
                        name,
                        value
                );

        box.setPadding(
                new Insets(20)
        );

        box.setMinHeight(170);
        box.setPrefWidth(170);

        box.setStyle(
                cardStyle(
                        cardBg,
                        cardBorder
                )
        );

        HBox.setHgrow(
                box,
                Priority.ALWAYS
        );

        box.setMaxWidth(
                Double.MAX_VALUE
        );

        return box;
    }

    // =========================================================
    // ISSUES PAGE
    // =========================================================
    private VBox createIssuesPage() {

        severityFilter
                = new ComboBox<>();

        severityFilter
                .getItems()
                .addAll(
                        "All",
                        "CRITICAL",
                        "WARNING",
                        "INFO"
                );

        severityFilter.setValue("All");
        severityFilter.setPrefWidth(150);

        severityFilter.setOnAction(
                e -> applySeverityFilter()
        );

        Label filterLabel
                = new Label("Severity:");

        filterLabel.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 14px;"
        );

        HBox filterBox
                = new HBox(
                        10,
                        filterLabel,
                        severityFilter
                );

        filterBox.setAlignment(
                Pos.CENTER
        );

        Region spacer
                = new Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        HBox header
                = new HBox(
                        titleBlock(
                                "Issues",
                                "Detected problems in the scanned Java source files."
                        ),
                        spacer,
                        filterBox
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        createIssuesTable();

        filteredIssues
                = new FilteredList<>(
                        allIssues,
                        i -> true
                );

        issuesTable.setItems(
                filteredIssues
        );

        issuesTable.setPrefHeight(420);

        HBox.setHgrow(
                issuesTable,
                Priority.ALWAYS
        );

        issuesTable
                .getSelectionModel()
                .setSelectionMode(
                        SelectionMode.SINGLE
                );

        issuesTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, newValue)
                        -> showIssueDetails(newValue)
                );

        HBox body
                = new HBox(
                        18,
                        issuesTable,
                        createDetailsPanel()
                );

        Label codeTitle
                = new Label(
                        "Code Preview"
                );

        codeTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
        );

        codePreviewArea
                = new TextArea(
                        "Select an issue from the table."
                );

        codePreviewArea.setEditable(false);
        codePreviewArea.setWrapText(false);
        codePreviewArea.setPrefRowCount(9);

        codePreviewArea.setStyle(
                "-fx-control-inner-background: #111827;"
                + "-fx-text-fill: #E5E7EB;"
                + "-fx-font-family: 'Consolas';"
                + "-fx-font-size: 13px;"
        );

        VBox codeCard
                = new VBox(
                        10,
                        codeTitle,
                        codePreviewArea
                );

        codeCard.setPadding(
                new Insets(18)
        );

        codeCard.setStyle(
                cardStyle(
                        CARD,
                        BORDER
                )
        );

        VBox page
                = pageBox();

        page.getChildren().addAll(
                header,
                body,
                codeCard
        );

        return page;
    }

    private void createIssuesTable() {

        issuesTable
                = new TableView<>();

        issuesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<IssueRow, String> file
                = column(
                        "File",
                        "file",
                        150
                );

        TableColumn<IssueRow, String> line
                = column(
                        "Line",
                        "line",
                        60
                );

        TableColumn<IssueRow, String> rule
                = column(
                        "Rule",
                        "rule",
                        140
                );

        TableColumn<IssueRow, String> severity
                = column(
                        "Severity",
                        "severity",
                        100
                );

        TableColumn<IssueRow, String> message
                = column(
                        "Message",
                        "message",
                        320
                );

        severity.setCellFactory(
                col
                -> new TableCell<IssueRow, String>() {

            @Override
            protected void updateItem(
                    String item,
                    boolean empty
            ) {

                super.updateItem(
                        item,
                        empty
                );

                if (empty || item == null) {

                    setText(null);
                    setStyle("");

                    return;
                }

                setText(item);
                setAlignment(Pos.CENTER);

                setStyle(
                        "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + severityColor(item)
                        + ";"
                );
            }
        }
        );

        issuesTable
                .getColumns()
                .addAll(
                        file,
                        line,
                        rule,
                        severity,
                        message
                );
    }

    private VBox createDetailsPanel() {

        Label icon
                = new Label("\u24D8");

        icon.setStyle(
                "-fx-text-fill: " + PRIMARY + ";"
                + "-fx-font-size: 22px;"
        );

        Label title
                = new Label(
                        "Issue Details"
                );

        title.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 18px;"
                + "-fx-font-weight: bold;"
        );

        HBox head
                = new HBox(
                        10,
                        icon,
                        title
                );

        head.setAlignment(
                Pos.CENTER_LEFT
        );

        for (Label l
                : new Label[]{
                    detailFile,
                    detailLine,
                    detailRule,
                    detailSeverity
                }) {

            l.setStyle(
                    "-fx-text-fill: " + TEXT + ";"
                    + "-fx-font-size: 14px;"
            );
        }

        GridPane grid
                = new GridPane();

        grid.setHgap(24);
        grid.setVgap(10);

        grid.addRow(
                0,
                key("File:"),
                detailFile
        );

        grid.addRow(
                1,
                key("Line:"),
                detailLine
        );

        grid.addRow(
                2,
                key("Rule:"),
                detailRule
        );

        grid.addRow(
                3,
                key("Severity:"),
                detailSeverity
        );

        Label fixIcon
                = new Label("\uD83D\uDD27");

        fixIcon.setStyle(
                "-fx-font-size: 18px;"
        );

        Label fixTitle
                = new Label(
                        "Suggested Fix"
                );

        fixTitle.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
        );

        HBox fixHead
                = new HBox(
                        10,
                        fixIcon,
                        fixTitle
                );

        fixHead.setAlignment(
                Pos.CENTER_LEFT
        );

        suggestionArea
                = new TextArea();

        suggestionArea.setEditable(false);
        suggestionArea.setWrapText(true);
        suggestionArea.setPrefRowCount(6);

        suggestionArea.setStyle(
                "-fx-control-inner-background: white;"
                + "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 13px;"
        );

        applyFixButton
                = button(
                        "\u2714  Apply Fix",
                        SUCCESS
                );

        applyFixButton.setMaxWidth(
                Double.MAX_VALUE
        );

        applyFixButton.setPrefHeight(46);
        applyFixButton.setDisable(true);

        applyFixButton.setOnAction(
                e -> applySelectedFix()
        );

        VBox panel
                = new VBox(
                        14,
                        head,
                        grid,
                        new Separator(),
                        fixHead,
                        suggestionArea,
                        applyFixButton
                );

        panel.setPadding(
                new Insets(20)
        );

        panel.setPrefWidth(350);
        panel.setMinWidth(330);
        panel.setMaxWidth(350);

        panel.setStyle(
                cardStyle(
                        CARD,
                        BORDER
                )
        );

        return panel;
    }

    // =========================================================
    // OTHER PAGES
    // =========================================================
    private VBox createResultsPage() {

        outputArea
                = new TextArea();

        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(28);

        outputArea.setStyle(
                "-fx-control-inner-background: #111827;"
                + "-fx-text-fill: #E5E7EB;"
                + "-fx-font-family: 'Consolas';"
                + "-fx-font-size: 12px;"
        );

        VBox page
                = pageBox();

        page.getChildren().addAll(
                titleBlock(
                        "Scan Results",
                        "Raw analyzer output of the latest scan."
                ),
                outputArea
        );

        return page;
    }

    private VBox createMetricsPage() {

        fileHotspotsTable
                = createHotspotTable();

        ruleHotspotsTable
                = createHotspotTable();

        VBox files
                = new VBox(
                        10,
                        sectionLabel("File Hotspots"),
                        fileHotspotsTable
                );

        VBox rules
                = new VBox(
                        10,
                        sectionLabel("Rule Hotspots"),
                        ruleHotspotsTable
                );

        HBox.setHgrow(
                files,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                rules,
                Priority.ALWAYS
        );

        VBox page
                = pageBox();

        page.getChildren().addAll(
                titleBlock(
                        "Metrics",
                        "Areas with the highest concentration of detected issues."
                ),
                new HBox(
                        18,
                        files,
                        rules
                )
        );

        return page;
    }

    private VBox createRulesPage() {

        String[][] rules = {
            {
                "Encapsulation",
                "Fields should be private and accessed through getters and setters."
            },
            {
                "Empty Method",
                "Methods with an empty body."
            },
            {
                "Empty If",
                "if statements with an empty block."
            },
            {
                "Infinite Loop",
                "Loops such as while (true) or for (;;) with no visible exit."
            },
            {
                "Too Many Parameters",
                "Methods that take too many parameters."
            },
            {
                "Naming Convention",
                "Names that do not follow Java naming conventions."
            },
            {
                "Data Type",
                "Wrong or unnecessary types, such as Int, string or wrapper types."
            }
        };

        VBox list
                = new VBox(0);

        list.setStyle(
                cardStyle(
                        CARD,
                        BORDER
                )
        );

        for (String[] r : rules) {

            Label name
                    = new Label(r[0]);

            name.setMinWidth(190);

            name.setStyle(
                    "-fx-text-fill: " + TEXT + ";"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: bold;"
            );

            Label desc
                    = new Label(r[1]);

            desc.setStyle(
                    "-fx-text-fill: " + MUTED + ";"
                    + "-fx-font-size: 14px;"
            );

            HBox row
                    = new HBox(
                            16,
                            name,
                            desc
                    );

            row.setPadding(
                    new Insets(
                            16,
                            20,
                            16,
                            20
                    )
            );

            row.setStyle(
                    "-fx-border-color: " + BORDER + ";"
                    + "-fx-border-width: 0 0 1 0;"
            );

            list.getChildren().add(row);
        }

        VBox page
                = pageBox();

        page.getChildren().addAll(
                titleBlock(
                        "Rules",
                        "What CleanCheck looks for in your code."
                ),
                list
        );

        return page;
    }

    private VBox createAboutPage() {

        Label body
                = new Label(
                        "CleanCheck 1.2.0\n"
                        + "A static code quality analyzer for Java projects.\n"
                        + "Powered by JavaFX."
                );

        body.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 15px;"
        );

        VBox card
                = new VBox(body);

        card.setPadding(
                new Insets(24)
        );

        card.setStyle(
                cardStyle(
                        CARD,
                        BORDER
                )
        );

        VBox page
                = pageBox();

        page.getChildren().addAll(
                titleBlock(
                        "About",
                        "Version and credits."
                ),
                card
        );

        return page;
    }

    private TableView<HotspotRow> createHotspotTable() {

        TableView<HotspotRow> table
                = new TableView<>();

        table.setPrefHeight(420);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        TableColumn<HotspotRow, String> name
                = column(
                        "Name",
                        "name",
                        200
                );

        TableColumn<HotspotRow, String> issues
                = column(
                        "Issues",
                        "issues",
                        80
                );

        TableColumn<HotspotRow, String> risk
                = column(
                        "Risk Score",
                        "riskScore",
                        100
                );

        risk.setCellFactory(
                col
                -> new TableCell<HotspotRow, String>() {

            @Override
            protected void updateItem(
                    String item,
                    boolean empty
            ) {

                super.updateItem(
                        item,
                        empty
                );

                if (empty || item == null) {

                    setText(null);
                    setStyle("");

                    return;
                }

                setText(item);
                setAlignment(Pos.CENTER);

                String color = INFO;

                try {

                    int r
                            = Integer.parseInt(item);

                    color
                            = r >= 20
                                    ? CRITICAL
                                    : (r >= 10
                                            ? WARNING
                                            : INFO);

                } catch (NumberFormatException ignored) {
                    // keep default colour
                }

                setStyle(
                        "-fx-font-weight: bold;"
                        + "-fx-text-fill: "
                        + color
                        + ";"
                );
            }
        }
        );

        table.getColumns().addAll(
                name,
                issues,
                risk
        );

        return table;
    }

    // =========================================================
    // ISSUE DETAILS
    // =========================================================
    private void showIssueDetails(
            IssueRow issue
    ) {

        if (issue == null) {

            clearIssueDetails();

            return;
        }

        detailFile.setText(
                issue.getFile()
        );

        detailLine.setText(
                issue.getLine()
        );

        detailRule.setText(
                issue.getRule()
        );

        detailSeverity.setText(
                issue.getSeverity()
        );

        detailSeverity.setStyle(
                "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: "
                + severityColor(
                        issue.getSeverity()
                )
                + ";"
        );

        suggestionArea.setText(
                issue.getSuggestion()
        );

        applyFixButton.setDisable(false);

        loadCodePreview(
                issue.getFile(),
                issue.getLine()
        );
    }

    private void clearIssueDetails() {

        if (suggestionArea == null) {
            return;
        }

        detailFile.setText("-");
        detailLine.setText("-");
        detailRule.setText("-");
        detailSeverity.setText("-");

        detailSeverity.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 14px;"
        );

        suggestionArea.clear();

        codePreviewArea.setText(
                "Select an issue from the table."
        );

        applyFixButton.setDisable(true);
    }

    private void loadCodePreview(
            String fileName,
            String lineText
    ) {

        if (projectPath == null) {

            codePreviewArea.setText(
                    "Project path is not selected."
            );

            return;
        }

        int issueLine;

        try {

            issueLine
                    = Integer.parseInt(
                            lineText.trim()
                    );

        } catch (NumberFormatException ex) {

            codePreviewArea.setText(
                    "Invalid line number: "
                    + lineText
            );

            return;
        }

        try {

            Path filePath
                    = findJavaFile(
                            projectPath,
                            fileName
                    );

            if (filePath == null) {

                codePreviewArea.setText(
                        "Source file not found: "
                        + fileName
                );

                return;
            }

            List<String> lines
                    = Files.readAllLines(
                            filePath
                    );

            if (lines.isEmpty()) {

                codePreviewArea.setText(
                        "The source file is empty."
                );

                return;
            }

            int start
                    = Math.max(
                            1,
                            issueLine - 3
                    );

            int end
                    = Math.min(
                            lines.size(),
                            issueLine + 3
                    );

            StringBuilder preview
                    = new StringBuilder();

            for (int n = start;
                    n <= end;
                    n++) {

                preview
                        .append(
                                n == issueLine
                                        ? "  >>> "
                                        : "      "
                        )
                        .append(
                                String.format(
                                        "%4d | ",
                                        n
                                )
                        )
                        .append(
                                lines.get(n - 1)
                        )
                        .append(
                                System.lineSeparator()
                        );
            }

            codePreviewArea.setText(
                    preview.toString()
            );

            codePreviewArea.positionCaret(0);

        } catch (IOException ex) {

            codePreviewArea.setText(
                    "Unable to read source file.\n\n"
                    + ex.getMessage()
            );
        }
    }

    private Path findJavaFile(
            Path project,
            String fileName
    ) throws IOException {

        Path direct
                = project.resolve(fileName);

        if (Files.isRegularFile(direct)) {
            return direct;
        }

        try (var stream = Files.walk(project)) {

            return stream
                    .filter(Files::isRegularFile)
                    .filter(
                            p
                            -> p.getFileName()
                                    .toString()
                                    .equals(fileName)
                    )
                    .findFirst()
                    .orElse(null);
        }
    }

    // =========================================================
    // APPLY FIX
    // =========================================================
    private void applySelectedFix() {

        IssueRow issue
                = issuesTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (issue == null) {

            showAlert(
                    "No Issue Selected",
                    "Please select an issue first."
            );

            return;
        }

        if (projectPath == null) {

            showAlert(
                    "No Project",
                    "Please select a project first."
            );

            return;
        }

        try {

            Path javaFile
                    = findJavaFile(
                            projectPath,
                            issue.getFile()
                    );

            if (javaFile == null) {

                showAlert(
                        "File Not Found",
                        "Could not find:\n"
                        + issue.getFile()
                );

                return;
            }

            String before
                    = Files.readString(
                            javaFile
                    );

            int issueLine;

            try {

                issueLine
                        = Integer.parseInt(
                                issue.getLine().trim()
                        );

            } catch (Exception ignored) {

                issueLine = -1;
            }

            new C_FixEngine().applyFix(
                    javaFile,
                    issue.getRule(),
                    issueLine
            );

            String after
                    = Files.readString(
                            javaFile
                    );

            if (!before.equals(after)) {

                showAlert(
                        "Fix Applied",
                        "The selected issue was fixed.\n\n"
                        + "File: "
                        + issue.getFile()
                        + "\n"
                        + "Rule: "
                        + issue.getRule()
                        + "\n\n"
                        + "Run Analysis again to verify the result."
                );

                applyFixButton.setDisable(true);

                loadCodePreview(
                        issue.getFile(),
                        issue.getLine()
                );

                setStatus(
                        "Fix applied. Run Analysis again to verify.",
                        WARNING
                );

            } else {

                showAlert(
                        "No Automatic Fix",
                        "CleanCheck could not fix this issue automatically.\n\n"
                        + "Rule: "
                        + issue.getRule()
                        + "\n\n"
                        + "Review the Suggested Fix manually."
                );

                setStatus(
                        "No automatic fix for the selected issue.",
                        WARNING
                );
            }

        } catch (Exception ex) {

            showAlert(
                    "Fix Error",
                    ex.getMessage()
            );

            setStatus(
                    "Fix failed.",
                    CRITICAL
            );
        }
    }

    private void showAlert(
            String title,
            String message
    ) {

        Alert alert
                = new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(title);
        alert.setHeaderText(null);

        alert.setContentText(
                message == null
                        ? "Unknown error."
                        : message
        );

        alert.showAndWait();
    }

    // =========================================================
    // PROJECT + ANALYSIS
    // =========================================================
    private void chooseProject(
            Stage stage
    ) {

        DirectoryChooser chooser
                = new DirectoryChooser();

        chooser.setTitle(
                "Select Java Project"
        );

        File selected
                = chooser.showDialog(stage);

        if (selected == null) {
            return;
        }

        projectPath
                = selected.toPath();

        projectLabel.setText(
                FOLDER
                + selected.getAbsolutePath()
        );

        setStatus(
                "Project selected",
                MUTED
        );

        clearIssueDetails();
    }

    private void startAnalysis() {

        if (projectPath == null) {
            setStatus("Please select a project first.", WARNING);
            return;
        }

        setStatus("Analysis is running...", MUTED);
        outputArea.clear();
        allIssues.clear();
        clearDashboard();
        clearIssueDetails();

        Thread thread = new Thread(() -> {

            long startTime = System.nanoTime();

            ByteArrayOutputStream output
                    = new ByteArrayOutputStream();

            PrintStream originalOut
                    = System.out;

            PrintStream captureOut
                    = new PrintStream(
                            output,
                            true,
                            StandardCharsets.UTF_8
                    );

            try {

                System.setOut(captureOut);

                /*
             * =====================================================
             * ANALYZE ONLY
             * =====================================================
             *
             * Start Analysis must NOT modify source files.
             *
             * Auto Fix is disabled here.
             *
             * Fixes are applied only when the user selects
             * an issue and clicks Apply Fix.
             * =====================================================
                 */
                C_CleanCheck.main(
                        new String[]{
                            projectPath.toString(),
                            "--analyze-only"
                        }
                );

            } catch (Exception ex) {

                ex.printStackTrace(captureOut);

            } finally {

                System.setOut(originalOut);
                captureOut.close();
            }

            double seconds
                    = (System.nanoTime() - startTime)
                    / 1_000_000_000.0;

            String result
                    = output.toString(
                            StandardCharsets.UTF_8
                    );

            Platform.runLater(() -> {

                outputArea.setText(result);

                parseIssues(result);

                updateDashboard(
                        result,
                        seconds
                );

                updateHotspots();

                if (!allIssues.isEmpty()) {

                    issuesTable
                            .getSelectionModel()
                            .select(0);
                }

                setStatus(
                        "\u2714  Analysis completed",
                        SUCCESS
                );
            });

        });

        thread.setDaemon(true);
        thread.start();
    }

    private void clearDashboard() {

        scoreValue.setText("\u2014");

        scoreBar.setProgress(0);

        totalValue.setText("0");
        criticalValue.setText("0");
        warningValue.setText("0");
        infoValue.setText("0");

        timeValue.setText("\u2014");

        if (fileHotspotsTable != null) {

            fileHotspotsTable
                    .getItems()
                    .clear();
        }

        if (ruleHotspotsTable != null) {

            ruleHotspotsTable
                    .getItems()
                    .clear();
        }
    }

    private void updateDashboard(
            String output,
            double seconds
    ) {

        totalValue.setText(
                String.valueOf(
                        allIssues.size()
                )
        );

        criticalValue.setText(
                String.valueOf(
                        countSeverity("CRITICAL")
                )
        );

        warningValue.setText(
                String.valueOf(
                        countSeverity("WARNING")
                )
        );

        infoValue.setText(
                String.valueOf(
                        countSeverity("INFO")
                )
        );

        timeValue.setText(
                String.format(
                        "%.1fs",
                        seconds
                )
        );

        Matcher m
                = Pattern
                        .compile(
                                "Initial Score\\s*:\\s*(\\d+)"
                                + "\\s*/\\s*100",
                                Pattern.CASE_INSENSITIVE
                        )
                        .matcher(output);

        if (m.find()) {

            int score
                    = Integer.parseInt(
                            m.group(1)
                    );

            scoreValue.setText(
                    String.valueOf(score)
            );

            scoreBar.setProgress(
                    score / 100.0
            );

            String color
                    = score >= 80
                            ? SUCCESS
                            : (score >= 50
                                    ? WARNING
                                    : CRITICAL);

            scoreBar.setStyle(
                    "-fx-accent: "
                    + color
                    + ";"
            );

        } else {

            scoreValue.setText(
                    "\u2014"
            );

            scoreBar.setProgress(0);
        }
    }

    private int countSeverity(
            String severity
    ) {

        int count = 0;

        for (IssueRow issue : allIssues) {

            if (issue
                    .getSeverity()
                    .equalsIgnoreCase(
                            severity
                    )) {

                count++;
            }
        }

        return count;
    }

    // =========================================================
    // PARSE OUTPUT
    // =========================================================
    private void parseIssues(
            String output
    ) {

        allIssues.clear();

        Set<String> keys
                = new HashSet<>();

        Pattern header
                = Pattern.compile(
                        "^!\\s*severity\\s*:\\s*(.*?)"
                        + "\\s*\\|\\s*fileName\\s*:\\s*(.*?)"
                        + "\\s*\\|\\s*Line\\s*:\\s*(.*?)"
                        + "\\s*\\|\\s*ruleName\\s*:\\s*(.*)$",
                        Pattern.CASE_INSENSITIVE
                );

        String severity = "";
        String file = "";
        String line = "";
        String rule = "";

        String message = "";
        String suggestion = "";

        for (String raw
                : output.split("\\R")) {

            String text
                    = raw.trim();

            Matcher m
                    = header.matcher(text);

            if (m.matches()) {

                if (!severity.isEmpty()) {

                    addIssueIfUnique(
                            keys,
                            file,
                            line,
                            rule,
                            severity,
                            message,
                            suggestion
                    );
                }

                severity
                        = m.group(1).trim();

                file
                        = m.group(2).trim();

                line
                        = m.group(3).trim();

                rule
                        = m.group(4).trim();

                message = "";
                suggestion = "";

                continue;
            }

            if (text.startsWith("Message")
                    && text.contains(":")) {

                message
                        = text.substring(
                                text.indexOf(':') + 1
                        ).trim();

            } else if (text.startsWith("Suggestion")
                    && text.contains(":")) {

                suggestion
                        = text.substring(
                                text.indexOf(':') + 1
                        ).trim();
            }
        }

        if (!severity.isEmpty()) {

            addIssueIfUnique(
                    keys,
                    file,
                    line,
                    rule,
                    severity,
                    message,
                    suggestion
            );
        }

        applySeverityFilter();
    }

    private void addIssueIfUnique(
            Set<String> keys,
            String file,
            String line,
            String rule,
            String severity,
            String message,
            String suggestion
    ) {

        String key
                = file
                + "|"
                + line
                + "|"
                + rule
                + "|"
                + severity
                + "|"
                + message;

        if (keys.add(key)) {

            allIssues.add(
                    new IssueRow(
                            file,
                            line,
                            rule,
                            severity,
                            message,
                            suggestion
                    )
            );
        }
    }

    private void applySeverityFilter() {

        if (filteredIssues == null) {
            return;
        }

        String selected
                = severityFilter.getValue();

        if (selected == null
                || selected.equals("All")) {

            filteredIssues.setPredicate(
                    i -> true
            );

        } else {

            filteredIssues.setPredicate(
                    i
                    -> i.getSeverity()
                            .equalsIgnoreCase(
                                    selected
                            )
            );
        }
    }

    // =========================================================
    // HOTSPOTS
    // =========================================================
    private void updateHotspots() {

        if (fileHotspotsTable == null
                || ruleHotspotsTable == null) {

            return;
        }

        Map<String, int[]> fileMap
                = new HashMap<>();

        Map<String, int[]> ruleMap
                = new HashMap<>();

        for (IssueRow issue : allIssues) {

            int risk
                    = risk(
                            issue.getSeverity()
                    );

            int[] f
                    = fileMap.computeIfAbsent(
                            issue.getFile(),
                            k -> new int[2]
                    );

            f[0]++;
            f[1] += risk;

            int[] r
                    = ruleMap.computeIfAbsent(
                            issue.getRule(),
                            k -> new int[2]
                    );

            r[0]++;
            r[1] += risk;
        }

        fileHotspotsTable
                .getItems()
                .setAll(
                        toRows(fileMap)
                );

        ruleHotspotsTable
                .getItems()
                .setAll(
                        toRows(ruleMap)
                );
    }

    private List<HotspotRow> toRows(
            Map<String, int[]> map
    ) {

        List<HotspotRow> rows
                = new ArrayList<>();

        map.forEach(
                (name, d)
                -> rows.add(
                        new HotspotRow(
                                name,
                                d[0],
                                d[1]
                        )
                )
        );

        rows.sort(
                Comparator.comparingInt(
                        (HotspotRow r)
                        -> Integer.parseInt(
                                r.getRiskScore()
                        )
                ).reversed()
        );

        return rows;
    }

    private int risk(
            String severity
    ) {

        if (severity == null) {
            return 0;
        }

        switch (severity.toUpperCase()) {

            case "CRITICAL":
                return 5;

            case "WARNING":
                return 3;

            case "INFO":
                return 1;

            default:
                return 0;
        }
    }

    // =========================================================
    // SMALL UI HELPERS
    // =========================================================
    private String severityColor(
            String severity
    ) {

        if (severity == null) {
            return TEXT;
        }

        switch (severity.toUpperCase()) {

            case "CRITICAL":
                return CRITICAL;

            case "WARNING":
                return WARNING;

            case "INFO":
                return INFO;

            default:
                return TEXT;
        }
    }

    private <S>
            TableColumn<S, String> column(
                    String title,
                    String property,
                    double width
            ) {

        TableColumn<S, String> col
                = new TableColumn<>(
                        title
                );

        col.setCellValueFactory(
                new PropertyValueFactory<>(
                        property
                )
        );

        col.setPrefWidth(width);

        return col;
    }

    private VBox pageBox() {

        VBox page
                = new VBox(20);

        page.setPadding(
                new Insets(28)
        );

        page.setStyle(
                "-fx-background-color: " + BG + ";"
        );

        return page;
    }

    private VBox titleBlock(
            String title,
            String subtitle
    ) {

        Label t
                = new Label(title);

        t.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 32px;"
                + "-fx-font-weight: bold;"
        );

        Label s
                = new Label(subtitle);

        s.setStyle(
                "-fx-text-fill: " + MUTED + ";"
                + "-fx-font-size: 14px;"
        );

        return new VBox(
                4,
                t,
                s
        );
    }

    private Label sectionLabel(
            String text
    ) {

        Label l
                = new Label(text);

        l.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
        );

        return l;
    }

    private Label key(
            String text
    ) {

        Label l
                = new Label(text);

        l.setMinWidth(70);

        l.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
        );

        return l;
    }

    private ScrollPane scroll(
            Node content
    ) {

        ScrollPane sp
                = new ScrollPane(content);

        sp.setFitToWidth(true);

        sp.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        sp.setStyle(
                "-fx-background-color: " + BG + ";"
                + "-fx-background: " + BG + ";"
        );

        return sp;
    }

    private Button button(
            String text,
            String color
    ) {

        Button b
                = new Button(text);

        b.setStyle(
                "-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 14px;"
                + "-fx-background-radius: 10;"
                + "-fx-padding: 11 20 11 20;"
                + "-fx-cursor: hand;"
        );

        return b;
    }

    private Label circle(
            String text,
            String bg,
            String fg,
            double size
    ) {

        Label l
                = new Label(text);

        l.setMinSize(
                size,
                size
        );

        l.setPrefSize(
                size,
                size
        );

        l.setMaxSize(
                size,
                size
        );

        l.setAlignment(
                Pos.CENTER
        );

        l.setStyle(
                "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: " + size + ";"
                + "-fx-text-fill: " + fg + ";"
                + "-fx-font-size: "
                + (int) (size * 0.45)
                + "px;"
                + "-fx-font-weight: bold;"
        );

        return l;
    }

    private String cardStyle(
            String bg,
            String border
    ) {

        return "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 16;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-radius: 16;"
                + "-fx-effect: dropshadow("
                + "gaussian,"
                + "rgba(15,23,42,0.06),"
                + "12,"
                + "0,"
                + "0,"
                + "3"
                + ");";
    }

    // =========================================================
    // ROW MODELS
    // =========================================================
    public static class IssueRow {

        private final String file;
        private final String line;
        private final String rule;
        private final String severity;
        private final String message;
        private final String suggestion;

        public IssueRow(
                String file,
                String line,
                String rule,
                String severity,
                String message,
                String suggestion
        ) {

            this.file = file;
            this.line = line;
            this.rule = rule;
            this.severity = severity;
            this.message = message;
            this.suggestion = suggestion;
        }

        public String getFile() {
            return file;
        }

        public String getLine() {
            return line;
        }

        public String getRule() {
            return rule;
        }

        public String getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public String getSuggestion() {
            return suggestion;
        }
    }

    public static class HotspotRow {

        private final String name;
        private final String issues;
        private final String riskScore;

        public HotspotRow(
                String name,
                int issues,
                int riskScore
        ) {

            this.name = name;
            this.issues
                    = String.valueOf(issues);

            this.riskScore
                    = String.valueOf(riskScore);
        }

        public String getName() {
            return name;
        }

        public String getIssues() {
            return issues;
        }

        public String getRiskScore() {
            return riskScore;
        }
    }

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(
            String[] args
    ) {

        launch(args);
    }
}
