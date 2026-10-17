package com.mycompany.cleancheck.config;

import com.mycompany.cleancheck.analysis.Severity;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Reads cleancheck.properties (optional).
 * <pre>
 * rules.disabled = Rule One, Rule Two
 * method.maxLines = 40            (also: method.maxParameters, method.maxComplexity,
 *                                  nesting.maxDepth, class.maxLines, duplicate.minStatements)
 * severity.Magic_Number = INFO    (CRITICAL, WARNING or INFO; "_" stands for the space in the rule name)
 * exclude = **&#47;generated/**, **&#47;*Test.java
 * </pre>
 * A single line can also be silenced in source with {@code // cleancheck:ignore [Rule Name, ...]},
 * and a whole file with {@code // cleancheck:ignore-file}.
 * @author KA
 */
public final class RuleConfig {
    public static final String FILE_NAME = "cleancheck.properties";
    private static final String SEVERITY_PREFIX = "severity.";

    private RuleConfig() {
    }

    public static CleanCheckConfig load() {
        return load(Path.of(System.getProperty("cleancheck.config", FILE_NAME)));
    }

    /** Loads the selected project's config, unless an explicit config path was supplied. */
    public static CleanCheckConfig loadForProject(Path projectRoot) {
        String override = System.getProperty("cleancheck.config");
        Path configPath = override == null || override.isBlank()
                ? projectRoot.resolve(FILE_NAME)
                : Path.of(override);
        return load(configPath);
    }

    public static Set<String> loadDisabled() {
        return load().disabled();
    }

    public static Set<String> loadDisabled(Path path) {
        return load(path).disabled();
    }

    /** Invalid or missing entries are ignored, so a typo never stops an analysis. */
    public static CleanCheckConfig load(Path path) {
        if (path == null || !Files.exists(path)) {
            return CleanCheckConfig.defaults();
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("CleanCheck config path is not a regular file: "
                    + path.toAbsolutePath().normalize());
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to read CleanCheck config: " + path.toAbsolutePath().normalize(), ex);
        }

        Set<String> disabled = new HashSet<>();
        for (String part : properties.getProperty("rules.disabled", "").split(",")) {
            String name = part.trim();
            if (!name.isEmpty()) {
                disabled.add(name.toLowerCase(Locale.ROOT));
            }
        }

        Map<String, Integer> thresholds = new HashMap<>();
        for (String key : CleanCheckConfig.DEFAULT_THRESHOLDS.keySet()) {
            String raw = properties.getProperty(key);
            if (raw != null) {
                try {
                    int value = Integer.parseInt(raw.trim());
                    if (value > 0) {
                        thresholds.put(key, value);
                    }
                } catch (NumberFormatException ignored) {
                    // keep the default
                }
            }
        }

        Map<String, Severity> severities = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.toLowerCase(Locale.ROOT).startsWith(SEVERITY_PREFIX)) {
                // ".properties" keys end at the first space, so "severity.Magic Number" cannot work:
                // write severity.Magic_Number (or Magic-Number, or Magic\\ Number).
                String rule = key.substring(SEVERITY_PREFIX.length()).replace('_', ' ').replace('-', ' ')
                        .trim().toLowerCase(Locale.ROOT);
                try {
                    severities.put(rule, Severity.valueOf(properties.getProperty(key).trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ignored) {
                    // unknown severity: keep the built-in one
                }
            }
        }

        List<String> excludes = new ArrayList<>();
        for (String part : properties.getProperty("exclude", "").split(",")) {
            String glob = part.trim();
            if (!glob.isEmpty()) {
                excludes.add(glob);
            }
        }

        return new CleanCheckConfig(disabled, thresholds, severities, excludes);
    }
}
