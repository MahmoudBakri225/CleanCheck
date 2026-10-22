package com.mycompany.cleancheck.classmode;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.AnalysisResult;
import com.mycompany.cleancheck.application.ProjectAnalyzer;
import com.mycompany.cleancheck.quality.QualityScore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans each direct student-project folder under a class folder
 * and builds ranking + detailed findings per project.
 */
public final class ClassMode {

    private ClassMode() {
    }

    public static List<ClassReport> analyze(Path classFolder) throws IOException {
        if (!Files.isDirectory(classFolder)) {
            throw new IOException("Class folder does not exist: " + classFolder);
        }

        QualityScore score = new QualityScore();
        ProjectAnalyzer analyzer = new ProjectAnalyzer();
        List<ClassReport> reports = new ArrayList<>();

        try (var children = Files.list(classFolder)) {
            for (Path student : children
                    .filter(Files::isDirectory)
                    // Generated exports live in this folder; never treat them as a student's project.
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase("reports"))
                    .sorted().toList()) {
                AnalysisResult result = analyzer.analyze(student);
                List<Warning> warnings = result.warnings();
                reports.add(new ClassReport(
                        student.getFileName().toString(),
                        score.calculate(warnings, result.totalSourceLines()),
                        warnings.size(),
                        score.countCritical(warnings),
                        score.countWarnings(warnings),
                        score.countInfo(warnings),
                        warnings
                ));
            }
        }

        reports.sort(Comparator.comparingInt(ClassReport::score).reversed()
                .thenComparing(ClassReport::student));
        return reports;
    }

    public static Map<String, Integer> commonRules(Path classFolder) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ClassReport report : analyze(classFolder)) {
            for (Warning warning : report.issues()) {
                counts.merge(warning.getRuleName(), 1, Integer::sum);
            }
        }
        return counts;
    }

    public static Map<String, Integer> commonRules(List<ClassReport> reports) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ClassReport report : reports) {
            for (Warning warning : report.issues()) {
                counts.merge(warning.getRuleName(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
