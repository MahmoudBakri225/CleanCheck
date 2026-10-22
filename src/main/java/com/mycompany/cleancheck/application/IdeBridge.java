package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.analysis.Warning;

import java.nio.file.Path;
import java.util.List;

/**
 * IDE-friendly problem list (one finding per line) for External Tools / NetBeans / IntelliJ regex parsers:
 * {@code path:line: SEVERITY: rule: message}
 */
public final class IdeBridge {

    private IdeBridge() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: IdeBridge <project-or-file>");
            System.exit(2);
        }
        Path target = Path.of(args[0]).toAbsolutePath().normalize();
        AnalysisResult result = new ProjectAnalyzer().analyze(
                java.nio.file.Files.isDirectory(target) ? target : target.getParent());
        printProblems(result.warnings());
        System.exit(result.warnings().isEmpty() ? 0 : 1);
    }

    public static void printProblems(List<Warning> warnings) {
        for (Warning w : warnings) {
            System.out.println(
                    w.getFileName() + ":" + w.getLineNumber() + ": "
                            + w.getSeverity() + ": "
                            + w.getRuleName() + ": "
                            + w.getMessage());
        }
    }
}
