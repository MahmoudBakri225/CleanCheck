package com.mycompany.cleancheck.rules;

/**
 * Common marker for both per-file rules ({@link Rule}) and project-wide rules ({@link ProjectRule}).
 * Allows the engine and UI to treat every analysis rule uniformly by name.
 */
public interface AnalysisRule {
    String getName();
}
