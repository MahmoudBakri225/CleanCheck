package com.mycompany.cleancheck.config;

import com.mycompany.cleancheck.analysis.Severity;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Settings read from cleancheck.properties. Every value has a default, so the file is optional.
 *
 * @param disabled        lower-case names of rules to skip
 * @param thresholds      numeric limits by key (see {@link #DEFAULT_THRESHOLDS})
 * @param severityByRule  lower-case rule name to the severity that replaces the built-in one
 * @param excludeGlobs    glob patterns, relative to the project root, of files that are not analyzed
 */
public record CleanCheckConfig(
        Set<String> disabled,
        Map<String, Integer> thresholds,
        Map<String, Severity> severityByRule,
        java.util.List<String> excludeGlobs
) {
    public static final String METHOD_MAX_LINES = "method.maxLines";
    public static final String METHOD_MAX_PARAMETERS = "method.maxParameters";
    public static final String METHOD_MAX_COMPLEXITY = "method.maxComplexity";
    public static final String NESTING_MAX_DEPTH = "nesting.maxDepth";
    public static final String CLASS_MAX_LINES = "class.maxLines";
    public static final String DUPLICATE_MIN_STATEMENTS = "duplicate.minStatements";

    public static final Map<String, Integer> DEFAULT_THRESHOLDS = Map.of(
            METHOD_MAX_LINES, 40,
            METHOD_MAX_PARAMETERS, 4,
            METHOD_MAX_COMPLEXITY, 10,
            NESTING_MAX_DEPTH, 4,
            CLASS_MAX_LINES, 500,
            DUPLICATE_MIN_STATEMENTS, 5
    );

    public CleanCheckConfig {
        disabled = Set.copyOf(disabled);
        thresholds = Map.copyOf(thresholds);
        severityByRule = Map.copyOf(severityByRule);
        excludeGlobs = java.util.List.copyOf(excludeGlobs);
    }

    public static CleanCheckConfig defaults() {
        return new CleanCheckConfig(Set.of(), Map.of(), Map.of(), java.util.List.of());
    }

    public static CleanCheckConfig withDisabled(Set<String> disabled) {
        return new CleanCheckConfig(disabled, Map.of(), Map.of(), java.util.List.of());
    }

    public int threshold(String key) {
        Integer configured = thresholds.get(key);
        return configured != null ? configured : DEFAULT_THRESHOLDS.get(key);
    }

    public Severity severityFor(String ruleName, Severity builtIn) {
        return severityByRule.getOrDefault(ruleName.toLowerCase(Locale.ROOT), builtIn);
    }
}
