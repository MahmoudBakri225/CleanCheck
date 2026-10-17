package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Visual shell for the issue details side panel. */
public final class DetailsPanelFactory {

    private DetailsPanelFactory() {
    }

    public static VBox shell(String titleText, Node body) {
        Label icon = new Label("i");
        icon.setMinSize(28, 28);
        icon.setAlignment(Pos.CENTER);
        icon.setStyle(
                "-fx-background-color: " + PRIMARY_SOFT + ";"
                        + "-fx-text-fill: " + PRIMARY + ";"
                        + "-fx-background-radius: 999;"
                        + "-fx-font-weight: bold;"
        );

        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        HBox head = new HBox(10, icon, title);
        head.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(16, head, body);
        card.setPadding(new Insets(20));
        card.setStyle(GuiTheme.cardStyle());
        return card;
    }
}
