package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;
import com.mycompany.cleancheck.database.repository.ScanSummary;
import com.mycompany.cleancheck.database.repository.ScanSummaryRepository;
import java.nio.file.Path;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shows the last stored scans of the selected project: a score badge per scan, coloured severity counts
 * and a short summary of the trend, in a table that fills the page.
 */
public final class QualityHistoryPage {

    private static final int MAX_ROWS = 10;
    private static final double ROW_HEIGHT = 46;
    private static final String[][] COLUMNS = {
        {"Scan", "scanId"}, {"Date", "date"}, {"Score", "score"},
        {"Issues", "issues"}, {"Critical", "critical"},
        {"Warnings", "warnings"}, {"Info", "info"}
    };

    private final Supplier<Path> projectPath;
    private final ScanSummaryRepository repository = new ScanSummaryRepository();
    private final TableView<HistoryRow> table = new TableView<>();
    private final Label summary = new Label();

    public QualityHistoryPage(Supplier<Path> projectPath) {
        this.projectPath = projectPath;
    }

    public VBox create() {
        for (String[] column : COLUMNS) {
            TableColumn<HistoryRow, String> col = new TableColumn<>(column[0]);
            col.setCellValueFactory(new PropertyValueFactory<>(column[1]));
            col.setSortable(false);
            switch (column[1]) {
                case "score" -> col.setCellFactory(c -> scoreCell());
                case "critical" -> col.setCellFactory(c -> countCell(CRITICAL));
                case "warnings" -> col.setCellFactory(c -> countCell(WARNING));
                case "info" -> col.setCellFactory(c -> countCell(INFO));
                default -> {
                }
            }
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(ROW_HEIGHT);
        table.setPlaceholder(emptyState("⌚", "No scans stored yet",
                "Select a project and press Scan. Every meaningful change is saved here."));
        // only as tall as its rows, so there are no empty striped rows below the data
        table.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(220.0, table.getItems().size() * ROW_HEIGHT + ROW_HEIGHT + 4),
                table.getItems()));
        table.setMinHeight(220);

        Button refreshButton = button("↻  Refresh History", PRIMARY);
        refreshButton.setOnAction(e -> refresh());

        summary.setStyle(muted(13));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(14, refreshButton, spacer, summary);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(table);
        card.setPadding(new Insets(10, 12, 12, 12));
        card.setStyle(GuiTheme.cardStyle());

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Quality History", "Meaningful quality changes saved for the selected project."),
                toolbar,
                card
        );
        refresh();
        return page;
    }

    public void refresh() {
        table.getItems().clear();
        summary.setText("");
        Path path = projectPath.get();
        if (path == null) {
            return;
        }
        try {
            int projectId = repository.findProjectId(path);
            if (projectId < 0) {
                return;
            }
            java.util.List<ScanSummary> scans = repository.findLatest(projectId, MAX_ROWS);
            for (ScanSummary scan : scans) {
                table.getItems().add(new HistoryRow(
                        scan.scanId(), scan.date(), scan.score(), scan.totalIssues(),
                        scan.criticalCount(), scan.warningCount(), scan.infoCount()));
            }
            summary.setText(describe(scans));
        } catch (Exception ex) {
            System.err.println("Unable to load quality history: " + ex.getMessage());
        }
    }

    /** One line about the trend: number of scans and the score change between the last two. */
    private static String describe(java.util.List<ScanSummary> newestFirst) {
        if (newestFirst.isEmpty()) {
            return "";
        }
        String count = newestFirst.size() + (newestFirst.size() == 1 ? " scan stored" : " scans stored");
        if (newestFirst.size() < 2) {
            return count;
        }
        int delta = newestFirst.get(0).score() - newestFirst.get(1).score();
        String change = delta == 0 ? "score unchanged"
                : (delta > 0 ? "score +" + delta : "score " + delta) + " since the previous scan";
        return count + "  ·  " + change;
    }

    private static TableCell<HistoryRow, String> scoreCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                int score = parseLeadingInt(item);
                String color = score >= 80 ? SUCCESS : score >= 50 ? WARNING : CRITICAL;
                setText(null);
                setGraphic(badge(item, "white", color));
            }
        };
    }

    private static TableCell<HistoryRow, String> countCell(String color) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                boolean zero = "0".equals(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " + (zero ? MUTED : color) + ";");
            }
        };
    }

    private static int parseLeadingInt(String text) {
        try {
            return Integer.parseInt(text.trim().split("\\s+")[0]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
