package com.mycompany.cleancheck.ui;

import javafx.scene.control.Label;

/**
 * Small helper for the status line shown at the bottom of the main window.
 * Extracted so status updates are not duplicated across the large GUI class.
 */
public final class GuiStatus {

    public static final String SUCCESS = "success";
    public static final String WARNING = "warning";
    public static final String ERROR = "error";
    public static final String INFO = "info";

    private final Label label;

    public GuiStatus(Label label) {
        this.label = label;
    }

    public void set(String message, String level) {
        if (label == null) {
            return;
        }
        label.setText(message == null ? "" : message);
        String color = switch (level == null ? INFO : level) {
            case SUCCESS -> GuiTheme.SUCCESS;
            case WARNING -> GuiTheme.WARNING;
            case ERROR -> GuiTheme.CRITICAL;
            default -> GuiTheme.MUTED;
        };
        label.setStyle("-fx-text-fill: " + color + ";");
    }

    public void ready() {
        set("Ready", SUCCESS);
    }
}
