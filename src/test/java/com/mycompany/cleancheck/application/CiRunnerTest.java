package com.mycompany.cleancheck.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CiRunnerTest {

    @Test
    void rejectsOnePathUsedForDifferentReportFormatsBeforeWriting(@TempDir Path temporaryDirectory)
            throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path output = temporaryDirectory.resolve("report.json");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = run(project, errors,
                "--json", output.toString(), "--csv", output.toString());

        assertEquals(CiRunner.EXIT_ERROR, exitCode);
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("same file"), errors.toString(StandardCharsets.UTF_8));
        assertTrue(Files.notExists(output));
    }

    @Test
    void rejectsHardLinkAliasesWithoutReplacingEitherFile(@TempDir Path temporaryDirectory) throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path first = temporaryDirectory.resolve("report.json");
        Path alias = temporaryDirectory.resolve("report.csv");
        String original = "keep this existing file";
        Files.writeString(first, original);
        Files.createLink(alias, first);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        int exitCode = run(project, errors,
                "--json", first.toString(), "--csv", alias.toString());

        assertEquals(CiRunner.EXIT_ERROR, exitCode);
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("same file"), errors.toString(StandardCharsets.UTF_8));
        assertEquals(original, Files.readString(first));
        assertEquals(original, Files.readString(alias));
    }

    private static int run(Path project, ByteArrayOutputStream errors, String... options) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
                PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            String[] args = new String[options.length + 1];
            args[0] = project.toString();
            System.arraycopy(options, 0, args, 1, options.length);
            return CiRunner.run(args, out, err);
        }
    }
}
