package com.mycompany.cleancheck.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.rules.Rule;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathTraversalRuleTest {

    @Test
    void detectsRequestLikePathParameterUsedForFileIo() {
        String source = """
                import java.io.FileInputStream;
                class Download {
                    FileInputStream open(String requestedPath) throws Exception {
                        return new FileInputStream(requestedPath);
                    }
                }
                """;

        assertTrue(analyze(source).stream().anyMatch(w -> w.getRuleName().equals("Path Traversal")));
    }

    @Test
    void pathNormalizationWithoutBaseContainmentDoesNotSuppressTheFinding() {
        String source = """
                import java.nio.file.Files;
                import java.nio.file.Path;
                class Download {
                    String read(String userPath) throws Exception {
                        Path target = Path.of("/srv/uploads", userPath).normalize();
                        return Files.readString(target);
                    }
                }
                """;

        assertTrue(analyze(source).stream().anyMatch(w -> w.getRuleName().equals("Path Traversal")));
    }

    @Test
    void acceptsNormalizedPathAfterContainmentCheck() {
        String source = """
                import java.nio.file.Files;
                import java.nio.file.Path;
                class Download {
                    String read(Path base, String userPath) throws Exception {
                        Path target = base.resolve(userPath).normalize();
                        if (!target.startsWith(base)) throw new SecurityException();
                        return Files.readString(target);
                    }
                }
                """;

        assertFalse(analyze(source).stream().anyMatch(w -> w.getRuleName().equals("Path Traversal")));
    }

    @Test
    void doesNotFlagAnExplicitCommandLineOutputPath() {
        String source = """
                import java.nio.file.Files;
                import java.nio.file.Path;
                class Cli {
                    void write(String[] args) throws Exception {
                        Files.writeString(Path.of(args[0]), "report");
                    }
                }
                """;

        assertFalse(analyze(source).stream().anyMatch(w -> w.getRuleName().equals("Path Traversal")));
    }

    @Test
    void readingAnAlreadySelectedFileDoesNotTurnItsContentsIntoPathTaint() {
        String source = """
                import java.nio.file.Files;
                import java.nio.file.Path;
                class Review {
                    void save(Path selectedFile) throws Exception {
                        String contents = Files.readString(selectedFile);
                        Files.writeString(selectedFile, contents);
                    }
                }
                """;

        assertFalse(analyze(source).stream().anyMatch(w -> w.getRuleName().equals("Path Traversal")));
    }

    private static List<Warning> analyze(String source) {
        Rule rule = SecurityRules.all().stream()
                .filter(candidate -> candidate.getName().equals("Path Traversal"))
                .findFirst().orElseThrow();
        return rule.analyze("Example.java", source);
    }
}
