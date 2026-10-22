package com.mycompany.cleancheck.fix;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.application.AnalysisResult;
import com.mycompany.cleancheck.application.ProjectAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Applies one automatic fix and keeps it only if it is safe: the target finding goes down, no new
 * finding appears, and (when a JDK is present) the project compiles with no new compile error.
 * Otherwise the original file is restored. UI-free, so the GUI, CLI and tests share one code path.
 */
public final class VerifiedFix {

    private record IssueKey(String fileName, String ruleName, Severity severity, String message) {
    }

    @FunctionalInterface
    interface AnalysisRunner {
        AnalysisResult analyze(Path projectRoot) throws IOException;
    }

    public enum Status {
        APPLIED,
        NO_CHANGE,
        ROLLED_BACK_NOT_REDUCED,
        ROLLED_BACK_NEW_ISSUES,
        ROLLED_BACK_COMPILE_ERRORS
    }

    /** backup is retained only for APPLIED outcomes, where the user can still roll back. */
    public record Outcome(Status status, Path backup, Set<String> newCompileErrors, boolean compileChecked) {
        public boolean applied() {
            return status == Status.APPLIED;
        }

        public boolean rolledBack() {
            return status == Status.ROLLED_BACK_NOT_REDUCED
                    || status == Status.ROLLED_BACK_NEW_ISSUES
                    || status == Status.ROLLED_BACK_COMPILE_ERRORS;
        }
    }

    private VerifiedFix() {
    }

    public static Outcome apply(ProjectAnalyzer analyzer, Path projectRoot, Path javaFile, String rule, int line)
            throws IOException {
        return apply(analyzer::analyze, projectRoot, javaFile, rule, line);
    }

    static Outcome apply(AnalysisRunner analyzer, Path projectRoot, Path javaFile, String rule, int line)
            throws IOException {
        String before = SourceFileIO.read(javaFile).text();
        AnalysisResult beforeAnalysis = analyzer.analyze(projectRoot);
        int targetsBefore = count(beforeAnalysis, rule);
        Map<String, Integer> compileBefore = CompileCheck.errorCounts(projectRoot, beforeAnalysis.javaFiles())
                .orElse(null);
        if (compileBefore == null && "Encapsulation".equalsIgnoreCase(rule)) {
            return new Outcome(Status.NO_CHANGE, null, Set.of(), false);
        }

        Path backup = FixVerificationService.createBackup(projectRoot, javaFile);
        try {
            new FixEngine().applyFix(javaFile, rule, line);

            if (before.equals(SourceFileIO.read(javaFile).text())) {
                FixVerificationService.discardBackup(projectRoot, backup);
                return new Outcome(Status.NO_CHANGE, null, Set.of(), compileBefore != null);
            }

            AnalysisResult afterAnalysis = analyzer.analyze(projectRoot);
            if (count(afterAnalysis, rule) >= targetsBefore) {
                restoreAndDiscard(projectRoot, backup, javaFile);
                return new Outcome(Status.ROLLED_BACK_NOT_REDUCED, null, Set.of(), compileBefore != null);
            }
            if (containsNewIssues(beforeAnalysis.warnings(), afterAnalysis.warnings())) {
                restoreAndDiscard(projectRoot, backup, javaFile);
                return new Outcome(Status.ROLLED_BACK_NEW_ISSUES, null, Set.of(), compileBefore != null);
            }

            if (compileBefore != null) {
                Map<String, Integer> compileAfter = CompileCheck.errorCounts(projectRoot, afterAnalysis.javaFiles())
                        .orElse(compileBefore);
                Set<String> introduced = newCompileErrors(compileBefore, compileAfter);
                if (!introduced.isEmpty()) {
                    restoreAndDiscard(projectRoot, backup, javaFile);
                    return new Outcome(Status.ROLLED_BACK_COMPILE_ERRORS, null, introduced, true);
                }
            }
            return new Outcome(Status.APPLIED, backup, Set.of(), compileBefore != null);
        } catch (IOException | RuntimeException failure) {
            try {
                FixVerificationService.restoreBackup(backup, javaFile);
            } catch (IOException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            try {
                FixVerificationService.discardBackup(projectRoot, backup);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private static void restoreAndDiscard(Path projectRoot, Path backup, Path javaFile) throws IOException {
        FixVerificationService.restoreBackup(backup, javaFile);
        FixVerificationService.discardBackup(projectRoot, backup);
    }

    private static int count(AnalysisResult result, String rule) {
        return (int) result.warnings().stream()
                .filter(w -> w.getRuleName().equalsIgnoreCase(rule))
                .count();
    }

    static boolean containsNewIssues(List<Warning> before, List<Warning> after) {
        Map<IssueKey, Integer> known = new HashMap<>();
        for (Warning w : before) {
            known.merge(key(w), 1, Integer::sum);
        }
        for (Warning warning : after) {
            IssueKey key = key(warning);
            int remaining = known.getOrDefault(key, 0);
            if (remaining == 0) {
                return true;
            }
            known.put(key, remaining - 1);
        }
        return false;
    }

    static Set<String> newCompileErrors(Map<String, Integer> before, Map<String, Integer> after) {
        Set<String> introduced = new TreeSet<>();
        for (Map.Entry<String, Integer> error : after.entrySet()) {
            if (error.getValue() > before.getOrDefault(error.getKey(), 0)) {
                introduced.add(error.getKey());
            }
        }
        return Set.copyOf(introduced);
    }

    private static IssueKey key(Warning warning) {
        return new IssueKey(warning.getFileName(), warning.getRuleName(), warning.getSeverity(), warning.getMessage());
    }
}
