package com.mycompany.cleancheck.application;
import com.mycompany.cleancheck.analysis.Warning;

import com.mycompany.cleancheck.database.DatabaseInitializer;
import com.mycompany.cleancheck.database.repository.ProjectRepository;
import com.mycompany.cleancheck.database.repository.ScanRepository;
import com.mycompany.cleancheck.database.repository.ScanSummary;
import com.mycompany.cleancheck.database.repository.ScanSummaryRepository;
import com.mycompany.cleancheck.database.service.IssueComparisonService;
import com.mycompany.cleancheck.database.service.ScanPersistenceService;
import com.mycompany.cleancheck.quality.QualityScore;
import com.mycompany.cleancheck.report.RegressionSummary;

import java.nio.file.Path;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CleanCheck {

    private static final String PROJECT_NAME = "CleanCheck";

    private static final String LINE = "+----------------------------------------------------------------+";

    private static final String SECTION_LINE = "  ----------------------------------------------------------------";

    /** Exit codes: 0 success, 2 usage / I-O / analysis failure (1 is kept for CiRunner's failed quality gate). */
    public static final int EXIT_OK = 0;
    public static final int EXIT_ERROR = 2;

    /** What a run produced: the exit code, and the analysis itself when one completed. */
    public record Outcome(int exitCode, AnalysisResult analysis) {
        static Outcome failed(int exitCode) {
            return new Outcome(exitCode, null);
        }
    }

    /**
     * Command-line entry point. Unlike the GUI, which calls {@link #execute(String[])} in-process and
     * must never stop the JVM, this exits with a non-zero code on failure so scripts can detect it.
     */
    public static void main(String[] args) {
        // Started without a project path (for example "Run" in an IDE): open the application.
        if (args.length == 0) {
            com.mycompany.cleancheck.ui.CleanCheckGUI.main(args);
            return;
        }
        int exitCode = run(args);
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args) {
        return execute(args).exitCode();
    }

    /** Runs without ever exiting the JVM. The GUI uses the returned analysis instead of parsing printed text. */
    public static Outcome execute(String[] args) {
        return execute(args, ConsoleOutput.out(), ConsoleOutput.err());
    }

    /** Runs with caller-owned output streams; useful for embedding the analyzer in the desktop UI. */
    public static Outcome execute(String[] args, PrintStream out, PrintStream err) {
        return ConsoleOutput.withStreams(out, err, () -> executeInternal(args));
    }

    private static Outcome executeInternal(String[] args) {

        if (args.length == 0) {
            ConsoleOutput.err().println("ERROR: Please provide a Java project path.");
            ConsoleOutput.err().println("Example: <path-to-your-project> --analyze-only");
            ConsoleOutput.err().println("ANALYSIS_STATUS : FAILED");
            return Outcome.failed(EXIT_ERROR);
        }

        final Path projectPath = Path.of(args[0]).toAbsolutePath().normalize();

        if (!java.nio.file.Files.isDirectory(projectPath)) {
            ConsoleOutput.err().println("ERROR: Project folder does not exist: " + projectPath);
            ConsoleOutput.err().println("ANALYSIS_STATUS : FAILED");
            return Outcome.failed(EXIT_ERROR);
        }

        // The command line is read-only. Analysis never modifies source files; fixes are only
        // attempted from the issue-level GUI action (see VerifiedFix).
        if (args.length > 1 && !"--analyze-only".equalsIgnoreCase(args[1])) {
            ConsoleOutput.err().println("Batch fixing is disabled. Run a read-only scan or use a verified issue-level fix in the GUI.");
            return Outcome.failed(EXIT_ERROR);
        }

        return runAnalysis(projectPath);
    }

    /** Read-only analysis: save the scan, compare with the baseline and print the summary. */
    private static Outcome runAnalysis(Path projectPath) {

        try {

            // STEP 1 - DATABASE INITIALIZATION

            DatabaseInitializer.initializeDatabase();

            ProjectRepository projectRepository = new ProjectRepository();
            String projectName = projectPath.getFileName() == null
                    ? PROJECT_NAME
                    : projectPath.getFileName().toString();

            int projectId = projectRepository.saveProject(
                            projectName,
                            projectPath.toString()
                    );

            if (projectId == -1) {

                ConsoleOutput.out().println(
                        "Unable to save project to database."
                );
                ConsoleOutput.out().println("ANALYSIS_STATUS : FAILED");

                return Outcome.failed(EXIT_ERROR);
            }

            ConsoleOutput.out().println(
                    "Using Project ID: "
                    + projectId
            );

            // STEP 2 - SCANNER + RULE ENGINE

            final ProjectAnalyzer analyzer = new ProjectAnalyzer();

            AnalysisResult initialAnalysis = analyzer.analyze(projectPath);
            List<Path> javaFiles = initialAnalysis.javaFiles();
            List<Warning> initialWarnings = new ArrayList<>(initialAnalysis.warnings());

            printHeader(
                    projectPath,
                    javaFiles.size()
            );

            printAnalysisResult(initialAnalysis);

            // STEP 3 - INITIAL QUALITY SCORE

            QualityScore qualityScore = new QualityScore();

            int initialScore = qualityScore.calculate(initialWarnings, initialAnalysis.totalSourceLines());

            printInitialQuality(initialScore, initialWarnings.size());

            // STEP 4 - SAVE INITIAL SCAN

            ScanRepository scanRepository = new ScanRepository();

            int historicalScanId = scanRepository.getLatestScanId(
                            projectId
                    );

            int currentInitialScanId = new ScanPersistenceService().save(
                            projectId,
                            initialScore,
                            initialWarnings.size(),
                            qualityScore.countCritical(
                                    initialWarnings
                            ),
                            qualityScore.countWarnings(
                                    initialWarnings
                            ),
                            qualityScore.countInfo(
                                    initialWarnings
                            ),
                            initialWarnings
                    );

            ConsoleOutput.out().println();
            ConsoleOutput.out().println("Initial Scan saved successfully!");
            ConsoleOutput.out().println("Historical Scan ID : " + historicalScanId);
            ConsoleOutput.out().println("Initial Scan ID    : " + currentInitialScanId);

            int baselineScanId = resolveBaseline(scanRepository, projectId, currentInitialScanId);

            IssueComparisonService.ComparisonResult comparison
                    = printRegression(baselineScanId, currentInitialScanId);

            printAnalysisOnlySummary(initialWarnings.size());

            // Carry the comparison with the analysis so the HTML report shows the same numbers
            // as the console instead of "Analysis Only".
            return new Outcome(EXIT_OK, initialAnalysis.withRegression(
                    buildRegressionSummary(projectId, baselineScanId, comparison)));

        } catch (Exception exception) {

            ConsoleOutput.out().println(
                    "ERROR: Unable to complete analysis: "
                    + exception.getMessage()
            );
            ConsoleOutput.out().println("ANALYSIS_STATUS : FAILED");
            return Outcome.failed(EXIT_ERROR);
        }
    }

    private static void printInitialQuality(int initialScore, int issueCount) {

        ConsoleOutput.out().println();

        ConsoleOutput.out().println(
                "================ INITIAL QUALITY ================="
        );

        ConsoleOutput.out().println(
                "Initial Score  : "
                + initialScore
                + " / 100"
        );

        ConsoleOutput.out().println(
                "Initial Issues : "
                + issueCount
        );

        ConsoleOutput.out().println(
                "=================================================="
        );
    }

    private static int resolveBaseline(ScanRepository scanRepository, int projectId, int currentInitialScanId) {

        // STEP 6 - BASELINE

        int baselineScanId = scanRepository.getBaselineScanId(
                        projectId
                );

        if (baselineScanId == -1) {

            boolean baselineSaved = scanRepository.setBaseline(
                            projectId,
                            currentInitialScanId
                    );

            if (baselineSaved) {

                baselineScanId = currentInitialScanId;

                ConsoleOutput.out().println();

                ConsoleOutput.out().println(
                        "================ BASELINE ========================="
                );

                ConsoleOutput.out().println(
                        "No baseline existed."
                );

                ConsoleOutput.out().println(
                        "Baseline created from Initial Scan."
                );

                ConsoleOutput.out().println(
                        "Baseline Scan ID : "
                        + baselineScanId
                );

                ConsoleOutput.out().println(
                        "===================================================="
                );

            } else {

                ConsoleOutput.out().println();

                ConsoleOutput.out().println(
                        "Unable to create project baseline."
                );
            }

        } else {

            ConsoleOutput.out().println();

            ConsoleOutput.out().println(
                    "================ BASELINE ========================="
            );

            ConsoleOutput.out().println(
                    "Existing Baseline Scan ID : "
                    + baselineScanId
            );

            ConsoleOutput.out().println(
                    "===================================================="
            );
        }

        return baselineScanId;
    }

    /** @return the comparison, or null when the current scan is itself the baseline (nothing to compare). */
    private static IssueComparisonService.ComparisonResult printRegression(int baselineScanId, int currentInitialScanId) {

        IssueComparisonService.ComparisonResult comparison = null;

        // STEP 7 - REGRESSION VARIABLES

        int regressionNewIssues = 0;

        int regressionResolvedIssues = 0;

        int regressionUnchangedIssues = 0;

        int regressionMovedIssues = 0;

        int regressionChangedIssues = 0;

        int regressionNewCriticalIssues = 0;

        boolean baselineIsCurrentScan = baselineScanId == currentInitialScanId;

        // STEP 8 - REGRESSION ANALYSIS

        if (baselineScanId != -1
                && !baselineIsCurrentScan) {

            ConsoleOutput.out().println();

            ConsoleOutput.out().println(
                    "================ REGRESSION ANALYSIS =============="
            );

            ConsoleOutput.out().println(
                    "Baseline Scan : "
                    + baselineScanId
            );

            ConsoleOutput.out().println(
                    "Initial Scan  : "
                    + currentInitialScanId
            );

            ConsoleOutput.out().println(
                    "----------------------------------------------------"
            );

            IssueComparisonService regressionComparisonService = new IssueComparisonService();

            IssueComparisonService.ComparisonResult regressionResult = regressionComparisonService
                            .compareScans(
                                    baselineScanId,
                                    currentInitialScanId
                            );

            comparison = regressionResult;

            regressionNewIssues = regressionResult.getNewIssues();

            regressionResolvedIssues = regressionResult.getResolvedIssues();

            regressionUnchangedIssues = regressionResult.getUnchangedIssues();

            regressionMovedIssues = regressionResult.getMovedIssues();

            regressionChangedIssues = regressionResult.getChangedIssues();

            regressionNewCriticalIssues = regressionResult
                            .getNewCriticalIssues();

            ConsoleOutput.out().println(
                    "New Issues      : "
                    + regressionNewIssues
            );

            ConsoleOutput.out().println(
                    "Resolved Issues : "
                    + regressionResolvedIssues
            );

            ConsoleOutput.out().println(
                    "Unchanged Issues: "
                    + regressionUnchangedIssues
            );

            ConsoleOutput.out().println(
                    "Moved Issues    : "
                    + regressionMovedIssues
            );

            ConsoleOutput.out().println(
                    "Changed Issues  : "
                    + regressionChangedIssues
            );

            ConsoleOutput.out().println(
                    "New Critical    : "
                    + regressionNewCriticalIssues
            );

            ConsoleOutput.out().println(
                    "===================================================="
            );

        } else {

            ConsoleOutput.out().println();

            ConsoleOutput.out().println(
                    "================ REGRESSION ANALYSIS =============="
            );

            ConsoleOutput.out().println(
                    "Current Initial Scan is the Baseline."
            );

            ConsoleOutput.out().println(
                    "Regression analysis skipped for this scan."
            );

            ConsoleOutput.out().println(
                    "===================================================="
            );
        }

        return comparison;
    }

    private static RegressionSummary buildRegressionSummary(
            int projectId,
            int baselineScanId,
            IssueComparisonService.ComparisonResult comparison) {

        if (comparison == null) {
            return null;
        }

        Integer baselineScore = null;
        try {
            for (ScanSummary scan : new ScanSummaryRepository().findLatest(projectId, 100_000)) {
                if (scan.scanId() == baselineScanId) {
                    baselineScore = scan.score();
                    break;
                }
            }
        } catch (java.sql.SQLException ex) {
            // The score is a nicety; the comparison counts are still valid without it.
            ConsoleOutput.err().println("Could not read the baseline score: " + ex.getMessage());
        }

        return new RegressionSummary(
                baselineScanId,
                baselineScore,
                comparison.getNewIssues(),
                comparison.getResolvedIssues(),
                comparison.getUnchangedIssues(),
                comparison.getMovedIssues(),
                comparison.getChangedIssues(),
                comparison.getNewCriticalIssues());
    }

    private static void printAnalysisOnlySummary(int issueCount) {

            ConsoleOutput.out().println();

            ConsoleOutput.out().println(
                    "================ ANALYSIS ONLY =================="
            );

            ConsoleOutput.out().println(
                    "Auto Fix       : DISABLED"
            );

            ConsoleOutput.out().println(
                    "Files Modified : NO"
            );

            ConsoleOutput.out().println(
                    "Issues Found   : "
                    + issueCount
            );

            ConsoleOutput.out().println(
                    "=================================================="
            );

            ConsoleOutput.out().println("ANALYSIS_STATUS : SUCCESS");
    }

    private static void printAnalysisResult(AnalysisResult result) {
        int fileNumber = 1;
        for (Path file : result.javaFiles()) {
            String relative = result.projectRoot()
                    .relativize(file.toAbsolutePath().normalize())
                    .toString()
                    .replace('\\', '/');

            List<Warning> fileWarnings = result.warnings().stream()
                    .filter(w -> w.getFileName().equals(relative))
                    .toList();

            ConsoleOutput.out().println();
            ConsoleOutput.out().println(SECTION_LINE);
            ConsoleOutput.out().println(">> FILE " + fileNumber + " : " + relative);
            ConsoleOutput.out().println("Status: " + (fileWarnings.isEmpty()
                    ? "No issues found"
                    : fileWarnings.size() + " issue(s) found"));
            ConsoleOutput.out().println(SECTION_LINE);

            for (Warning warning : fileWarnings) {
                ConsoleOutput.out().println(warning);
                ConsoleOutput.out().println();
            }
            fileNumber++;
        }
    }

    // PRINT HEADER

    private static void printHeader(
            Path projectPath,
            int fileCount) {

        ConsoleOutput.out().println(
                LINE
        );

        ConsoleOutput.out().println(
                "|                      CLEAN CHECK                             |"
        );

        ConsoleOutput.out().println(
                "|                 Java Code Quality Analyzer                   |"
        );

        ConsoleOutput.out().println(
                LINE
        );

        ConsoleOutput.out().println(
                "  Project Folder : "
                + projectPath.toAbsolutePath()
        );

        ConsoleOutput.out().println(
                "  Java Files     : "
                + fileCount
        );

        ConsoleOutput.out().println(
                LINE
        );
    }
}
