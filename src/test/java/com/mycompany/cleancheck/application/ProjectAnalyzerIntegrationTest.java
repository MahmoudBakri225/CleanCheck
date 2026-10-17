package com.mycompany.cleancheck.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.cleancheck.rules.RuleEngine;
import com.mycompany.cleancheck.config.CleanCheckConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class ProjectAnalyzerIntegrationTest {

    private String previousConfigPath;

    @BeforeEach
    void clearConfigOverride() {
        previousConfigPath = System.getProperty("cleancheck.config");
        System.clearProperty("cleancheck.config");
    }

    @AfterEach
    void restoreConfigOverride() {
        if (previousConfigPath == null) {
            System.clearProperty("cleancheck.config");
        } else {
            System.setProperty("cleancheck.config", previousConfigPath);
        }
    }

    @Test
    void scansProjectAndReportsFilesThatCouldNotBeParsed(@TempDir Path temporaryDirectory) throws IOException {
        Path sourceDirectory = Files.createDirectories(temporaryDirectory.resolve("src"));
        Files.writeString(sourceDirectory.resolve("Sample.java"), """
                package example;
                class Sample {
                    void run() { System.out.println("ready"); }
                }
                """);
        Files.writeString(sourceDirectory.resolve("Broken.java"), "class Broken {\n");

        AnalysisResult result = new ProjectAnalyzer(new RuleEngine(Set.of())).analyze(temporaryDirectory);

        assertEquals(2, result.javaFiles().size());
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.getFileName().equals("src/Broken.java")
                && warning.getRuleName().equals(RuleEngine.PARSE_ERROR_RULE)));
        assertTrue(result.warnings().stream().anyMatch(warning -> warning.getFileName().equals("src/Sample.java")
                && warning.getRuleName().equals("System Out")));
    }

    @Test
    void configuredGlobExcludesOnlyMatchingProjectFiles(@TempDir Path temporaryDirectory) throws IOException {
        Path generated = Files.createDirectories(temporaryDirectory.resolve("src/generated"));
        Files.writeString(generated.resolve("Generated.java"), "class Generated {}\n");
        Files.writeString(temporaryDirectory.resolve("App.java"), "class App {}\n");
        RuleEngine engine = new RuleEngine(new CleanCheckConfig(Set.of(), Map.of(), Map.of(),
                List.of("src/generated/**")));

        AnalysisResult result = new ProjectAnalyzer(engine).analyze(temporaryDirectory);

        assertEquals(1, result.javaFiles().size());
        assertFalse(result.javaFiles().get(0).toString().contains("Generated.java"));
    }

    @Test
    void invalidConfiguredGlobFailsWithThePatternInTheMessage(@TempDir Path temporaryDirectory) throws IOException {
        RuleEngine engine = new RuleEngine(new CleanCheckConfig(Set.of(), Map.of(), Map.of(), List.of("[")));

        IOException failure = assertThrows(IOException.class,
                () -> new ProjectAnalyzer(engine).analyze(temporaryDirectory));

        assertTrue(failure.getMessage().contains("["));
    }

    @Test
    void defaultAnalyzerLoadsRulesAndExcludesFromTheSelectedProject(@TempDir Path temporaryDirectory)
            throws IOException {
        Path sourceDirectory = Files.createDirectories(temporaryDirectory.resolve("src"));
        Path generatedDirectory = Files.createDirectories(sourceDirectory.resolve("generated"));
        Files.writeString(sourceDirectory.resolve("App.java"),
                "class App { void run() { System.out.println(\"visible\"); } }\n");
        Files.writeString(generatedDirectory.resolve("Generated.java"),
                "class Generated { void run() { System.out.println(\"hidden\"); } }\n");
        Files.writeString(temporaryDirectory.resolve("cleancheck.properties"),
                "rules.disabled = System Out\nexclude = src/generated/**\n");

        AnalysisResult result = new ProjectAnalyzer().analyze(temporaryDirectory);

        assertEquals(1, result.javaFiles().size());
        assertTrue(result.warnings().stream().noneMatch(warning -> warning.getRuleName().equals("System Out")));
    }

    @Test
    void explicitConfigPathOverridesProjectConfig(@TempDir Path temporaryDirectory) throws IOException {
        Files.writeString(temporaryDirectory.resolve("App.java"),
                "class App { void run() { System.out.println(\"visible\"); } }\n");
        Files.writeString(temporaryDirectory.resolve("cleancheck.properties"), "exclude = [\n");
        Path explicitConfig = temporaryDirectory.resolve("shared.properties");
        Files.writeString(explicitConfig, "rules.disabled = System Out\n");
        System.setProperty("cleancheck.config", explicitConfig.toString());

        AnalysisResult result = new ProjectAnalyzer().analyze(temporaryDirectory);

        assertTrue(result.warnings().stream().noneMatch(warning -> warning.getRuleName().equals("System Out")));
    }

    @Test
    void configPathThatExistsAsADirectoryIsNotTreatedAsMissing(@TempDir Path temporaryDirectory)
            throws IOException {
        Files.writeString(temporaryDirectory.resolve("App.java"), "class App {}\n");
        Files.createDirectory(temporaryDirectory.resolve("cleancheck.properties"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new ProjectAnalyzer().analyze(temporaryDirectory));

        assertTrue(failure.getMessage().contains("cleancheck.properties"));
    }

    @Test
    void sourceLineCountUsesTheSameDecodedSnapshotAsAnalysis(@TempDir Path temporaryDirectory) throws IOException {
        Path source = temporaryDirectory.resolve("Legacy.java");
        Files.write(source, "class Café {}\r\n// naïve source\n".getBytes(StandardCharsets.ISO_8859_1));

        AnalysisResult result = new ProjectAnalyzer(new RuleEngine(Set.of())).analyze(temporaryDirectory);

        assertEquals(2, result.totalSourceLines());
        Files.delete(source);
        assertEquals(2, result.totalSourceLines());
    }
}
