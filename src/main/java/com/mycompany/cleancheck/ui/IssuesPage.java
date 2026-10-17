package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Figma-style Issues layout with filter bar + content. */
public final class IssuesPage {

    private IssuesPage() {
    }

    public static VBox create(Node filterBox, Node tableAndDetails, Node codeCard) {
        VBox filterCard = new VBox(filterBox);
        filterCard.setPadding(new Insets(14, 16, 14, 16));
        filterCard.setStyle(
                GuiTheme.cardStyle()
                        + "-fx-background-radius: 16;"
                        + "-fx-border-radius: 16;"
        );

        VBox tableCard = new VBox(tableAndDetails);
        tableCard.setPadding(new Insets(12));
        tableCard.setStyle(GuiTheme.cardStyle());
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("Issues", "Search, filter, learn, and apply safe verified fixes."),
                filterCard,
                tableCard
        );
        if (codeCard != null) {
            page.getChildren().add(codeCard);
        }
        return page;
    }

    public static VBox detailsCard(Node content) {
        VBox card = new VBox(content);
        card.setPadding(new Insets(18));
        card.setStyle(GuiTheme.cardStyle());
        return card;
    }
}
