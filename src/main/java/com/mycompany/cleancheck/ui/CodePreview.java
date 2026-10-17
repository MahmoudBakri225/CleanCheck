package com.mycompany.cleancheck.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds the "source around the issue" text shown under the issues table.
 * Pure Java (no JavaFX), so it is unit-tested directly.
 */
final class CodePreview {

    /** Lines shown on each side of the issue line. */
    private static final int CONTEXT = 3;

    private CodePreview() {
    }

    /** The text to show for {@code fileName} at {@code lineText}; problems come back as a readable message. */
    static String render(Path projectPath, String fileName, String lineText) {
        if (projectPath == null) {
            return "Project path is not selected.";
        }

        int issueLine;
        try {
            issueLine = Integer.parseInt(lineText.trim());
        } catch (NumberFormatException ex) {
            return "Invalid line number: " + lineText;
        }

        try {
            Path filePath = findJavaFile(projectPath, fileName);
            if (filePath == null) {
                return "Source file not found: " + fileName;
            }

            List<String> lines = Files.readAllLines(filePath);
            if (lines.isEmpty()) {
                return "The source file is empty.";
            }

            int start = Math.max(1, issueLine - CONTEXT);
            int end = Math.min(lines.size(), issueLine + CONTEXT);

            StringBuilder preview = new StringBuilder();
            for (int n = start; n <= end; n++) {
                preview.append(n == issueLine ? "  >>> " : "      ")
                        .append(String.format("%4d | ", n))
                        .append(lines.get(n - 1))
                        .append(System.lineSeparator());
            }
            return preview.toString();

        } catch (IOException ex) {
            return "Unable to read source file.\n\n" + ex.getMessage();
        }
    }

    /** The file directly under the project, otherwise the first file with that name below it; null if none. */
    static Path findJavaFile(Path project, String fileName) throws IOException {
        Path direct = project.resolve(fileName);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        try (var stream = Files.walk(project)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);
        }
    }
}
