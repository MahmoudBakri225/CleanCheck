package com.mycompany.cleancheck.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

class JavaFileScannerTest {

    @Test
    void rejectsSymbolicLinkProjectRootInsteadOfReturningAnEmptyCleanScan(@TempDir Path temporaryDirectory)
            throws IOException {
        Path actualProject = Files.createDirectory(temporaryDirectory.resolve("actual-project"));
        Files.writeString(actualProject.resolve("App.java"), "class App {}\n");
        Path linkedProject = temporaryDirectory.resolve("linked-project");
        try {
            Files.createSymbolicLink(linkedProject, actualProject);
        } catch (IOException | UnsupportedOperationException | SecurityException unavailable) {
            Assumptions.assumeTrue(false, "This environment cannot create symbolic links.");
            return;
        }

        IOException failure = assertThrows(IOException.class, () -> new JavaFileScanner().scan(linkedProject));

        assertTrue(failure.getMessage().contains("symbolic link"));
    }

    @Test
    void prunesBuildAndGitTreesButKeepsFoldersNamedTargetInsideSources(@TempDir Path project)
            throws IOException {
        Path buildOutput = Files.createDirectories(project.resolve("target/generated"));
        Path gitObjects = Files.createDirectories(project.resolve(".git/objects"));
        Path sourcePackage = Files.createDirectories(project.resolve("src/main/java/target"));
        Files.writeString(buildOutput.resolve("Generated.java"), "class Generated {}\n");
        Files.writeString(gitObjects.resolve("Tracked.java"), "class Tracked {}\n");
        Path expected = sourcePackage.resolve("Domain.java");
        Files.writeString(expected, "class Domain {}\n");

        List<Path> files = new JavaFileScanner().scan(project);

        assertEquals(List.of(expected), files);
    }
}
