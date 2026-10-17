package com.mycompany.cleancheck.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The slice of a source file shown in the code review: a few lines around the finding, or a larger window
 * of the whole file. UI-free. Problems (no project, bad line, missing file) come back as {@link #error()}.
 */
public record CodeSnippet(
        String fileName,
        int focusLine,
        int firstLine,
        List<String> lines,
        int totalLines,
        boolean startsInBlockComment,
        String error) {

    /** Lines shown on each side of the finding in the short view. */
    public static final int CONTEXT = 7;
    /** Lines shown on each side of the finding in the whole-file view (the window is capped for speed). */
    public static final int WIDE = 150;

    public static CodeSnippet failure(String fileName, String message) {
        return new CodeSnippet(fileName, 0, 1, List.of(), 0, false, message);
    }

    public boolean ok() {
        return error == null;
    }

    public int lastLine() {
        return firstLine + lines.size() - 1;
    }

    public boolean hasMoreAbove() {
        return firstLine > 1;
    }

    public boolean hasMoreBelow() {
        return lastLine() < totalLines;
    }

    public static CodeSnippet load(Path project, String fileName, String lineText, boolean wide) {
        if (project == null) {
            return failure(fileName, "Project path is not selected.");
        }
        int focus;
        try {
            focus = Integer.parseInt(lineText == null ? "" : lineText.trim());
        } catch (NumberFormatException ex) {
            return failure(fileName, "Invalid line number: " + lineText);
        }
        try {
            Path file = CodePreview.findJavaFile(project, fileName);
            if (file == null) {
                return failure(fileName, "Source file not found: " + fileName);
            }
            List<String> all = Files.readAllLines(file);
            if (all.isEmpty()) {
                return failure(fileName, "The source file is empty.");
            }
            int clamped = Math.max(1, Math.min(focus, all.size()));
            int radius = wide ? WIDE : CONTEXT;
            int start = Math.max(1, clamped - radius);
            int end = Math.min(all.size(), clamped + radius);

            JavaSyntax.State state = new JavaSyntax.State();
            for (int n = 1; n < start; n++) {
                JavaSyntax.tokenize(all.get(n - 1), state);
            }
            return new CodeSnippet(fileName, clamped, start, new ArrayList<>(all.subList(start - 1, end)),
                    all.size(), state.inBlockComment(), null);
        } catch (IOException ex) {
            return failure(fileName, "Unable to read source file: " + ex.getMessage());
        }
    }
}
