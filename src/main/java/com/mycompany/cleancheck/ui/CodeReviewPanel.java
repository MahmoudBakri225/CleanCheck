package com.mycompany.cleancheck.ui;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Code review under the issues table: syntax view, annotation, and an edit mode
 * that can write the full file back into the project.
 */
final class CodeReviewPanel {

    private static final String FONT = "-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;";
    private static final String EDITOR_BG = "#0F172A";
    private static final int WHOLE_FILE_HEIGHT = 460;
    private static final String CRITICAL = "#EF4444";
    private static final String WARNING = "#F59E0B";
    private static final String INFO = "#3B82F6";

    private final VBox root = new VBox(10);

    private boolean wide;
    private boolean editing;
    private Path lastProject;
    private String lastFile;
    private String lastLine;
    private IssueRow lastFocus;
    private List<IssueRow> lastAll = List.of();
    private Path resolvedFile;

    CodeReviewPanel() {
        clear();
    }

    VBox node() {
        return root;
    }

    void clear() {
        lastProject = null;
        lastFile = null;
        resolvedFile = null;
        editing = false;
        root.getChildren().setAll(wrapped("Select an issue in the table to review the code around it.", muted(13)));
    }

    void show(Path project, String fileName, String lineText, IssueRow focus, List<IssueRow> all) {
        lastProject = project;
        lastFile = fileName;
        lastLine = lineText;
        lastFocus = focus;
        lastAll = all == null ? List.of() : all;
        editing = false;
        render();
    }

    private void render() {
        CodeSnippet snippet = CodeSnippet.load(lastProject, lastFile, lastLine, wide);
        if (!snippet.ok()) {
            root.getChildren().setAll(wrapped(snippet.error(), muted(13)));
            return;
        }
        resolvedFile = resolvePath(lastProject, lastFile);

        Label file = new Label(snippet.fileName());
        file.setStyle(strong(14));
        Label where = new Label("line " + snippet.focusLine() + " of " + snippet.totalLines());
        where.setStyle(muted(12));
        HBox header = new HBox(10, file, where);
        header.setAlignment(Pos.CENTER_LEFT);
        if (lastFocus != null) {
            Label sev = new Label(lastFocus.getSeverity());
            sev.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;"
                    + "-fx-background-color: " + severityColor(lastFocus.getSeverity()) + ";"
                    + "-fx-background-radius: 999; -fx-padding: 3 10;");
            header.getChildren().add(sev);
        }
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button showMore = new Button(wide ? "Show less" : "Show more");
        showMore.setStyle(btn());
        showMore.setOnAction(e -> {
            wide = !wide;
            render();
        });
        Button copy = new Button("Copy");
        copy.setStyle(btn());
        copy.setOnAction(e -> copyToClipboard(snippet));

        Button edit = new Button(editing ? "View" : "Edit code");
        edit.setStyle(btnPrimary());
        edit.setOnAction(e -> {
            editing = !editing;
            render();
        });

        HBox actions = new HBox(8, spacer, showMore, copy, edit);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox top = new HBox(12, header, actions);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions, Priority.ALWAYS);

        if (editing) {
            root.getChildren().setAll(top, buildEditor(snippet));
        } else {
            root.getChildren().setAll(top, buildViewer(snippet));
        }
    }

    private VBox buildViewer(CodeSnippet snippet) {
        Map<Integer, String> gutter = new HashMap<>();
        for (IssueRow row : lastAll) {
            if (row.getFile() != null && lastFile != null
                    && row.getFile().replace('\\', '/').endsWith(
                    lastFile.replace('\\', '/').substring(Math.max(0, lastFile.replace('\\', '/').lastIndexOf('/') + 1)))) {
                try {
                    int ln = Integer.parseInt(row.getLine().replaceAll("[^0-9]", ""));
                    gutter.put(ln, row.getSeverity());
                } catch (Exception ignored) {
                }
            }
        }

        VBox lines = new VBox(0);
        int focus = snippet.focusLine();
        for (int i = 0; i < snippet.lines().size(); i++) {
            int lineNo = snippet.firstLine() + i;
            String content = snippet.lines().get(i);
            boolean isFocus = lineNo == focus;
            String markerSeverity = gutter.get(lineNo);
            lines.getChildren().add(lineRow(lineNo, content, isFocus, markerSeverity));
            if (isFocus && lastFocus != null) {
                lines.getChildren().add(annotation(lastFocus));
            }
        }

        ScrollPane scroll = new ScrollPane(lines);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(wide ? WHOLE_FILE_HEIGHT : 300);
        scroll.setStyle("-fx-background: " + EDITOR_BG + "; -fx-background-color: " + EDITOR_BG + "; -fx-border-color: transparent;");

        VBox editor = new VBox(scroll);
        editor.setStyle("-fx-background-color: " + EDITOR_BG + "; -fx-background-radius: 12;"
                + "-fx-border-color: #1E293B; -fx-border-radius: 12;");
        return editor;
    }

    private VBox buildEditor(CodeSnippet snippet) {
        String full;
        try {
            if (resolvedFile != null && Files.isRegularFile(resolvedFile)) {
                full = Files.readString(resolvedFile, StandardCharsets.UTF_8);
            } else {
                full = String.join(System.lineSeparator(), snippet.lines());
            }
        } catch (Exception ex) {
            full = String.join(System.lineSeparator(), snippet.lines());
        }

        TextArea area = new TextArea(full);
        area.setStyle(FONT + "-fx-control-inner-background: #0F172A; -fx-text-fill: #E2E8F0;"
                + "-fx-highlight-fill: #2563EB; -fx-highlight-text-fill: white;");
        area.setPrefHeight(wide ? WHOLE_FILE_HEIGHT : 320);
        area.setWrapText(false);

        Label hint = new Label("Edit the full file, then Save to disk. Re-run Analyze to refresh findings.");
        hint.setStyle(muted(12));

        Label status = new Label("");
        status.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669;");

        Button save = new Button("Save to project");
        save.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold;"
                + "-fx-background-radius: 10; -fx-padding: 8 16; -fx-cursor: hand;");
        save.setOnAction(e -> {
            if (resolvedFile == null) {
                status.setText("Could not resolve file path.");
                status.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
                return;
            }
            try {
                Files.writeString(resolvedFile, area.getText(), StandardCharsets.UTF_8);
                status.setText("Saved: " + resolvedFile.getFileName());
                status.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669;");
            } catch (Exception ex) {
                status.setText("Save failed: " + ex.getMessage());
                status.setStyle("-fx-font-size: 12px; -fx-text-fill: #DC2626;");
            }
        });

        Button cancel = new Button("Cancel");
        cancel.setStyle(btn());
        cancel.setOnAction(e -> {
            editing = false;
            render();
        });

        HBox bar = new HBox(10, save, cancel, status);
        bar.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, hint, area, bar);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: " + EDITOR_BG + "; -fx-background-radius: 12;");
        return box;
    }

    private static Path resolvePath(Path project, String fileName) {
        if (project == null || fileName == null || fileName.isBlank()) {
            return null;
        }
        Path direct = Path.of(fileName);
        if (direct.isAbsolute() && Files.isRegularFile(direct)) {
            return direct;
        }
        Path under = project.resolve(fileName).normalize();
        if (Files.isRegularFile(under)) {
            return under;
        }
        // try stripping leading project folder name
        String normalized = fileName.replace('\\', '/');
        int slash = normalized.indexOf('/');
        if (slash > 0) {
            Path alt = project.resolve(normalized.substring(slash + 1)).normalize();
            if (Files.isRegularFile(alt)) {
                return alt;
            }
        }
        return Files.isRegularFile(under) ? under : null;
    }

    private HBox lineRow(int lineNo, String content, boolean focus, String markerSeverity) {
        Label gutter = new Label(String.valueOf(lineNo));
        gutter.setMinWidth(48);
        gutter.setAlignment(Pos.CENTER_RIGHT);
        gutter.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-padding: 2 8 2 0;");
        if (markerSeverity != null) {
            gutter.setStyle("-fx-text-fill: " + severityColor(markerSeverity)
                    + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 8 2 0;");
        }

        TextFlow flow = new TextFlow();
        JavaSyntax.State state = new JavaSyntax.State();
        for (JavaSyntax.Token seg : JavaSyntax.tokenize(content, state)) {
            flow.getChildren().add(piece(seg.text(), seg.kind()));
        }
        flow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(flow, Priority.ALWAYS);

        HBox row = new HBox(0, gutter, flow);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(1, 12, 1, 4));
        if (focus) {
            String color = lastFocus == null ? INFO : severityColor(lastFocus.getSeverity());
            row.setStyle("-fx-background-color: " + tint(color) + ";");
        } else if (markerSeverity != null) {
            row.setStyle("-fx-background-color: rgba(255,255,255,0.04);");
        }
        return row;
    }

    private VBox annotation(IssueRow issue) {
        String color = severityColor(issue.getSeverity());
        Label message = wrapped(issue.getRule() + ": " + issue.getMessage(),
                "-fx-text-fill: #F8FAFC; -fx-font-size: 13px; -fx-font-weight: bold;");
        VBox note = new VBox(4, message);
        if (issue.getSuggestion() != null && !issue.getSuggestion().isBlank()) {
            note.getChildren().add(wrapped("Fix: " + issue.getSuggestion(),
                    "-fx-text-fill: #CBD5E1; -fx-font-size: 12px;"));
        }
        note.setPadding(new Insets(8, 12, 8, 12));
        note.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 8;"
                + "-fx-border-color: " + color + "; -fx-border-radius: 8;");
        VBox holder = new VBox(note);
        holder.setPadding(new Insets(2, 14, 8, 72));
        return holder;
    }

    private static Text piece(String content, JavaSyntax.Kind kind) {
        Text t = new Text(content);
        t.setStyle("-fx-fill: " + kindColor(kind) + ";" + FONT
                + (kind == JavaSyntax.Kind.KEYWORD ? "-fx-font-weight: bold;" : ""));
        return t;
    }

    private static String kindColor(JavaSyntax.Kind kind) {
        return switch (kind) {
            case KEYWORD -> "#C4B5FD";
            case STRING -> "#86EFAC";
            case COMMENT -> "#64748B";
            case NUMBER -> "#FDBA74";
            case ANNOTATION -> "#FDE68A";
            default -> "#E2E8F0";
        };
    }

    private static String severityColor(String severity) {
        if (severity == null) {
            return INFO;
        }
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> CRITICAL;
            case "WARNING" -> WARNING;
            default -> INFO;
        };
    }

    private static String tint(String hex) {
        return switch (hex) {
            case CRITICAL -> "rgba(239,68,68,0.22)";
            case WARNING -> "rgba(245,158,11,0.20)";
            default -> "rgba(59,130,246,0.20)";
        };
    }

    private static void copyToClipboard(CodeSnippet snippet) {
        ClipboardContent content = new ClipboardContent();
        content.putString(String.join(System.lineSeparator(), snippet.lines()));
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static Label wrapped(String text, String style) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setStyle(style);
        return l;
    }

    private static String muted(int size) {
        return "-fx-text-fill: #64748B; -fx-font-size: " + size + "px;";
    }

    private static String strong(int size) {
        return "-fx-text-fill: #0F172A; -fx-font-size: " + size + "px; -fx-font-weight: bold;";
    }

    private static String btn() {
        return "-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-background-radius: 10;"
                + "-fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 12px;";
    }

    private static String btnPrimary() {
        return "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-background-radius: 10;"
                + "-fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;";
    }
}
