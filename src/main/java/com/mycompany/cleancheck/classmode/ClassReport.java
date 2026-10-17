package com.mycompany.cleancheck.classmode;

import com.mycompany.cleancheck.analysis.Warning;

import java.util.List;

/** Summary + detailed findings for one student project. */
public record ClassReport(
        String student,
        int score,
        int totalIssues,
        int critical,
        int warnings,
        int info,
        List<Warning> issues
) {
    public ClassReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
