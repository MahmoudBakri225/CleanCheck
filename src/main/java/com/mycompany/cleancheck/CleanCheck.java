package com.mycompany.cleancheck;

import com.mycompany.cleancheck.scanner.JavaFileScanner;
import com.mycompany.cleancheck.quality.QualityScore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CleanCheck {

    public static void main(String[] args) {
        try {
            Path projectPath = Path.of("test-project");

            JavaFileScanner scanner = new JavaFileScanner();
            RuleEngine engine = new RuleEngine();

            List<Path> javaFiles = scanner.scan(projectPath);

            System.out.println("========================================");
            System.out.println(" CLEAN CHECK");
            System.out.println(" Java Code Quality Analyzer");
            System.out.println("========================================");
            System.out.println("Project: " + projectPath.toAbsolutePath());
            System.out.println("Files Found: " + javaFiles.size());
            System.out.println("========================================");

            List<Warning> allWarnings = new ArrayList<>();

            for (Path file : javaFiles) {
                String source = scanner.readFile(file);
                String fileName = file.getFileName().toString();

                System.out.println("\nAnalyzing: " + fileName);

                List<Warning> warnings = engine.analyze(fileName, source);
                allWarnings.addAll(warnings);

                for (Warning w : warnings) {
                    System.out.println("\n" + w);
                }
            }

            QualityScore quality = new QualityScore();
            int score = quality.calculate(allWarnings);
            String rating = quality.getRating(score);

            System.out.println("\n========================================");
            System.out.println(" CODE QUALITY REPORT");
            System.out.println("========================================");
            System.out.println("Quality Score: " + score + " / 100");
            System.out.println("Rating: " + rating);
            System.out.println();
            System.out.println("Critical Issues: " + quality.countCritical(allWarnings));
            System.out.println("Warnings      : " + quality.countWarnings(allWarnings));
            System.out.println("Info          : " + quality.countInfo(allWarnings));
            System.out.println();
            System.out.println("Total Files   : " + javaFiles.size());
            System.out.println("Total Issues  : " + allWarnings.size());

            quality.printMostCommonProblems(allWarnings);
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("Analysis failed: " + e.getMessage());
        }
    }
}
