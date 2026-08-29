package com.mycompany.cleancheck;

import com.mycompany.cleancheck.quality.C_QualityScore;
import com.mycompany.cleancheck.scanner.C_JavaFileScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of the CleanCheck application.
 *
 * Scans a Java project, analyzes its files using all registered rules,
 * displays the results in the console, and generates an HTML report.
 *
 * @author KA
 */
public class C_CleanCheck {

    private static final String PROJECT_NAME = "CleanCheck";

    private static final String LINE =
            "+----------------------------------------------------------------+";

    private static final String SECTION_LINE =
            "  ----------------------------------------------------------------";

    /**
     * Starts the CleanCheck application.
     *
     * @param args optional project path
     */
    public static void main(String[] args) {
        final Path projectPath = Path.of(
                args.length > 0 ? args[0] : "test-project"
        );

        try {
            final C_JavaFileScanner scanner = new C_JavaFileScanner();
            final C_RuleEngine engine = new C_RuleEngine();

            final List<Path> javaFiles = scanner.scan(projectPath);
            final List<C_Warning> allWarnings = new ArrayList<>();

            printHeader(projectPath, javaFiles.size());

            for (int i = 0; i < javaFiles.size(); i++) {
                analyzeFile(
                        scanner,
                        engine,
                        javaFiles.get(i),
                        allWarnings,
                        i + 1
                );
            }

            printReport(javaFiles.size(), allWarnings);

            final List<String> fileNames = new ArrayList<>();

            for (Path file : javaFiles) {
                fileNames.add(file.getFileName().toString());
            }

            final C_HtmlReportGenerator htmlReport =
                    new C_HtmlReportGenerator();

            final String reportPath = htmlReport.generate(
                    PROJECT_NAME,
                    fileNames,
                    allWarnings
            );

            System.out.println();
            System.out.println("HTML report generated: " + reportPath);

        } catch (IOException exception) {
            System.out.println(
                    "Unable to read project files: "
                            + exception.getMessage()
            );
        }
    }

    /**
     * Analyzes one Java file using all registered rules.
     *
     * @param scanner scanner used to read the Java file
     * @param engine rule engine used to analyze the file
     * @param file Java file being analyzed
     * @param allWarnings global list of warnings
     * @param fileNumber file number used for display
     * @throws IOException if the file cannot be read
     */
    private static void analyzeFile(
            C_JavaFileScanner scanner,
            C_RuleEngine engine,
            Path file,
            List<C_Warning> allWarnings,
            int fileNumber
    ) throws IOException {

        final String fileName = file.getFileName().toString();

        final List<C_Warning> warnings = engine.analyze(
                fileName,
                scanner.readFile(file)
        );

        allWarnings.addAll(warnings);

        System.out.println();
        System.out.println(SECTION_LINE);
        System.out.println(">> FILE " + fileNumber + " : " + fileName);
        System.out.println(
                "Status: "
                        + (warnings.isEmpty()
                        ? "No issues found"
                        : warnings.size() + " issue(s) found")
        );
        System.out.println(SECTION_LINE);

        for (C_Warning warning : warnings) {
            System.out.println(warning);
            System.out.println();
        }
    }

    /**
     * Prints the application header.
     *
     * @param projectPath root path of the scanned project
     * @param fileCount number of Java files found
     */
    private static void printHeader(
            Path projectPath,
            int fileCount
    ) {
        System.out.println(LINE);
        System.out.println(
                "|                      CLEAN CHECK                             |"
        );
        System.out.println(
                "|                 Java Code Quality Analyzer                   |"
        );
        System.out.println(LINE);

        System.out.println(
                "  Project Folder : " + projectPath.toAbsolutePath()
        );
        System.out.println(
                "  Java Files     : " + fileCount
        );

        System.out.println(LINE);
    }

    /**
     * Calculates and prints the final quality report.
     *
     * @param fileCount number of analyzed Java files
     * @param warnings all warnings detected in the project
     */
    private static void printReport(
            int fileCount,
            List<C_Warning> warnings
    ) {
        final C_QualityScore qualityScore = new C_QualityScore();
        final int score = qualityScore.calculate(warnings);

        System.out.println();
        System.out.println(LINE);
        System.out.println(
                "|                     QUALITY REPORT                            |"
        );
        System.out.println(LINE);

        System.out.println("Quality Score : " + score + " / 100");
        System.out.println(
                "Rating        : " + qualityScore.getRating(score)
        );

        System.out.println(SECTION_LINE);

        System.out.println(
                "Critical      : "
                        + qualityScore.countCritical(warnings)
        );
        System.out.println(
                "Warnings      : "
                        + qualityScore.countWarnings(warnings)
        );
        System.out.println(
                "Information   : "
                        + qualityScore.countInfo(warnings)
        );

        System.out.println(SECTION_LINE);

        System.out.println("Files Checked : " + fileCount);
        System.out.println("Total Issues  : " + warnings.size());

        System.out.println(SECTION_LINE);

        qualityScore.printMostCommonProblems(warnings);

        System.out.println(LINE);
    }
}
