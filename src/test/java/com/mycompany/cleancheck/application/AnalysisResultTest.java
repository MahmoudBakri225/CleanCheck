package com.mycompany.cleancheck.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.report.RegressionSummary;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisResultTest {

    @Test
    void sameFindingsRequiresTheSameMultiplicity() {
        Warning first = finding("src/A.java", "SQL Injection");
        Warning second = finding("src/B.java", "XXE");
        AnalysisResult baseline = new AnalysisResult(Path.of("project"), List.of(),
                List.of(first, first, second));
        AnalysisResult changed = new AnalysisResult(Path.of("project"), List.of(),
                List.of(first, second, second));

        assertFalse(baseline.sameFindingsAs(changed));
        assertTrue(baseline.sameFindingsAs(new AnalysisResult(Path.of("project"), List.of(),
                List.of(second, first, first))));
    }

    @Test
    void regressionCannotBeReusedWhenSourceSizeChanged() {
        RegressionSummary regression = new RegressionSummary(4, 85, 1, 0, 2, 0, 0, 0);
        AnalysisResult earlier = new AnalysisResult(Path.of("project"), List.of(), List.of(), regression, 100);
        AnalysisResult current = new AnalysisResult(Path.of("project"), List.of(), List.of(), null, 101);

        assertTrue(current.withRegressionFrom(earlier).regression() == null);
    }

    private static Warning finding(String file, String rule) {
        return new Warning(file, 10, rule, Severity.WARNING, "message", "suggestion");
    }
}
