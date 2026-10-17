package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.application.AnalysisResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Prevents a report destination from replacing a source file that was just analyzed. */
final class ReportOutputGuard {

    private ReportOutputGuard() {
    }

    static void rejectSourceOverwrite(AnalysisResult analysis, Path target) throws IOException {
        if (analysis == null) {
            throw new IllegalArgumentException("Analysis result is required.");
        }
        if (target == null || target.getFileName() == null) {
            throw new IOException("A report file path is required.");
        }

        Path destination = target.toAbsolutePath().normalize();
        for (Path source : analysis.javaFiles()) {
            Path normalizedSource = source.toAbsolutePath().normalize();
            boolean samePath = destination.equals(normalizedSource);
            boolean sameFile = false;
            if (!samePath && Files.exists(destination) && Files.exists(normalizedSource)) {
                sameFile = Files.isSameFile(destination, normalizedSource);
            }
            if (samePath || sameFile) {
                throw new IOException("Report output cannot replace analyzed source file: " + source);
            }
        }
    }
}
