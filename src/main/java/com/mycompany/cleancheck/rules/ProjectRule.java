package com.mycompany.cleancheck.rules;

import com.mycompany.cleancheck.analysis.Warning;

import java.util.List;
import java.util.Map;

/** A rule that needs to see every file at once (for example duplicate code across files). */
public interface ProjectRule {

    String getName();

    /** @param sourcesByFile project-relative file name to source text, in a stable order */
    List<Warning> analyze(Map<String, String> sourcesByFile);
}
