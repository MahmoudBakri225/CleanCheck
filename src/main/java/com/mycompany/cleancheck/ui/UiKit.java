package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Shared JavaFX factory helpers used by every CleanCheck screen. */
public final class UiKit {

    private UiKit() {
    }

    public static VBox pageBox() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(28, 32, 32, 32));
        page.setStyle("-fx-background-color: " + BG + ";");
        return page;
    }

    public static VBox titleBlock(String title, String subtitle) {
        Label t = new Label(title);
        t.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                        + "-fx-font-size: 28px;"
                        + "-fx-font-weight: bold;"
        );
        Label s = new Label(subtitle);
        s.setWrapText(true);
        s.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 14px;");
        return new VBox(6, t, s);
    }

    public static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: " + TEXT + ";"
                        + "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
        );
        return l;
    }

    public static Label key(String text) {
        Label l = new Label(text);
        l.setMinWidth(78);
        l.setStyle(
                "-fx-text-fill: " + MUTED + ";"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
        );
        return l;
    }

    public static ScrollPane scroll(javafx.scene.Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    public static Button button(String text, String color) {
        Button b = new Button(text);
        if (PRIMARY.equals(color) || "#4F46E5".equals(color) || "#3B82F6".equals(color)
                || "#2563EB".equals(color)) {
            b.setStyle(GuiTheme.primaryButton());
        } else if (SUCCESS.equals(color) || "#16A34A".equals(color) || "#22C55E".equals(color)) {
            b.setStyle(GuiTheme.successButton());
        } else {
            b.setStyle(
                    "-fx-background-color: " + color + ";"
                            + "-fx-text-fill: white;"
                            + "-fx-font-weight: bold;"
                            + "-fx-font-size: 13px;"
                            + "-fx-background-radius: 12;"
                            + "-fx-padding: 11 18 11 18;"
                            + "-fx-cursor: hand;"
            );
        }
        return b;
    }

    public static Label circle(String text, String bg, String fg, double size) {
        Label l = new Label(text);
        l.setMinSize(size, size);
        l.setPrefSize(size, size);
        l.setMaxSize(size, size);
        l.setAlignment(Pos.CENTER);
        l.setStyle(
                "-fx-background-color: " + bg + ";"
                        + "-fx-background-radius: " + size + ";"
                        + "-fx-text-fill: " + fg + ";"
                        + "-fx-font-size: " + (int) (size * 0.4) + "px;"
                        + "-fx-font-weight: bold;"
        );
        return l;
    }

    public static String cardStyle(String bg, String border) {
        if (CARD.equals(bg) && BORDER.equals(border)) {
            return GuiTheme.cardStyle();
        }
        return "-fx-background-color: " + bg + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 18, 0, 0, 6);";
    }

    public static <S> TableColumn<S, String> column(String title, String property, double width) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setPrefWidth(width);
        return col;
    }

    // ---------------------------------------------------------------------
    // Shared building blocks used by the redesigned screens
    // ---------------------------------------------------------------------

    /** A wrapping label that is never cut off with an ellipsis (JavaFX needs USE_PREF_SIZE for that). */
    public static Label wrapped(String text, String style) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle(style);
        return l;
    }

    public static String muted(int px) {
        return "-fx-text-fill: " + MUTED + "; -fx-font-size: " + px + "px;";
    }

    public static String strong(int px) {
        return "-fx-text-fill: " + TEXT + "; -fx-font-size: " + px + "px; -fx-font-weight: bold;";
    }

    /** Small rounded pill. */
    public static Label badge(String text, String textColor, String background) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: " + textColor + ";"
                        + "-fx-background-color: " + background + ";"
                        + "-fx-background-radius: 999;"
                        + "-fx-padding: 3 10 3 10;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
        );
        return l;
    }

    public static String severityColor(String severity) {
        if (severity == null) {
            return MUTED;
        }
        return switch (severity.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "CRITICAL" -> CRITICAL;
            case "WARNING" -> WARNING;
            case "INFO" -> INFO;
            default -> MUTED;
        };
    }

    public static Label severityBadge(String severity) {
        String name = severity == null ? "-" : severity.trim().toUpperCase(java.util.Locale.ROOT);
        return badge(name, "white", severityColor(severity));
    }

    /** Big number with a coloured top edge, a small title above and a caption below. */
    public static VBox statCard(String title, String value, String caption, String accent) {
        Region top = new Region();
        top.setPrefHeight(4);
        top.setMinHeight(4);
        top.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 4;");

        Label t = new Label(title.toUpperCase(java.util.Locale.ROOT));
        t.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 32px; -fx-font-weight: bold;");

        VBox box = new VBox(6, top, t, v);
        if (caption != null && !caption.isEmpty()) {
            box.getChildren().add(wrapped(caption, muted(12)));
        }
        box.setPadding(new Insets(16));
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle(GuiTheme.cardStyle());
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /**
     * Equal-width row of cards. A GridPane with percentage columns is used on purpose: with an HBox the
     * wrapped labels inside the cards are measured at the wrong width and the cards become very tall.
     */
    public static GridPane cardRow(Node... cards) {
        double[] percents = new double[cards.length];
        java.util.Arrays.fill(percents, 100.0 / Math.max(1, cards.length));
        return columns(16, percents, cards);
    }

    /** Two columns with the given percentage for the left one. */
    public static GridPane split(double leftPercent, double gap, Node left, Node right) {
        return columns(gap, new double[]{leftPercent, 100.0 - leftPercent}, left, right);
    }

    private static GridPane columns(double gap, double[] percents, Node... nodes) {
        GridPane grid = new GridPane();
        grid.setHgap(gap);
        for (int i = 0; i < nodes.length; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(percents[i]);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
            if (nodes[i] instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
            grid.add(nodes[i], i, 0);
            GridPane.setFillWidth(nodes[i], true);
            GridPane.setFillHeight(nodes[i], true);
        }
        return grid;
    }

    /** Card with a heading, an optional one-line explanation and any content. */
    public static VBox sectionCard(String title, String subtitle, Node content) {
        VBox box = new VBox(12);
        box.getChildren().add(sectionLabel(title));
        if (subtitle != null && !subtitle.isEmpty()) {
            box.getChildren().add(wrapped(subtitle, muted(13)));
        }
        box.getChildren().add(content);
        box.setPadding(new Insets(20));
        box.setStyle(GuiTheme.cardStyle());
        return box;
    }

    /** "label ......... [bar] 7" row. */
    public static HBox barRow(String label, double fraction, String color, String rightText, double labelWidth) {
        Label name = new Label(label);
        name.setMinWidth(labelWidth);
        name.setPrefWidth(labelWidth);
        name.setStyle(strong(13));

        ProgressBar bar = new ProgressBar(Math.max(0.0, Math.min(1.0, fraction)));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(10);
        bar.setStyle("-fx-accent: " + color + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label right = new Label(rightText);
        right.setMinWidth(Region.USE_PREF_SIZE);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox row = new HBox(12, name, bar, right);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static VBox emptyState(String icon, String title, String text) {
        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 34px; -fx-text-fill: " + MUTED + ";");
        Label t = new Label(title);
        t.setStyle(strong(16));
        VBox box = new VBox(8, i, t, wrapped(text, muted(13) + "-fx-text-alignment: center;"));
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        return box;
    }

    /** Secondary (outlined) button. */
    public static Button ghostButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: " + PRIMARY + ";"
                        + "-fx-font-weight: bold;"
                        + "-fx-font-size: 13px;"
                        + "-fx-border-color: " + PRIMARY + ";"
                        + "-fx-border-radius: 12;"
                        + "-fx-background-radius: 12;"
                        + "-fx-padding: 10 18 10 18;"
                        + "-fx-cursor: hand;"
        );
        return b;
    }

    /** Shortens a long path for display, keeping the end (the part people recognise). */
    /** Folder and file name only ("app/UserService.java"): the part that tells two files apart. */
    public static String fileLabel(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        String[] parts = normalized.split("/");
        int n = parts.length;
        return n >= 2 ? parts[n - 2] + "/" + parts[n - 1] : normalized;
    }

    /** File column that shows "folder/File.java" and the full path in a tooltip. */
    public static TableColumn<IssueRow, String> fileColumn(String title, double width) {
        TableColumn<IssueRow, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFile()));
        col.setCellFactory(c -> new javafx.scene.control.TableCell<IssueRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(fileLabel(item));
                    setTooltip(new javafx.scene.control.Tooltip(item));
                }
            }
        });
        col.setPrefWidth(width);
        return col;
    }

    public static String shortPath(String path, int max) {
        if (path == null || path.length() <= max) {
            return path == null ? "" : path;
        }
        return "\u2026" + path.substring(path.length() - (max - 1));
    }
}
