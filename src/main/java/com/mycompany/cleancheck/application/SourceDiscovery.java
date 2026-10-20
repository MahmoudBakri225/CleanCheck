package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.rules.RuleEngine;
import com.mycompany.cleancheck.scanner.JavaFileScanner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Responsible only for finding Java source files and applying exclude globs.
 * Separated from analysis so discovery can be reused and tested independently.
 */
public final class SourceDiscovery {

    private final JavaFileScanner scanner;

    public SourceDiscovery() {
        this.scanner = new JavaFileScanner();
    }

    public SourceDiscovery(JavaFileScanner scanner) {
        this.scanner = scanner;
    }

    public List<Path> discover(Path projectRoot, RuleEngine engine) throws IOException {
        if (projectRoot == null) {
            throw new IOException("Project path is required.");
        }
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("Project folder does not exist: " + root);
        }
        return excludeConfigured(root, scanner.scan(root), engine);
    }

    public String read(Path file) throws IOException {
        return scanner.readFile(file);
    }

    public static String relativeName(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private List<Path> excludeConfigured(Path root, List<Path> files, RuleEngine engine) throws IOException {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String glob : engine.config().excludeGlobs()) {
            try {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
            } catch (PatternSyntaxException ex) {
                throw new IOException("Invalid CleanCheck exclude glob '" + glob + "'. Check cleancheck.properties.", ex);
            }
        }
        if (matchers.isEmpty()) {
            return files;
        }
        return files.stream()
                .filter(file -> {
                    Path relative = Path.of(relativeName(root, file));
                    return matchers.stream().noneMatch(m -> m.matches(relative));
                })
                .toList();
    }

    public JavaFileScanner scanner() {
        return scanner;
    }
}
