package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.config.RuleConfig;
import com.mycompany.cleancheck.rules.RuleEngine;
import com.mycompany.cleancheck.scanner.JavaFileScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates analysis: discovers sources, runs the rule engine, and builds the result.
 * Source discovery is delegated to {@link SourceDiscovery}.
 */
public final class ProjectAnalyzer {

    private final SourceDiscovery discovery;
    private final RuleEngine configuredEngine;

    public ProjectAnalyzer() {
        this.discovery = new SourceDiscovery();
        this.configuredEngine = null;
    }

    public ProjectAnalyzer(RuleEngine engine) {
        this.discovery = new SourceDiscovery();
        this.configuredEngine = Objects.requireNonNull(engine, "engine");
    }

    public ProjectAnalyzer(SourceDiscovery discovery, RuleEngine engine) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.configuredEngine = engine;
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

        List<Path> files = discovery.discover(root, engine);
        List<Warning> warnings = new ArrayList<>();
        Map<String, String> sources = new LinkedHashMap<>();
        long totalSourceLines = 0;

        for (Path file : files) {
            String relative = SourceDiscovery.relativeName(root, file);
            String source = discovery.read(file);
            sources.put(relative, source);
            totalSourceLines = Math.min(Integer.MAX_VALUE, totalSourceLines + source.lines().count());
            warnings.addAll(engine.analyze(relative, source));
        }
        warnings.addAll(engine.analyzeProject(sources));

        return new AnalysisResult(root, files, warnings, (int) totalSourceLines);
    }

    public JavaFileScanner scanner() {
        return discovery.scanner();
    }

    public RuleEngine engine() {
        return configuredEngine == null ? new RuleEngine() : configuredEngine;
    }
}
