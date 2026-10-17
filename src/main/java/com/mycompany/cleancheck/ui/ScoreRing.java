package com.mycompany.cleancheck.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Full-circle quality gauge. Arcs use explicit center coordinates inside a Group
 * so the stroke renders as one continuous ring (not broken end-caps).
 */
final class ScoreRing extends VBox {

    private static final double R = 70;
    private static final double STROKE = 12;
    private static final double CX = R + STROKE;
    private static final double CY = R + STROKE;

    private final Arc track = new Arc();
    private final Arc value = new Arc();
    private final Text scoreText = new Text("—");
    private final Text outOf = new Text("out of 100");
    private final Label pill = new Label("No scan");

    ScoreRing() {
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setMinSize(190, 220);
        setPrefSize(200, 230);

        setup(track, Color.web("#E5EAF2"), -360);
        setup(value, Color.web("#E5EAF2"), 0);

        scoreText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 44));
        scoreText.setFill(Color.web("#0F172A"));
        scoreText.setTextAlignment(TextAlignment.CENTER);

        outOf.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        outOf.setFill(Color.web("#94A3B8"));
        outOf.setTextAlignment(TextAlignment.CENTER);

        VBox labels = new VBox(2, scoreText, outOf);
        labels.setAlignment(Pos.CENTER);

        Group arcs = new Group(track, value);
        double box = 2 * (R + STROKE);
        StackPane ring = new StackPane(arcs, labels);
        ring.setMinSize(box, box);
        ring.setPrefSize(box, box);
        ring.setMaxSize(box, box);

        pill.setStyle(pillStyle("#F1F5F9", "#94A3B8"));
        getChildren().addAll(ring, pill);
    }

    private static void setup(Arc arc, Color stroke, double length) {
        arc.setCenterX(CX);
        arc.setCenterY(CY);
        arc.setRadiusX(R);
        arc.setRadiusY(R);
        arc.setStartAngle(90);
        arc.setLength(length);
        arc.setType(ArcType.OPEN);
        arc.setStroke(stroke);
        arc.setStrokeWidth(STROKE);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setFill(null);
    }

    private static String pillStyle(String bg, String fg) {
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                + "-fx-background-radius: 999; -fx-padding: 5 16 5 16;"
                + "-fx-font-size: 12px; -fx-font-weight: bold;";
    }

    void reset() {
        scoreText.setText("—");
        value.setLength(0);
        value.setStroke(Color.web("#E8EEF5"));
        scoreText.setFill(Color.web("#94A3B8"));
        pill.setText("No scan");
        pill.setStyle(pillStyle("#F1F5F9", "#94A3B8"));
    }

    void setScore(int score) {
        int s = Math.max(0, Math.min(100, score));
        scoreText.setText(String.valueOf(s));
        value.setLength(-360.0 * (s / 100.0));
        scoreText.setFill(Color.web("#0F172A"));

        String label;
        String stroke;
        String bg;
        String fg;
        if (s >= 90) {
            label = "Excellent";
            stroke = "#10B981";
            bg = "#D1FAE5";
            fg = "#047857";
        } else if (s >= 75) {
            label = "Good";
            stroke = "#3B82F6";
            bg = "#DBEAFE";
            fg = "#1D4ED8";
        } else if (s >= 50) {
            label = "Fair";
            stroke = "#F59E0B";
            bg = "#FEF3C7";
            fg = "#D97706";
        } else {
            label = "Needs work";
            stroke = "#EF4444";
            bg = "#FEE2E2";
            fg = "#DC2626";
        }
        value.setStroke(Color.web(stroke));
        pill.setText(label);
        pill.setStyle(pillStyle(bg, fg));
    }
}
