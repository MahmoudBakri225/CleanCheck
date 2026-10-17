package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Class Mode layout for a mentor: folder bar, class summary, ranking, repeated mistakes and project details. */
public final class ClassModePage {

    private ClassModePage() {
    }

    public static VBox create(
            Label folderLabel,
            Button choose,
            Button analyze,
            Button export,
            Node summary,
            Node ranking,
            Node common,
            Node detail
    ) {
        folderLabel.setStyle(muted(13));
        folderLabel.setWrapText(true);
        folderLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(folderLabel, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.NEVER);
        HBox bar = new HBox(12, folderLabel, choose, analyze, export);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 18, 16, 18));
        bar.setStyle(GuiTheme.cardStyle());

        VBox rankingCard = sectionCard("Class ranking",
                "Best score first. Click a student to see every finding.", ranking);
        VBox commonCard = sectionCard("Mistakes the class repeats",
                "Teach these first: they appear across the most projects.", common);
        var middle = split(60, 16, rankingCard, commonCard);

        VBox detailCard = new VBox(detail);
        detailCard.setPadding(new Insets(20));
        detailCard.setStyle(GuiTheme.cardStyle());

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Class Mode",
                        "Pick a folder with one subfolder per student project, analyze it, and compare."),
                bar, summary, middle, detailCard);
        return page;
    }
}
