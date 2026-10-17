package com.mycompany.cleancheck.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * CleanCheck's signature emblem for the hero: the {@link CleanCheckLogo} badge ({ } + check) inside the
 * "time-travel" ring, a gradient arc with an arrow head that slowly turns <b>backwards</b> (rewind = every fix
 * can be undone), an outer orbit with the four colours of the tool (critical, warning, info, fixed) and three
 * floating chips for the safety steps (Backup, Compile OK, Rollback).
 * <p>
 * Pure shapes, no data, so it is correct before any project is chosen. Animations run on the JavaFX
 * Application Thread and are stopped whenever the node leaves the scene.
 */
final class CleanCheckEmblem extends VBox {

    private static final double S = 400;
    private static final double C = S / 2;

    private final Rotate orbitSpin = new Rotate(0, C, C);
    private final Rotate rewindSpin = new Rotate(0, C, C);
    private final List<Animation> animations = new ArrayList<>();

    CleanCheckEmblem() {
        super(6);
        setAlignment(Pos.CENTER);

        Pane stage = new Pane();
        stage.setMinSize(S, S);
        stage.setPrefSize(S, S);
        stage.setMaxSize(S, S);

        Circle glow = new Circle(C, C, 135);
        glow.setFill(new RadialGradient(0, 0, C, C, 135, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(52, 211, 153, 0.38)), new Stop(1, Color.rgb(52, 211, 153, 0))));

        Circle inner = new Circle(C, C, 140);
        inner.setFill(Color.TRANSPARENT);
        inner.setStroke(Color.rgb(255, 255, 255, 0.12));
        inner.setStrokeWidth(1.5);

        // outer orbit with the four tool colours
        Circle ring = new Circle(C, C, 188);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(255, 255, 255, 0.24));
        ring.setStrokeWidth(1.5);
        ring.getStrokeDashArray().addAll(3.0, 9.0);
        Group orbit = new Group(ring,
                dot(-25, GuiTheme.CRITICAL), dot(65, GuiTheme.WARNING),
                dot(155, GuiTheme.INFO), dot(245, GuiTheme.SUCCESS));
        orbit.getTransforms().add(orbitSpin);

        // the rewind arc: counter-clockwise, ends in an arrow head
        double radius = 160;
        double startDeg = 40;
        double lengthDeg = 260;
        Arc arc = new Arc(C, C, radius, radius, startDeg, lengthDeg);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, CleanCheckLogo.NEON_CYAN), new Stop(1, CleanCheckLogo.EMERALD_LIGHT)));
        arc.setStrokeWidth(7);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setEffect(new DropShadow(16, Color.rgb(34, 211, 238, 0.55)));

        double end = Math.toRadians(startDeg + lengthDeg);
        double ex = C + radius * Math.cos(end);
        double ey = C - radius * Math.sin(end);
        double tx = -Math.sin(end);   // direction of travel at the end of the arc (counter-clockwise)
        double ty = -Math.cos(end);
        double nx = -ty;
        double ny = tx;
        Polygon head = new Polygon(ex + tx * 18, ey + ty * 18, ex + nx * 11, ey + ny * 11, ex - nx * 11, ey - ny * 11);
        head.setFill(CleanCheckLogo.EMERALD_LIGHT);
        Group rewind = new Group(arc, head);
        rewind.getTransforms().add(rewindSpin);

        // the badge in the middle
        CleanCheckLogo logo = new CleanCheckLogo(190);
        logo.setEffect(new DropShadow(40, 0, 16, Color.rgb(6, 182, 212, 0.55)));
        logo.setLayoutX(C - 95);
        logo.setLayoutY(C - 95);

        stage.getChildren().addAll(glow, inner, orbit, rewind, logo);

        Label backup = chip("\u2714  Backup", 6, 70);
        Label compile = chip("\u2714  Compile OK", 262, 318);
        Label rollback = chip("\u21A9  Rollback", 4, 296);
        stage.getChildren().addAll(backup, compile, rollback);

        // animations (started when the node is on screen)
        animations.add(spin(orbitSpin, 360, 90));
        animations.add(spin(rewindSpin, -360, 36));
        animations.add(bob(logo, -10, 3.2));
        animations.add(bob(backup, -6, 2.6));
        animations.add(bob(compile, 6, 3.0));
        animations.add(bob(rollback, -5, 2.9));

        Label word = new Label("CleanCheck");
        word.setStyle("-fx-text-fill: white; -fx-font-size: 30px; -fx-font-weight: bold;");
        Label tagline = new Label("TIME-TRAVEL SAFE FIXES");
        tagline.setStyle("-fx-text-fill: #A7F3D0; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox wordmark = new VBox(2, word, tagline);
        wordmark.setAlignment(Pos.CENTER);

        getChildren().addAll(stage, wordmark);

        sceneProperty().addListener((obs, old, scene) -> {
            for (Animation a : animations) {
                if (scene == null) {
                    a.stop();
                } else {
                    a.play();
                }
            }
        });
    }

    private static Circle dot(double degrees, String color) {
        double angle = Math.toRadians(degrees);
        Circle d = new Circle(C + 188 * Math.cos(angle), C + 188 * Math.sin(angle), 8, Color.web(color));
        d.setStroke(Color.rgb(255, 255, 255, 0.85));
        d.setStrokeWidth(2);
        d.setEffect(new DropShadow(14, Color.web(color, 0.8)));
        return d;
    }

    private static Label chip(String text, double x, double y) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-background-color: rgba(8,15,40,0.62); -fx-background-radius: 999;"
                + "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 999; -fx-padding: 6 14 6 14;");
        l.setLayoutX(x);
        l.setLayoutY(y);
        return l;
    }

    private static Timeline spin(Rotate rotate, double degrees, double seconds) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(rotate.angleProperty(), 0)),
                new KeyFrame(Duration.seconds(seconds), new KeyValue(rotate.angleProperty(), degrees, Interpolator.LINEAR)));
        t.setCycleCount(Animation.INDEFINITE);
        return t;
    }

    private static TranslateTransition bob(Node node, double dy, double seconds) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(seconds), node);
        t.setByY(dy);
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        return t;
    }
}
