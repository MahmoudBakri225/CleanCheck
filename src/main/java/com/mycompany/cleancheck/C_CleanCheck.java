package com.mycompany.cleancheck;

import com.mycompany.cleancheck.quality.C_QualityScore;
import com.mycompany.cleancheck.scanner.C_JavaFileScanner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of the CleanCheck console application.
 * Scans a Java project, runs all registered rules on every .java file,
 * and prints a detailed quality report with score and issue counts.
 *
 * @author KA
 */
public class C_CleanCheck {

    private static final String LINE = "+----------------------------------------------------------------+";
    private static final String SECTION_LINE = "  ----------------------------------------------------------------";

    /**
     * Starts the CleanCheck console application.
     * Accepts an optional project path as the first argument.
     * If no argument is provided, it defaults to the folder "test-project".
     *
     * @param args command-line arguments (optional project path)
     */
    public static void main(String[] args) {
        Path projectPath = Path.of(args.length > 0 ? args[0] : "test-project");

        try {
            C_JavaFileScanner scanner = new C_JavaFileScanner();
            C_RuleEngine engine = new C_RuleEngine();

            List<Path> javaFiles = scanner.scan(projectPath);
            List<C_Warning> allWarnings = new ArrayList<>();

            printHeader(projectPath, javaFiles.size());

            for (int i = 0; i < javaFiles.size(); i++) {
                analyzeFile(scanner, engine, javaFiles.get(i), allWarnings, i + 1);
            }

            printReport(javaFiles.size(), allWarnings);

        } catch (IOException exception) {
            System.out.println("Unable to read project files: " + exception.getMessage());
        }
    }

    /**
     * Analyzes a single Java file, prints its warnings to the console,
     * and adds them to the global list of all warnings.
     *
     * @param scanner     the file scanner used to read the source code
     * @param engine      the rule engine that runs all quality rules
     * @param file        the path of the Java file to analyze
     * @param allWarnings the list that accumulates warnings from all files
     * @param fileNumber  the sequential number of the file (for display)
     * @throws IOException if the file cannot be read
     */
    private static void analyzeFile(C_JavaFileScanner scanner, C_RuleEngine engine,
                                    Path file, List<C_Warning> allWarnings, int fileNumber)
            throws IOException {

        String fileName = file.getFileName().toString();
        List<C_Warning> warnings = engine.analyze(fileName, scanner.readFile(file));
        allWarnings.addAll(warnings);

        System.out.println();
        System.out.println(SECTION_LINE);
        System.out.println(">> FILE " + fileNumber + " : " + fileName);
        System.out.println("Status: " + (warnings.isEmpty()
                ? "No issues found"
                : warnings.size() + " issue(s) found"));
        System.out.println(SECTION_LINE);

        for (C_Warning warning : warnings) {
            System.out.println(warning);
            System.out.println();
        }
    }

    /**
     * Prints the application header with project path and number of Java files found.
     *
     * @param projectPath the root path of the scanned project
     * @param fileCount   the total number of Java files discovered
     */
    private static void printHeader(Path projectPath, int fileCount) {
        System.out.println(LINE);
        System.out.println("|                      CLEAN CHECK                             |");
        System.out.println("|                 Java Code Quality Analyzer                   |");
        System.out.println(LINE);
        System.out.println("  Project Folder : " + projectPath.toAbsolutePath());
        System.out.println("  Java Files     : " + fileCount);
        System.out.println(LINE);
    }

    /**
     * Calculates the quality score and prints the final summary report,
     * including score, rating, severity counts, and most common problems.
     *
     * @param fileCount the total number of files that were analyzed
     * @param warnings  the complete list of warnings found in the project
     */
    private static void printReport(int fileCount, List<C_Warning> warnings) {
        C_QualityScore qualityScore = new C_QualityScore();
        int score = qualityScore.calculate(warnings);

        System.out.println();
        System.out.println(LINE);
        System.out.println("|                     QUALITY REPORT                            |");
        System.out.println(LINE);
        System.out.println("Quality Score : " + score + " / 100");
        System.out.println("Rating        : " + qualityScore.getRating(score));
        System.out.println(SECTION_LINE);
        System.out.println("Critical      : " + qualityScore.countCritical(warnings));
        System.out.println("Warnings      : " + qualityScore.countWarnings(warnings));
        System.out.println("Information   : " + qualityScore.countInfo(warnings));
        System.out.println(SECTION_LINE);
        System.out.println("Files Checked : " + fileCount);
        System.out.println("Total Issues  : " + warnings.size());
        System.out.println(SECTION_LINE);
        qualityScore.printMostCommonProblems(warnings);
        System.out.println(LINE);
    }
}