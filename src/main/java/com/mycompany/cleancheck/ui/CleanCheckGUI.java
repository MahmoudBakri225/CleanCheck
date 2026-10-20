package com.mycompany.cleancheck.ui;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.application.CleanCheck;
import com.mycompany.cleancheck.application.AnalysisResult;
import com.mycompany.cleancheck.application.AppServices;
import com.mycompany.cleancheck.application.ProjectAnalyzer;
import com.mycompany.cleancheck.report.ReportWriter;
import com.mycompany.cleancheck.fix.FixEngine;
import com.mycompany.cleancheck.fix.VerifiedFix;
import com.mycompany.cleancheck.fix.FixVerificationService;
import com.mycompany.cleancheck.rules.RuleEducation;

import static com.mycompany.cleancheck.ui.UiKit.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CleanCheck - Java Static Code Quality Analyzer
 *
 * Layout: dark header + project strip on top, sidebar on the left, one page at
 * a time in the centre (Dashboard, Scan Results, Issues, Metrics, Rules,
 * About).
 */
public class CleanCheckGUI extends Application {

    private PageNavigator pageNavigator;

    // =========================================================
    // PALETTE
    // =========================================================
    private static final String BG = GuiTheme.BG;
    private static final String CARD = GuiTheme.CARD;
    private static final String HEADER_BG = GuiTheme.HEADER_BG;
    private static final String PRIMARY = GuiTheme.PRIMARY;
    private static final String TEXT = GuiTheme.TEXT;
    private static final String MUTED = GuiTheme.MUTED;
    private static final String BORDER = GuiTheme.BORDER;
    private static final String CRITICAL = GuiTheme.CRITICAL;
    private static final String WARNING = GuiTheme.WARNING;
    private static final String INFO = GuiTheme.INFO;
    private static final String SUCCESS = GuiTheme.SUCCESS;

    private static final String FOLDER = "\uD83D\uDCC1  ";

    // =========================================================
    // STATE
    // =========================================================
    private final Map<String, Node> pages = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private StackPane pageHost;

    private Path projectPath;
    private boolean analysisRunning;
    private boolean fixRunning;
    private Button startButton;

    private record FixExecution(Path javaFile, int issueLine, VerifiedFix.Outcome outcome) {
    }
    private BorderPane rootPane;
    private boolean darkMode;
    private final RegressionPage regressionPage = new RegressionPage(() -> projectPath);

    /** Result of the last Analyze run (with its baseline comparison); used by the Report export. */
    private volatile AnalysisResult lastAnalysis;
    private final QualityHistoryPage historyPage = new QualityHistoryPage(() -> projectPath);
    private Label projectLabel;
    private Label statusLabel;

    private final RecentProjects recentProjects = RecentProjects.standard();
    private HomePage homePage;
    private AnalyzePage analyzePage;
    private final IssueExplanationPanel explanation = new IssueExplanationPanel();
    private final CodeReviewPanel codeReview = new CodeReviewPanel();

    // dashboard
    private final DashboardController dashboard = new DashboardController();

    // issues
    private final ObservableList<IssueRow> allIssues
            = FXCollections.observableArrayList();

    private final ObservableList<IssueRow> resolvedIssues
            = FXCollections.observableArrayList();

    private FilteredList<IssueRow> filteredIssues;

    private TableView<IssueRow> issuesTable;

    private ComboBox<String> severityFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<String> ruleFilter;

    /** Special entry of the rule filter: show only the security findings. */
    private static final String SECURITY_ONLY = "Security only";
    private javafx.scene.control.TextField issueSearchField;

    private final Label detailFile = new Label("-");
    private final Label detailLine = new Label("-");
    private final Label detailRule = new Label("-");
    private final Label detailSeverity = new Label("-");
    private final Label detailRisk = new Label("-");
    private final Label detailConfidence = new Label("-");
    private final Label educationLabel = new Label("Select a finding to see its learning note.");

    private TextArea suggestionArea;
    private TextArea codePreviewArea;

    private Button applyFixButton;
    private Button rollbackFixButton;
    private Label verificationLabel;
    private Label impactLabel;

    private Path lastFixBackup;
    private Path lastFixProjectRoot;
    private Path lastFixedSource;
    private String lastFixedFile;
    private String lastFixedRule;
    private int lastFixedLine = -1;

    // metrics + raw output
    private final ScanResultsPage resultsPage = new ScanResultsPage();
    private final MetricsPage metricsPage = new MetricsPage();
    private final SecurityReviewPage securityPage = new SecurityReviewPage();

    // class mode page
    private final ClassModeController classMode = new ClassModeController(
            () -> (Stage) pageHost.getScene().getWindow(),
            this::setStatus
    );

    // =========================================================
    // START
    // =========================================================
    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();

        rootPane = root;
        root.setStyle(GuiTheme.rootStyle(darkMode));

        root.setTop(createHeader(stage));
        root.setLeft(createSidebar());

        pageHost = new StackPane();

        root.setCenter(pageHost);

        homePage = new HomePage(() -> chooseProject(stage), recentProjects::list, this::openProject,
                this::navigate, recentProjects::remove);
        pages.put("Home", scroll(homePage.create()));

        pages.put(
                "Dashboard",
                scroll(dashboard.create())
        );

        analyzePage = new AnalyzePage(this::startAnalysis, () -> chooseProject(stage), () -> projectPath);
        pages.put("Analyze", scroll(analyzePage.create()));

        pages.put(
                "Scan Results",
                scroll(resultsPage.create())
        );

        pages.put(
                "Issues",
                scroll(createIssuesPage())
        );

        pages.put(
                "Security Review",
                scroll(securityPage.create())
        );

        pages.put(
                "Metrics",
                scroll(metricsPage.create())
        );

        pages.put(
                "Regression",
                scroll(regressionPage.create())
        );

        pages.put(
                "Quality History",
                scroll(historyPage.create())
        );

        pages.put(
                "Class Mode",
                scroll(classMode.createPage())
        );

        pages.put(
                "Fix Verification",
                scroll(FixVerificationPage.create())
        );

        pages.put(
                "Rules",
                scroll(RulesPage.create())
        );

        pages.put(
                "About",
                scroll(AboutPage.create())
        );

        showPage("Home");

        Scene scene
                = new Scene(
                        root,
                        1350,
                        850
                );

        applyStylesheet(scene);

        stage.setTitle(
                "CleanCheck \u2014 Code Quality Analysis"
        );

        stage.setScene(scene);

        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setMaximized(true);

        stage.show();
    }

    private void applyStylesheet(Scene scene) {
        java.net.URL css = CleanCheckGUI.class.getResource("cleancheck.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    private void toggleTheme(Button themeButton) {
        darkMode = !darkMode;
        rootPane.setStyle(GuiTheme.rootStyle(darkMode));
        themeButton.setText(darkMode ? "\u2600  Light" : "\u263E  Dark");
    }

    // =========================================================
    // HEADER
    // =========================================================
    private VBox createHeader(Stage stage) {

        // Figma top bar: logo + brand | project | actions
        CleanCheckLogo logo = new CleanCheckLogo(40);

        Label title = new Label("CleanCheck");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitle = new Label("Code Analysis Tool");
        subtitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        VBox titleBox = new VBox(0, title, subtitle);

        Region vline = new Region();
        vline.setPrefSize(1, 28);
        vline.setStyle("-fx-background-color: #334155;");

        if (projectLabel == null) {
            projectLabel = new Label(FOLDER + "No project selected");
        }
        projectLabel.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 13px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button themeButton = button("Theme", "#334155");
        themeButton.setOnAction(e -> toggleTheme(themeButton));

        Button chooseButton = button("Project", "#334155");
        chooseButton.setOnAction(e -> chooseProject(stage));

        Button reportButton = button("Report", "#7C3AED");
        reportButton.setOnAction(e -> exportHtmlReport(stage));

        startButton = button("Scan", "#2563EB");
        startButton.setOnAction(e -> startAnalysis());

        HBox topBar = new HBox(14, logo, titleBox, vline, projectLabel, spacer, themeButton, chooseButton, reportButton, startButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 24, 14, 24));
        topBar.setStyle(GuiTheme.headerStyle());

        if (statusLabel == null) {
            statusLabel = new Label();
        }
        setStatus("Ready", SUCCESS);

        Region strip = new Region();
        HBox.setHgrow(strip, Priority.ALWAYS);

        HBox statusStrip = new HBox(16, new Label(""), strip, statusLabel);
        // keep project path only in header for Figma look; status on right
        statusStrip.setAlignment(Pos.CENTER_LEFT);
        statusStrip.setPadding(new Insets(10, 24, 10, 24));
        statusStrip.setStyle(GuiTheme.statusStripStyle());
        statusLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        return new VBox(topBar, statusStrip);
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

    /** Saves a report under the project reports/YYYY-MM-DD folder (user can still change path in the dialog). */
    private void exportHtmlReport(Stage stage) {

        if (projectPath == null) {
            setStatus("Please select a project first.", WARNING);
            return;
        }

        String dateFolder = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Path reportsDir = projectPath.resolve("reports").resolve(dateFolder);
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException ex) {
            setStatus("Cannot create reports folder: " + ex.getMessage(), CRITICAL);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export report (default: project/reports/" + dateFolder + ")");
        chooser.setInitialDirectory(reportsDir.toFile());
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("HTML report (*.html)", "*.html"),
                new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"),
                new FileChooser.ExtensionFilter("SARIF for GitHub Code Scanning (*.sarif)", "*.sarif"),
                new FileChooser.ExtensionFilter("CSV for spreadsheets (*.csv)", "*.csv")
        );
        String name = projectPath.getFileName() == null
                ? "CleanCheck"
                : projectPath.getFileName().toString();
        chooser.setInitialFileName(name + "_Report.html");

        File target = chooser.showSaveDialog(stage);
        if (target == null) {
            return;
        }

        final Path root = projectPath;
        setStatus("Exporting report...", MUTED);

        Thread thread = new Thread(() -> {
            try {
                // Fresh read-only scan; the baseline comparison of the last Analyze run is attached
                // only if the code has not changed since (same findings), never stale numbers.
                AnalysisResult analysis = AppServices.getInstance().get(ProjectAnalyzer.class).analyze(root)
                        .withRegressionFrom(lastAnalysis);
                String written = ReportWriter.write(analysis, target.toPath());
                Platform.runLater(() -> setStatus(
                        "\u2714  Report exported: " + written, SUCCESS));
            } catch (IOException | RuntimeException ex) {
                Platform.runLater(() -> setStatus(
                        "Report export failed: " + ex.getMessage(), CRITICAL));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // =========================================================
    // SIDEBAR
    // =========================================================
    private VBox createSidebar() {

        String[][] items = {
            {"Home", "\u2302"},
            {"Dashboard", "\u25A3"},
            {"Analyze", "\u25B6"},
            {"Scan Results", "\u25A4"},
            {"Issues", "\u26A0"},
            {"Security Review", "\u25C8"},
            {"Metrics", "\u25A5"},
            {"Regression", "\u21C4"},
            {"Quality History", "\u231A"},
            {"Class Mode", "\u2637"},
            {"Fix Verification", "\u2714"},
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

        bar.setStyle(GuiTheme.sidebarStyle());

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
                    e -> navigate(item[0])
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
                        "CleanCheck  ·  Quality Lab"
                );

        version.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
        );

        Label powered
                = new Label(
                        "Offline  ·  AST  ·  Mentored Review"
                );

        powered.setStyle(
                "-fx-text-fill: " + MUTED + ";"
                + "-fx-font-size: 11px;"
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
        b.setStyle(selected ? GuiTheme.navSelected() : GuiTheme.navNormal());
    }

    /**
     * User-initiated navigation: clears the status line first, so a message about one page
     * (for example "No automatic fix for the selected issue.") does not follow the user around.
     */
    private void navigate(String name) {
        if (statusLabel != null) {
            setStatus("Ready", SUCCESS);
        }
        showPage(name);
    }

    private void showPage(String name) {
        // Prefer the exact nav key so pages like "Class Mode" are not lost.
        String candidate = name;
        Node page = pages.get(candidate);
        if (page == null) {
            PageId id = PageId.fromLabel(name);
            if (id != null) {
                candidate = id.label();
                page = pages.get(candidate);
            }
        }
        final String resolved = candidate;
        if (page != null) {
            pageHost.getChildren().setAll(page);
        }
        navButtons.forEach((key, b) -> styleNav(b, key.equals(resolved)));
        if (PageId.ANALYZE.label().equals(resolved) && analyzePage != null) {
            analyzePage.refresh();
        } else if (PageId.HOME.label().equals(resolved) && homePage != null) {
            homePage.refresh();
        }
    }

    /** Opens a project chosen from Home (recent list or drag and drop). */
    private void openProject(Path path) {
        if (fixRunning) {
            setStatus("Wait for the verified fix to finish before changing projects.", WARNING);
            return;
        }
        projectPath = path;
        updateRollbackAvailability();
        if (projectLabel != null) {
            projectLabel.setText(FOLDER + path);
        }
        clearIssueDetails();
        showPage("Dashboard");
        setStatus("Project ready: " + path.getFileName(), SUCCESS);
    }

    // =========================================================
    // DASHBOARD
    // =========================================================
    // =========================================================
    // ISSUES PAGE
    // =========================================================
    private VBox createIssuesPage() {

        severityFilter = new ComboBox<>();
        severityFilter.getItems().addAll("All", "CRITICAL", "WARNING", "INFO");
        severityFilter.setValue("All");
        severityFilter.setPrefWidth(120);
        severityFilter.setOnAction(e -> applyIssuesFilter());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Open", "Fixed", "All");
        statusFilter.setValue("Open");
        statusFilter.setPrefWidth(100);
        statusFilter.setOnAction(e -> applyIssuesFilter());

        ruleFilter = new ComboBox<>();
        ruleFilter.getItems().add("All Rules");
        ruleFilter.setValue("All Rules");
        ruleFilter.setPrefWidth(160);
        ruleFilter.setOnAction(e -> applyIssuesFilter());

        issueSearchField = new javafx.scene.control.TextField();
        issueSearchField.setPromptText("Search issues...");
        issueSearchField.setPrefWidth(180);
        issueSearchField.textProperty().addListener((obs, o, n) -> applyIssuesFilter());

        Label filterLabel = new Label("Severity");
        filterLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label statusLabelFilter = new Label("Status");
        statusLabelFilter.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label ruleLabel = new Label("Rule");
        ruleLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label searchLabel = new Label("Search");
        searchLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px; -fx-font-weight: bold;");

        HBox filterBox = new HBox(
                10,
                new VBox(4, filterLabel, severityFilter),
                new VBox(4, statusLabelFilter, statusFilter),
                new VBox(4, ruleLabel, ruleFilter),
                new VBox(4, searchLabel, issueSearchField)
        );
        filterBox.setAlignment(Pos.BOTTOM_LEFT);

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
                                "Click an issue to try a verified automatic fix; issues needing judgment stay manual."
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

        VBox leftColumn = new VBox(16);
        HBox body
                = new HBox(
                        18,
                        leftColumn,
                        createDetailsPanel()
                );
        HBox.setHgrow(leftColumn, Priority.ALWAYS);

        Label codeTitle
                = new Label(
                        "Code Review"
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
                        codeReview.node()
                );

        codeCard.setPadding(
                new Insets(6, 0, 0, 0)
        );

        codeCard.setStyle("-fx-background-color: transparent;");

        leftColumn.getChildren().addAll(issuesTable, codeCard);

        return IssuesPage.create(filterBox, body, null);
    }

    private void createIssuesTable() {

        issuesTable
                = new TableView<>();

        issuesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );
        issuesTable.setStyle(GuiTheme.tableStyle());
        issuesTable.setFixedCellSize(36);

        TableColumn<IssueRow, String> file
                = fileColumn(
                        "File",
                        190
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
                        90
                );

        TableColumn<IssueRow, String> status
                = column(
                        "Status",
                        "status",
                        80
                );

        TableColumn<IssueRow, String> message
                = column(
                        "Message",
                        "message",
                        280
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
                        status,
                        message
                );

        issuesTable.setRowFactory(table -> {
            TableRow<IssueRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    IssueRow selected = row.getItem();
                    issuesTable.getSelectionModel().select(selected);
                    showIssueDetails(selected);
                    // Never auto-fix on click. Fix only via Apply Verified Fix button.
                }
            });
            return row;
        });
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

        grid.addRow(
                4,
                key("Risk:"),
                detailRisk
        );

        grid.addRow(
                5,
                key("Confidence:"),
                detailConfidence
        );

        educationLabel.setWrapText(true);
        educationLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px;");
        Label learningTitle = sectionLabel("Learning Note");

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
                "-fx-control-inner-background: " + CARD + ";"
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

        rollbackFixButton = button(
                "↶  Rollback Last Fix",
                "#7C3AED"
        );
        rollbackFixButton.setMaxWidth(Double.MAX_VALUE);
        rollbackFixButton.setDisable(true);
        rollbackFixButton.setOnAction(e -> rollbackLastFix());

        impactLabel = new Label("Impact: Select an issue to calculate the expected change.");
        impactLabel.setWrapText(true);
        impactLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        verificationLabel = new Label("Verification: Waiting for a fix.");
        verificationLabel.setWrapText(true);
        verificationLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        VBox panel
                = new VBox(
                        14,
                        head,
                        grid,
                        new Separator(),
                        explanation.node(),
                        new Separator(),
                        impactLabel,
                        verificationLabel,
                        applyFixButton,
                        rollbackFixButton
                );

        panel.setPadding(
                new Insets(20)
        );

        panel.setPrefWidth(410);
        panel.setMinWidth(370);
        panel.setMaxWidth(450);

        panel.setStyle(
                cardStyle(
                        CARD,
                        BORDER
                )
        );

        return panel;
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
                fileLabel(issue.getFile())
        );
        detailFile.setTooltip(new javafx.scene.control.Tooltip(issue.getFile()));

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
        boolean explainFixed = "Fixed".equalsIgnoreCase(issue.getStatus());
        explanation.show(
                issue.getRule(),
                issue.getSeverity(),
                issue.getMessage(),
                issue.getSuggestion(),
                !explainFixed && FixEngine.supportsAutomaticFix(issue.getRule(), issue.getMessage()),
                explainFixed
        );

        detailRisk.setText(IssueAdvisor.riskLabel(issue.getSeverity()));
        detailConfidence.setText(IssueAdvisor.confidenceLabel(issue.getRule()));
        String learn = RuleEducation.forRule(issue.getRule());
        educationLabel.setText(learn);
        educationLabel.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 13px;"
                + "-fx-padding: 10;"
                + "-fx-background-color: " + GuiTheme.TINT_INFO + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + GuiTheme.TINT_INFO_BORDER + ";"
                + "-fx-border-radius: 8;"
        );
        impactLabel.setText(
                "Impact: 1 source file • line " + issue.getLine()
                + " • targeted rule: " + issue.getRule()
                + " • automatic fix may modify this file."
        );

        verificationLabel.setText(
                "Verification: After applying the fix, run Analyze to verify resolution and check for regressions."
        );
        verificationLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        boolean alreadyFixed = "Fixed".equalsIgnoreCase(issue.getStatus());
        boolean hasAutomaticFix = !alreadyFixed
                && FixEngine.supportsAutomaticFix(issue.getRule(), issue.getMessage());
        applyFixButton.setDisable(!hasAutomaticFix || analysisRunning || fixRunning);
        if (alreadyFixed) {
            applyFixButton.setText("Already Fixed");
            impactLabel.setText("This finding was already fixed and verified.");
            setStatus("Selected issue is already Fixed.", SUCCESS);
        } else if (hasAutomaticFix) {
            applyFixButton.setText("Apply Verified Fix");
            setStatus("Safe automatic fix available for this issue.", SUCCESS);
        } else {
            applyFixButton.setText("Manual Change Required");
            impactLabel.setText("This issue needs a behavior decision. CleanCheck will not change the code automatically.");
            setStatus("No automatic fix for the selected issue.", WARNING);
        }

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
        detailFile.setTooltip(null);
        detailLine.setText("-");
        detailRule.setText("-");
        detailSeverity.setText("-");
        detailRisk.setText("-");
        detailConfidence.setText("-");
        educationLabel.setText("Select a finding to see its learning note.");

        detailSeverity.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                + "-fx-font-size: 14px;"
        );

        suggestionArea.clear();
        explanation.clear();
        if (impactLabel != null) impactLabel.setText("Impact: Select an issue to calculate the expected change.");
        if (verificationLabel != null) verificationLabel.setText("Verification: Waiting for a fix.");

        codeReview.clear();

        applyFixButton.setDisable(true);
        applyFixButton.setText("Apply Verified Fix");
    }

    private void loadCodePreview(
            String fileName,
            String lineText
    ) {
        IssueRow focus = issuesTable == null ? null : issuesTable.getSelectionModel().getSelectedItem();
        if (focus != null && !(fileName.equals(focus.getFile()) && lineText.equals(focus.getLine()))) {
            focus = null;
        }
        codeReview.show(projectPath, fileName, lineText, focus, allIssues);
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
                    Alert.AlertType.INFORMATION,
                    "Please select an issue first."
            );

            return;
        }

        if (!FixEngine.supportsAutomaticFix(issue.getRule(), issue.getMessage())) {
            setStatus("This issue needs a manual code change.", WARNING);
            return;
        }

        if (projectPath == null) {

            showAlert(
                    "No Project",
                    Alert.AlertType.WARNING,
                    "Please select a project first."
            );

            return;
        }

        if (analysisRunning || fixRunning) {
            setStatus("Wait for the current project operation to finish.", WARNING);
            return;
        }

        Path analysisRoot = projectPath;
        String fileName = issue.getFile();
        String rule = issue.getRule();
        int parsedLine;
        try {
            parsedLine = Integer.parseInt(issue.getLine().trim());
        } catch (NumberFormatException ignored) {
            parsedLine = -1;
        }
        final int issueLine = parsedLine;

        fixRunning = true;
        applyFixButton.setDisable(true);
        startButton.setDisable(true);
        updateRollbackAvailability();
        setStatus("Applying and verifying the fix...", MUTED);

        Task<FixExecution> task = new Task<>() {
            @Override
            protected FixExecution call() throws Exception {
                Path javaFile = CodePreview.findJavaFile(analysisRoot, fileName);
                if (javaFile == null) {
                    throw new IOException("Could not find source file: " + fileName);
                }
                VerifiedFix.Outcome outcome = VerifiedFix.apply(
                        AppServices.getInstance().get(ProjectAnalyzer.class), analysisRoot, javaFile, rule, issueLine);
                return new FixExecution(javaFile, issueLine, outcome);
            }
        };

        task.setOnSucceeded(event -> {
            fixRunning = false;
            startButton.setDisable(false);
            updateRollbackAvailability();
            if (!analysisRoot.equals(projectPath)) {
                setStatus("The fix completed for the earlier project selection. Select it again to review the result.", WARNING);
                return;
            }
            finishVerifiedFix(issue, fileName, rule, task.getValue());
        });

        task.setOnFailed(event -> {
            fixRunning = false;
            startButton.setDisable(false);
            applyFixButton.setDisable(false);
            updateRollbackAvailability();
            showAlert("Fix Error", Alert.AlertType.ERROR,
                    task.getException() == null ? "Fix failed." : task.getException().getMessage());
            setStatus("Fix failed. The original source was restored when possible.", CRITICAL);
        });

        Thread worker = new Thread(task, "cleancheck-verified-fix");
        worker.setDaemon(true);
        worker.start();
    }

    private void finishVerifiedFix(IssueRow issue, String fileName, String rule, FixExecution execution) {
        VerifiedFix.Outcome fix = execution.outcome();
        int issueLine = execution.issueLine();
        if (fix.rolledBack()) {
            applyFixButton.setDisable(false);
            String reason = switch (fix.status()) {
                case ROLLED_BACK_COMPILE_ERRORS -> "The change would break compilation:\n  "
                        + String.join("\n  ", fix.newCompileErrors().stream().limit(5).toList());
                case ROLLED_BACK_NEW_ISSUES -> "The change introduced a new finding.";
                default -> "The target finding did not go away.";
            };
            showAlert("Fix Not Verified", Alert.AlertType.WARNING,
                    "The selected change was not safe, so CleanCheck restored the original file.\n\n" + reason);
            verificationLabel.setText("Verification: attempted change was rolled back. "
                    + reason.lines().findFirst().orElse(""));
            setStatus("Fix was not verified; original file restored.", WARNING);
            return;
        }

        if (fix.applied()) {
            lastFixBackup = fix.backup();
            lastFixProjectRoot = projectPath.toAbsolutePath().normalize();
            lastFixedSource = execution.javaFile().toAbsolutePath().normalize();
            lastFixedFile = fileName;
            lastFixedRule = rule;
            lastFixedLine = issueLine;
            updateRollbackAvailability();

            IssueRow fixedRow = issue.asFixed();
            resolvedIssues.removeIf(row -> row.getFile().equals(fixedRow.getFile())
                    && row.getLine().equals(fixedRow.getLine())
                    && row.getRule().equals(fixedRow.getRule()));
            resolvedIssues.add(0, fixedRow);
            issue.setStatus("Fixed");
            if (statusFilter != null) {
                statusFilter.setValue("All");
            }
            applyIssuesFilter();
            if (issuesTable != null) {
                issuesTable.refresh();
            }

            verificationLabel.setText(fix.compileChecked()
                    ? "Verification: Fix applied, no new findings and no new compile errors. Re-analyze to confirm."
                    : "Verification: Fix applied and marked as Fixed (compile check skipped: no JDK). Re-analyze to confirm.");
            loadCodePreview(fileName, issue.getLine());
            setStatus("Fix applied. Issue moved to Fixed. Re-analyzing...", SUCCESS);
            startAnalysis();
        } else {
            applyFixButton.setDisable(false);
            showAlert("No Automatic Fix", Alert.AlertType.INFORMATION,
                    "CleanCheck could not fix this issue automatically.\n\nRule: "
                    + rule + "\n\nReview the Suggested Fix manually.");
            setStatus("No automatic fix for the selected issue.", WARNING);
        }
    }

    private void rollbackLastFix() {
        if (analysisRunning || fixRunning) {
            setStatus("Wait for the current operation to finish before rolling back.", WARNING);
            return;
        }
        if (lastFixBackup == null || lastFixedSource == null || projectPath == null) {
            showAlert("Rollback", Alert.AlertType.WARNING, "No rollback point is available.");
            return;
        }
        if (!projectPath.toAbsolutePath().normalize().equals(lastFixProjectRoot)) {
            setStatus("Select the project where the last fix was applied before rolling it back.", WARNING);
            return;
        }

        try {
            if (!java.nio.file.Files.isRegularFile(lastFixedSource)) {
                showAlert("Rollback", Alert.AlertType.ERROR, "Original source file could not be found.");
                return;
            }

            Path rollbackPoint = lastFixBackup;
            Path rollbackProject = lastFixProjectRoot;
            FixVerificationService.restoreBackup(rollbackPoint, lastFixedSource);
            lastFixBackup = null;
            updateRollbackAvailability();
            verificationLabel.setText(
                    "Verification: Rollback completed. The original source was restored; run Analyze to confirm."
            );
            setStatus("Last fix rolled back successfully.", SUCCESS);
            loadCodePreview(lastFixedFile, String.valueOf(Math.max(1, lastFixedLine)));
            try {
                FixVerificationService.discardBackup(rollbackProject, rollbackPoint);
            } catch (IOException cleanupFailure) {
                setStatus("Rollback completed, but its temporary backup could not be removed: "
                        + cleanupFailure.getMessage(), WARNING);
            }
        } catch (Exception ex) {
            showAlert("Rollback Error", Alert.AlertType.ERROR, ex.getMessage());
            setStatus("Rollback failed.", CRITICAL);
        }
    }

    private void updateRollbackAvailability() {
        if (rollbackFixButton == null) {
            return;
        }
        rollbackFixButton.setDisable(!FixRollbackPolicy.canRollback(projectPath, lastFixProjectRoot,
                lastFixedSource, lastFixBackup, analysisRunning || fixRunning));
    }

    private void showAlert(
            String title,
            Alert.AlertType alertType,
            String message
    ) {

        Alert alert
                = new Alert(
                        alertType
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

        openProject(selected.toPath());
    }

    private void startAnalysis() {

        if (analysisRunning || fixRunning) {
            return;
        }

        if (projectPath == null) {
            setStatus("Please select a project first.", WARNING);
            return;
        }

        final Path analysisRoot = projectPath;
        analysisRunning = true;
        startButton.setDisable(true);
        updateRollbackAvailability();
        setStatus("Analysis is running...", MUTED);
        resultsPage.clear();
        allIssues.clear();
        dashboard.clear(metricsPage);
        securityPage.clear();
        clearIssueDetails();

        Thread thread = new Thread(() -> {

            long startTime = System.nanoTime();

            ByteArrayOutputStream output
                    = new ByteArrayOutputStream();

            final CleanCheck.Outcome[] outcome = new CleanCheck.Outcome[1];

            PrintStream captureOut
                    = new PrintStream(
                            output,
                            true,
                            StandardCharsets.UTF_8
                    );

            try {

                /*
             * =====================================================
             * ANALYZE ONLY
             * =====================================================
             *
             * Start Analysis must NOT modify source files.
             *
             * Auto Fix is disabled here.
             *
             * A single click on an issue attempts a verified fix only when
             * a narrow, deterministic transformation exists.
             * =====================================================
                 */
                outcome[0] = CleanCheck.execute(
                        new String[]{
                            analysisRoot.toString(),
                            "--analyze-only"
                        },
                        captureOut,
                        captureOut
                );

            } catch (Exception ex) {

                ex.printStackTrace(captureOut);

            } finally {

                captureOut.close();
            }

            double seconds
                    = (System.nanoTime() - startTime)
                    / 1_000_000_000.0;

            String result
                    = output.toString(
                            StandardCharsets.UTF_8
                    );

            lastAnalysis = outcome[0] == null ? null : outcome[0].analysis();

            Platform.runLater(() -> {
                try {
                    if (!analysisRoot.equals(projectPath)) {
                        setStatus("Selected project changed while analysis was running. Run the scan again.", WARNING);
                        return;
                    }

                    resultsPage.setText(result);
                    AnalysisResult completedAnalysis = outcome[0] == null ? null : outcome[0].analysis();
                    parseIssues(result, completedAnalysis);
                    verifyLastFixAfterAnalysis();

                    dashboard.update(
                            result,
                            seconds,
                            allIssues,
                            rootPane,
                            completedAnalysis == null ? 0 : completedAnalysis.javaFiles().size()
                    );

                    updateHotspots();
                    regressionPage.refresh();
                    historyPage.refresh();

                    if (!allIssues.isEmpty()) {
                        issuesTable.getSelectionModel().select(0);
                    }

                    if (outcome[0] == null || outcome[0].exitCode() != 0
                            || result.contains("ANALYSIS_STATUS : FAILED")) {
                        setStatus("Analysis failed. Review the error output.", CRITICAL);
                    } else {
                        setStatus("\u2714  Analysis completed", SUCCESS);
                        recentProjects.add(analysisRoot);
                    }
                } finally {
                    analysisRunning = false;
                    startButton.setDisable(false);
                    updateRollbackAvailability();
                }
            });

        });

        thread.setDaemon(true);
        thread.start();
    }

    // =========================================================
    // PARSE OUTPUT
    // =========================================================
    private void verifyLastFixAfterAnalysis() {
        if (lastFixedFile == null || lastFixedRule == null) return;

        boolean stillExists = false;
        for (IssueRow issue : allIssues) {
            int currentLine = -1;
            try {
                currentLine = Integer.parseInt(issue.getLine().trim());
            } catch (NumberFormatException ignored) {
                // keep unknown line
            }

            boolean sameTarget = lastFixedFile.equals(issue.getFile())
                    && lastFixedRule.equalsIgnoreCase(issue.getRule())
                    && (lastFixedLine < 0 || currentLine < 0
                        || Math.abs(lastFixedLine - currentLine) <= 3);

            if (sameTarget) {
                stillExists = true;
                break;
            }
        }

        if (stillExists) {
            verificationLabel.setText(
                    "Verification: FAILED — the target rule is still reported after re-analysis. Review the suggestion or rollback."
            );
            verificationLabel.setStyle("-fx-text-fill: " + CRITICAL + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            setStatus("Fix verification failed: target issue remains.", CRITICAL);
        } else {
            verificationLabel.setText(
                    "Verification: PASSED — the target issue is no longer reported after re-analysis. Review the issue count for regression safety."
            );
            verificationLabel.setStyle("-fx-text-fill: " + SUCCESS + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            setStatus("Fix verified successfully after re-analysis.", SUCCESS);
            rollbackFixButton.setDisable(false);
        }
    }

    private void parseIssues(
            String output,
            AnalysisResult analysis
    ) {
        allIssues.clear();
        // The analysis object is authoritative; parsing the printed text is only a fallback.
        List<IssueRow> parsed = new ArrayList<>();
        if (analysis != null) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (Warning w : analysis.warnings()) {
                IssueRow row = IssueRow.from(w);
                if (seen.add(row.getFile() + "|" + row.getLine() + "|" + row.getRule() + "|" + row.getMessage())) {
                    parsed.add(row);
                }
            }
        } else {
            parsed = IssueOutputParser.parse(output);
        }
        for (IssueRow row : parsed) {
            row.setStatus("Open");
            allIssues.add(row);
        }
        for (IssueRow fixed : resolvedIssues) {
            allIssues.add(fixed);
        }
        if (ruleFilter != null) {
            java.util.LinkedHashSet<String> rules = new java.util.LinkedHashSet<>();
            rules.add("All Rules");
            rules.add(SECURITY_ONLY);
            for (IssueRow row : allIssues) {
                rules.add(row.getRule());
            }
            String selectedRule = ruleFilter.getValue();
            ruleFilter.getItems().setAll(rules);
            if (selectedRule != null && rules.contains(selectedRule)) {
                ruleFilter.setValue(selectedRule);
            } else {
                ruleFilter.setValue("All Rules");
            }
        }
        applyIssuesFilter();
    }

    private void applyIssuesFilter() {
        if (filteredIssues == null) {
            return;
        }

        String severity = severityFilter == null ? "All" : severityFilter.getValue();
        String status = statusFilter == null ? "Open" : statusFilter.getValue();

        String rule = ruleFilter == null ? "All Rules" : ruleFilter.getValue();
        String q = issueSearchField == null || issueSearchField.getText() == null
                ? ""
                : issueSearchField.getText().trim().toLowerCase();

        filteredIssues.setPredicate(issue -> {
            boolean severityOk = severity == null
                    || severity.equals("All")
                    || issue.getSeverity().equalsIgnoreCase(severity);

            boolean statusOk = status == null
                    || status.equals("All")
                    || issue.getStatus().equalsIgnoreCase(status);

            boolean ruleOk = rule == null
                    || rule.equals("All Rules")
                    || (rule.equals(SECURITY_ONLY)
                            ? com.mycompany.cleancheck.security.SecurityCatalog.isSecurity(issue.getRule())
                            : issue.getRule().equalsIgnoreCase(rule));

            boolean searchOk = q.isEmpty()
                    || issue.getFile().toLowerCase().contains(q)
                    || issue.getRule().toLowerCase().contains(q)
                    || issue.getMessage().toLowerCase().contains(q)
                    || issue.getSuggestion().toLowerCase().contains(q);

            return severityOk && statusOk && ruleOk && searchOk;
        });
    }

    private void applySeverityFilter() {
        applyIssuesFilter();
    }

    // =========================================================
    // HOTSPOTS
    // =========================================================
    private void updateHotspots() {

        metricsPage.show(allIssues);
        securityPage.show(allIssues);
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

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(
            String[] args
    ) {

        launch(args);
    }
}
