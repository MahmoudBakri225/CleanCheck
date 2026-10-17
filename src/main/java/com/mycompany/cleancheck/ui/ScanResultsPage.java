package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Raw analyzer output of the latest scan, shown like a terminal window. */
public final class ScanResultsPage {

    private static final String CONSOLE_BG = "#0B1220";

    private final TextArea outputArea = new TextArea();

    public VBox create() {
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(28);
        outputArea.setPromptText("Run a scan to see the analyzer output here.");
        outputArea.setStyle(
                "-fx-control-inner-background: " + CONSOLE_BG + ";"
                        + "-fx-background-color: " + CONSOLE_BG + ";"
                        + "-fx-border-color: transparent;"
                        + "-fx-background-insets: 0;"
                        + "-fx-background-radius: 0 0 18 18;"
                        + "-fx-padding: 6 10 10 10;"
                        + "-fx-text-fill: #E5E7EB;"
                        + "-fx-highlight-fill: #1D4ED8;"
                        + "-fx-font-family: 'Consolas', 'Monospaced';"
                        + "-fx-font-size: 12.5px;"
        );
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        // terminal title bar: three dots, a name and a copy button
        HBox dots = new HBox(7, dot("#EF4444"), dot("#F59E0B"), dot("#22C55E"));
        dots.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label("analyzer output");
        name.setStyle("-fx-text-fill: #94A3B8; -fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button copy = new Button("Copy");
        copy.setStyle("-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: #E2E8F0;"
                + "-fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 8;"
                + "-fx-padding: 5 14 5 14; -fx-cursor: hand;");
        copy.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(outputArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });
        HBox bar = new HBox(14, dots, name, spacer, copy);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 16));
        bar.setStyle("-fx-background-color: #111A2E; -fx-background-radius: 18 18 0 0;");

        VBox console = new VBox(bar, outputArea);
        console.setStyle("-fx-background-color: " + CONSOLE_BG + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #1E293B; -fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.25), 24, 0, 0, 8);");
        VBox.setVgrow(console, Priority.ALWAYS);

        Label hint = new Label("Technical output used for verification and committee transparency.");
        hint.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Scan Results", "Raw analyzer output of the latest scan."),
                hint,
                console
        );
        return page;
    }

    private static Circle dot(String color) {
        return new Circle(5, Color.web(color));
    }

    public void setText(String text) {
        outputArea.setText(text == null ? "" : text);
    }

    public void clear() {
        outputArea.clear();
    }
}
