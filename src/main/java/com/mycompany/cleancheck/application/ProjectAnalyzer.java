package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.config.RuleConfig;
import com.mycompany.cleancheck.rules.RuleEngine;
import com.mycompany.cleancheck.scanner.JavaFileScanner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.regex.PatternSyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Single source of truth for project analysis.
 * Dashboard, CLI reports and Class Mode use the same scanner/rules pipeline.
 */
public final class ProjectAnalyzer {

    private final JavaFileScanner scanner;
    private final RuleEngine configuredEngine;

    public ProjectAnalyzer() {
        this.scanner = new JavaFileScanner();
        this.configuredEngine = null;
    }

    /** Uses the given engine, so callers can ignore cleancheck.properties (verification, tests). */
    public ProjectAnalyzer(RuleEngine engine) {
        this.scanner = new JavaFileScanner();
        this.configuredEngine = Objects.requireNonNull(engine, "engine");
    }

    public AnalysisResult analyze(Path projectRoot) throws IOException {
        if (projectRoot == null) {
            throw new IOException("Project path is required.");
        }

        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("Project folder does not exist: " + root);
        }

        RuleEngine engine = configuredEngine == null
                ? new RuleEngine(RuleConfig.loadForProject(root))
                : configuredEngine;
        List<Path> files = excludeConfigured(root, scanner.scan(root), engine);
        List<Warning> warnings = new ArrayList<>();
        Map<String, String> sources = new LinkedHashMap<>();
        long totalSourceLines = 0;

        for (Path file : files) {
            String relative = relativeName(root, file);
            String source = scanner.readFile(file);
            sources.put(relative, source);
            totalSourceLines = Math.min(Integer.MAX_VALUE, totalSourceLines + source.lines().count());
            warnings.addAll(engine.analyze(relative, source));
        }
        warnings.addAll(engine.analyzeProject(sources));

        return new AnalysisResult(root, files, warnings, (int) totalSourceLines);
    }

    private static String relativeName(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    /** Drops files matching an "exclude" glob from cleancheck.properties (patterns are relative to the project). */
    private List<Path> excludeConfigured(Path root, List<Path> files, RuleEngine engine) throws IOException {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String glob : engine.config().excludeGlobs()) {
            try {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
            } catch (PatternSyntaxException ex) {
                throw new IOException("Invalid CleanCheck exclude glob '" + glob + "'.", ex);
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

    /** Returns the injected engine, or an application-default engine for introspection. */
    public RuleEngine engine() {
        return configuredEngine == null ? new RuleEngine() : configuredEngine;
    }

}
