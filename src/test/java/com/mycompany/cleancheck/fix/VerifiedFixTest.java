package com.mycompany.cleancheck.fix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.AnalysisResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerifiedFixTest {

    @Test
    void lineShiftsDoNotCreateNewFindings() {
        Warning before = warning(10, Severity.WARNING);
        Warning after = warning(14, Severity.WARNING);

        assertFalse(VerifiedFix.containsNewIssues(List.of(before), List.of(after)));
    }

    @Test
    void severityEscalationCountsAsNewIssue() {
        Warning before = warning(10, Severity.INFO);
        Warning after = warning(10, Severity.CRITICAL);

        assertTrue(VerifiedFix.containsNewIssues(List.of(before), List.of(after)));
    }

    @Test
    void repeatedFindingsAreMatchedAsOccurrencesRatherThanCollapsedIntoASet() {
        Warning first = warning(10, Severity.WARNING);
        Warning second = warning(20, Severity.WARNING);
        Warning third = warning(30, Severity.WARNING);

        assertTrue(VerifiedFix.containsNewIssues(List.of(first, second), List.of(first, second, third)));
    }

    @Test
    void repeatedCompilerDiagnosticsAreComparedByMultiplicity() {
        String diagnostic = "Example.java: incompatible types";

        assertEquals(java.util.Set.of(diagnostic), VerifiedFix.newCompileErrors(
                Map.of(diagnostic, 1), Map.of(diagnostic, 2)));
        assertEquals(java.util.Set.of(), VerifiedFix.newCompileErrors(
                Map.of(diagnostic, 2), Map.of(diagnostic, 1)));
    }

    @Test
    void restoresSourceWhenPostFixAnalysisThrows(@TempDir Path project) throws IOException {
        String original = "class Example { void run() { string value = \"text\"; } }\n";
        Path sourceFile = Files.writeString(project.resolve("Example.java"), original);
        int[] analysisCalls = {0};
        VerifiedFix.AnalysisRunner analyzer = root -> {
            if (analysisCalls[0]++ == 0) {
                return new AnalysisResult(root, List.of(), List.of(
                        warning(1, Severity.CRITICAL)));
            }
            throw new IOException("synthetic analysis failure");
        };

        IOException failure = assertThrows(IOException.class,
                () -> VerifiedFix.apply(analyzer, project, sourceFile, "Data Type", 1));

        assertEquals("synthetic analysis failure", failure.getMessage());
        assertEquals(original, Files.readString(sourceFile));
        Path backupRoot = project.resolve(".cleancheck-backups");
        assertFalse(hasEntries(backupRoot));
    }

    @Test
    void removesBackupWhenFixMakesNoChange(@TempDir Path project) throws IOException {
        Path source = Files.writeString(project.resolve("Example.java"), "class Example {}\n");
        VerifiedFix.AnalysisRunner analyzer = root -> new AnalysisResult(root, List.of(), List.of());

        VerifiedFix.Outcome outcome = VerifiedFix.apply(analyzer, project, source, "Unknown Rule", 1);

        assertEquals(VerifiedFix.Status.NO_CHANGE, outcome.status());
        assertNull(outcome.backup());
        assertFalse(hasEntries(project.resolve(".cleancheck-backups")));
    }

    @Test
    void removesBackupAfterRejectedChangeHasBeenRestored(@TempDir Path project) throws IOException {
        String original = "class Example {\n    public int Value;\n}\n";
        Path source = Files.writeString(project.resolve("Example.java"), original);
        VerifiedFix.AnalysisRunner analyzer = root -> new AnalysisResult(root, List.of(),
                List.of(new Warning("Example.java", 2, "Naming Convention", Severity.WARNING,
                        "Field names should start with a lowercase letter.", "Rename the field.")));

        VerifiedFix.Outcome outcome = VerifiedFix.apply(analyzer, project, source, "Naming Convention", 2);

        assertEquals(VerifiedFix.Status.ROLLED_BACK_NOT_REDUCED, outcome.status());
        assertNull(outcome.backup());
        assertEquals(original, Files.readString(source));
        Path backupRoot = project.resolve(".cleancheck-backups");
        assertFalse(hasEntries(backupRoot));
    }

    @Test
    void keepsRollbackPointForAnAcceptedFix(@TempDir Path project) throws IOException {
        String original = "class Example {\n    public int Value;\n}\n";
        Path source = Files.writeString(project.resolve("Example.java"), original);
        int[] calls = {0};
        VerifiedFix.AnalysisRunner analyzer = root -> new AnalysisResult(root, List.of(),
                calls[0]++ == 0
                        ? List.of(new Warning("Example.java", 2, "Naming Convention", Severity.WARNING,
                                "Field names should start with a lowercase letter.", "Rename the field."))
                        : List.of());

        VerifiedFix.Outcome outcome = VerifiedFix.apply(analyzer, project, source, "Naming Convention", 2);

        assertEquals(VerifiedFix.Status.APPLIED, outcome.status());
        assertTrue(outcome.backup() != null && Files.isRegularFile(outcome.backup()));
        assertEquals(original, Files.readString(outcome.backup()));
        assertFalse(Files.readString(source).equals(original));
    }

    private static boolean hasEntries(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (var entries = Files.list(directory)) {
            return entries.findAny().isPresent();
        }
    }

    private static Warning warning(int line, Severity severity) {
        return new Warning("Example.java", line, "Data Type", severity,
                "'string' is not a valid Java type as written.", "Use String (capital S).");
    }
}
