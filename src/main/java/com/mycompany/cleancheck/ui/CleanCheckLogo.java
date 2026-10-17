package com.mycompany.cleancheck.ui;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * The CleanCheck mark: code braces { } around a check disc, in a gradient from neon tech blue to emerald green.
 * <p>
 * Everything is drawn as vectors (no font, no image), so it stays sharp at any size and on HiDPI screens.
 * Use it anywhere a {@link javafx.scene.Node} fits:
 * <pre>
 *   new CleanCheckLogo(40)                 // rounded gradient badge (header, window icon)
 *   new CleanCheckLogo(120, false)         // glyph only, for dark backgrounds
 *   CleanCheckLogo.lockup(44, "white", "#94A3B8")   // logo + "CleanCheck" wordmark
 * </pre>
 */
public final class CleanCheckLogo extends Canvas {

    public static final Color TECH_BLUE = Color.web("#2F6BFF");
    public static final Color NEON_CYAN = Color.web("#22D3EE");
    public static final Color EMERALD = Color.web("#10B981");
    public static final Color EMERALD_LIGHT = Color.web("#6EE7B7");

    private final double size;
    private final boolean plate;

    /** Badge version: rounded gradient plate with white braces. */
    public CleanCheckLogo(double size) {
        this(size, true);
    }

    /**
     * @param plate true = rounded gradient plate with white braces; false = no plate, braces in the gradient
     *              (meant for dark backgrounds)
     */
    public CleanCheckLogo(double size, boolean plate) {
        super(size, size);
        this.size = size;
        this.plate = plate;
        draw();
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double s = size;
        g.clearRect(0, 0, s, s);

        if (plate) {
            double arc = s * 0.30;
            g.setFill(new LinearGradient(0, 0, s, s, false, CycleMethod.NO_CYCLE,
                    new Stop(0.00, TECH_BLUE),
                    new Stop(0.55, Color.web("#12B5CB")),
                    new Stop(1.00, EMERALD)));
            g.fillRoundRect(0, 0, s, s, arc, arc);

            // soft gloss on the upper half
            g.setFill(new LinearGradient(0, 0, 0, s * 0.55, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 255, 255, 0.30)),
                    new Stop(1, Color.rgb(255, 255, 255, 0.0))));
            g.fillRoundRect(0, 0, s, s * 0.55, arc, arc);

            g.setStroke(Color.rgb(255, 255, 255, 0.35));
            g.setLineWidth(Math.max(1, s * 0.012));
            g.strokeRoundRect(0.5, 0.5, s - 1, s - 1, arc, arc);
        }

        Paint ink = plate
                ? Color.WHITE
                : new LinearGradient(s * 0.1, 0, s * 0.9, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, NEON_CYAN), new Stop(1, EMERALD_LIGHT));
        g.setStroke(ink);
        g.setLineWidth(s * 0.07);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        brace(g, s * 0.13, s * 0.27, s * 0.17, s * 0.46, false);
        brace(g, s * 0.70, s * 0.27, s * 0.17, s * 0.46, true);

        // check disc in the middle
        double cx = s / 2;
        double cy = s / 2;
        double r = s * 0.185;
        g.setEffect(new DropShadow(s * 0.12, 0, s * 0.03, Color.rgb(4, 120, 87, 0.55)));
        g.setFill(new LinearGradient(cx - r, cy - r, cx + r, cy + r, false, CycleMethod.NO_CYCLE,
                new Stop(0, EMERALD_LIGHT), new Stop(1, Color.web("#059669"))));
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
        g.setEffect(null);

        g.setStroke(Color.WHITE);
        g.setLineWidth(s * 0.055);
        g.beginPath();
        g.moveTo(cx - r * 0.48, cy + r * 0.02);
        g.lineTo(cx - r * 0.12, cy + r * 0.40);
        g.lineTo(cx + r * 0.52, cy - r * 0.36);
        g.stroke();
    }

    /** One curly brace as a bezier path; {@code mirrored} turns "{" into "}". */
    private static void brace(GraphicsContext g, double x, double top, double w, double h, boolean mirrored) {
        double bottom = top + h;
        double mid = top + h / 2;
        g.beginPath();
        g.moveTo(px(x, w, 1.00, mirrored), top);
        g.bezierCurveTo(px(x, w, 0.45, mirrored), top,
                px(x, w, 0.45, mirrored), top + h * 0.10,
                px(x, w, 0.45, mirrored), top + h * 0.22);
        g.lineTo(px(x, w, 0.45, mirrored), mid - h * 0.12);
        g.bezierCurveTo(px(x, w, 0.45, mirrored), mid - h * 0.04,
                px(x, w, 0.25, mirrored), mid,
                px(x, w, 0.00, mirrored), mid);
        g.bezierCurveTo(px(x, w, 0.25, mirrored), mid,
                px(x, w, 0.45, mirrored), mid + h * 0.04,
                px(x, w, 0.45, mirrored), mid + h * 0.12);
        g.lineTo(px(x, w, 0.45, mirrored), bottom - h * 0.22);
        g.bezierCurveTo(px(x, w, 0.45, mirrored), bottom - h * 0.10,
                px(x, w, 0.45, mirrored), bottom,
                px(x, w, 1.00, mirrored), bottom);
        g.stroke();
    }

    private static double px(double x, double w, double t, boolean mirrored) {
        return mirrored ? x + w * (1 - t) : x + w * t;
    }

    /** Logo + wordmark + small tagline, e.g. for the header or an About page. */
    public static HBox lockup(double logoSize, String titleColor, String taglineColor) {
        Label title = new Label("CleanCheck");
        title.setStyle("-fx-text-fill: " + titleColor + "; -fx-font-size: " + Math.round(logoSize * 0.42)
                + "px; -fx-font-weight: bold;");
        Label tagline = new Label("Code Analysis Tool");
        tagline.setStyle("-fx-text-fill: " + taglineColor + "; -fx-font-size: " + Math.round(logoSize * 0.26) + "px;");
        VBox words = new VBox(0, title, tagline);
        words.setAlignment(Pos.CENTER_LEFT);
        HBox box = new HBox(12, new CleanCheckLogo(logoSize), words);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
