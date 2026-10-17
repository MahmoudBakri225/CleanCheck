package com.mycompany.cleancheck.ui;

import static com.mycompany.cleancheck.ui.GuiTheme.*;
import static com.mycompany.cleancheck.ui.UiKit.*;

import javafx.scene.layout.VBox;

/** About screen: what the tool is, how it differs from the alternatives, and the stack. */
public final class AboutPage {

    private AboutPage() {
    }

    public static VBox create() {
        String body = "-fx-text-fill: " + TEXT + "; -fx-font-size: 14px;";
        VBox page = pageBox();
        page.getChildren().addAll(
                titleBlock("About", "What CleanCheck is, in thirty seconds."),
                sectionCard("What it does", null, wrapped(
                        "CleanCheck is an offline Java quality tool for students and mentors. It analyzes a whole project "
                                + "with stable rules, explains the principle behind each issue, applies only safe verified fixes "
                                + "and tracks quality over time.", body)),
                sectionCard("Why not SonarQube or a chatbot?", null, wrapped(
                        "SonarQube is an enterprise platform; CleanCheck is lightweight, local and educational. "
                                + "A chatbot helps once; CleanCheck repeats the same full-project analysis, keeps history and "
                                + "fixes locally without uploading your source code.", body)),
                sectionCard("Under the hood", null, wrapped(
                        "Java 26  \u00B7  JavaFX  \u00B7  JavaParser  \u00B7  SQLite\n"
                                + "Workflow: Analyze \u2192 Explain \u2192 Fix \u2192 Track\n"
                                + RuleKnowledge.all().size() + " rules, "
                                + RuleKnowledge.autoFixRules().size() + " with verified automatic fixes\n"
                                + "Version: CleanCheck v11", muted(13))));
        return page;
    }
}
