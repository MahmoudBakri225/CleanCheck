
package com.mycompany.cleancheck;

import com.mycompany.cleancheck.database.C_DatabaseInitializer;
import com.mycompany.cleancheck.database.repository.C_IssueRepository;
import com.mycompany.cleancheck.database.repository.C_ProjectRepository;
import com.mycompany.cleancheck.database.repository.C_ScanHistoryRepository;
import com.mycompany.cleancheck.database.repository.C_ScanRepository;
import com.mycompany.cleancheck.database.service.C_IssueComparisonService;
import com.mycompany.cleancheck.database.service.C_ScanComparisonService;
import com.mycompany.cleancheck.quality.C_QualityScore;
import com.mycompany.cleancheck.scanner.C_JavaFileScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class C_CleanCheck {

    private static final String PROJECT_NAME
            = "CleanCheck";

    private static final String LINE
            = "+----------------------------------------------------------------+";

    private static final String SECTION_LINE
            = "  ----------------------------------------------------------------";

    public static void main(String[] args) {

        final Path projectPath
                = Path.of(
                        args.length > 0
                                ? args[0]
                                : "test-project"
                );

        /*
         * =====================================================
         * ANALYSIS MODE
         * =====================================================
         *
         * When the JavaFX GUI calls CleanCheck with:
         *
         *     --analyze-only
         *
         * the project is analyzed only.
         *
         * No automatic fixes are applied.
         *
         * The normal CLI execution without this argument
         * keeps the complete workflow:
         *
         * Analyze -> Fix -> Analyze Again
         *
         * =====================================================
         */
        final boolean analyzeOnly
                = args.length > 1
                && "--analyze-only".equalsIgnoreCase(
                        args[1]
                );

        try {

            // =====================================================
            // STEP 1 - DATABASE INITIALIZATION
            // =====================================================
            C_DatabaseInitializer.initializeDatabase();

            C_ProjectRepository projectRepository
                    = new C_ProjectRepository();

            int projectId
                    = projectRepository.saveProject(
                            PROJECT_NAME,
                            projectPath.toAbsolutePath().toString()
                    );

            if (projectId == -1) {

                System.out.println(
                        "Unable to save project to database."
                );

                return;
            }

            System.out.println(
                    "Using Project ID: "
                    + projectId
            );

            // =====================================================
            // STEP 2 - SCANNER + RULE ENGINE
            // =====================================================
            final C_JavaFileScanner scanner
                    = new C_JavaFileScanner();

            final C_RuleEngine engine
                    = new C_RuleEngine();

            List<Path> javaFiles
                    = scanner.scan(projectPath);

            List<C_Warning> initialWarnings
                    = new ArrayList<>();

            printHeader(
                    projectPath,
                    javaFiles.size()
            );

            // =====================================================
            // STEP 3 - INITIAL ANALYSIS
            // =====================================================
            for (int i = 0; i < javaFiles.size(); i++) {

                analyzeFile(
                        scanner,
                        engine,
                        javaFiles.get(i),
                        initialWarnings,
                        i + 1
                );
            }

            // =====================================================
            // STEP 4 - INITIAL QUALITY SCORE
            // =====================================================
            C_QualityScore qualityScore
                    = new C_QualityScore();

            int initialScore
                    = qualityScore.calculate(
                            initialWarnings
                    );

            System.out.println();

            System.out.println(
                    "================ INITIAL QUALITY ================="
            );

            System.out.println(
                    "Initial Score  : "
                    + initialScore
                    + " / 100"
            );

            System.out.println(
                    "Initial Issues : "
                    + initialWarnings.size()
            );

            System.out.println(
                    "=================================================="
            );

            // =====================================================
            // STEP 5 - SAVE INITIAL SCAN
            // =====================================================
            C_ScanRepository scanRepository
                    = new C_ScanRepository();

            int historicalScanId
                    = scanRepository.getLatestScanId(
                            projectId
                    );

            int currentInitialScanId
                    = scanRepository.saveScan(
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
                            )
                    );

            if (currentInitialScanId == -1) {

                System.out.println(
                        "Unable to save initial scan."
                );

                return;
            }

            // =====================================================
            // STEP 6 - SAVE INITIAL ISSUES
            // =====================================================
            C_IssueRepository initialIssueRepository
                    = new C_IssueRepository();

            for (C_Warning warning : initialWarnings) {

                initialIssueRepository.saveIssue(
                        currentInitialScanId,
                        warning.getFileName(),
                        warning.getLineNumber(),
                        warning.getRuleName(),
                        warning.getSeverity().toString(),
                        warning.getMessage(),
                        warning.getSuggestion()
                );
            }

            System.out.println();

            System.out.println(
                    "Initial Scan saved successfully!"
            );

            System.out.println(
                    "Historical Scan ID : "
                    + historicalScanId
            );

            System.out.println(
                    "Initial Scan ID    : "
                    + currentInitialScanId
            );

            // =====================================================
            // STEP 7 - BASELINE
            // =====================================================
            int baselineScanId
                    = scanRepository.getBaselineScanId(
                            projectId
                    );

            if (baselineScanId == -1) {

                boolean baselineSaved
                        = scanRepository.setBaseline(
                                projectId,
                                currentInitialScanId
                        );

                if (baselineSaved) {

                    baselineScanId
                            = currentInitialScanId;

                    System.out.println();

                    System.out.println(
                            "================ BASELINE ========================="
                    );

                    System.out.println(
                            "No baseline existed."
                    );

                    System.out.println(
                            "Baseline created from Initial Scan."
                    );

                    System.out.println(
                            "Baseline Scan ID : "
                            + baselineScanId
                    );

                    System.out.println(
                            "===================================================="
                    );

                } else {

                    System.out.println();

                    System.out.println(
                            "Unable to create project baseline."
                    );
                }

            } else {

                System.out.println();

                System.out.println(
                        "================ BASELINE ========================="
                );

                System.out.println(
                        "Existing Baseline Scan ID : "
                        + baselineScanId
                );

                System.out.println(
                        "===================================================="
                );
            }

            // =====================================================
            // STEP 8 - REGRESSION VARIABLES
            // =====================================================
            int regressionNewIssues = 0;

            int regressionResolvedIssues = 0;

            int regressionUnchangedIssues = 0;

            int regressionMovedIssues = 0;

            int regressionChangedIssues = 0;

            int regressionNewCriticalIssues = 0;

            boolean baselineIsCurrentScan
                    = baselineScanId
                    == currentInitialScanId;

            // =====================================================
            // STEP 9 - REGRESSION ANALYSIS
            // =====================================================
            if (baselineScanId != -1
                    && !baselineIsCurrentScan) {

                System.out.println();

                System.out.println(
                        "================ REGRESSION ANALYSIS =============="
                );

                System.out.println(
                        "Baseline Scan : "
                        + baselineScanId
                );

                System.out.println(
                        "Initial Scan  : "
                        + currentInitialScanId
                );

                System.out.println(
                        "----------------------------------------------------"
                );

                C_IssueComparisonService regressionComparisonService
                        = new C_IssueComparisonService();

                C_IssueComparisonService.ComparisonResult regressionResult
                        = regressionComparisonService
                                .compareScans(
                                        baselineScanId,
                                        currentInitialScanId
                                );

                regressionNewIssues
                        = regressionResult.getNewIssues();

                regressionResolvedIssues
                        = regressionResult.getResolvedIssues();

                regressionUnchangedIssues
                        = regressionResult.getUnchangedIssues();

                regressionMovedIssues
                        = regressionResult.getMovedIssues();

                regressionChangedIssues
                        = regressionResult.getChangedIssues();

                regressionNewCriticalIssues
                        = regressionResult
                                .getNewCriticalIssues();

                System.out.println(
                        "New Issues      : "
                        + regressionNewIssues
                );

                System.out.println(
                        "Resolved Issues : "
                        + regressionResolvedIssues
                );

                System.out.println(
                        "Unchanged Issues: "
                        + regressionUnchangedIssues
                );

                System.out.println(
                        "Moved Issues    : "
                        + regressionMovedIssues
                );

                System.out.println(
                        "Changed Issues  : "
                        + regressionChangedIssues
                );

                System.out.println(
                        "New Critical    : "
                        + regressionNewCriticalIssues
                );

                System.out.println(
                        "===================================================="
                );

            } else {

                System.out.println();

                System.out.println(
                        "================ REGRESSION ANALYSIS =============="
                );

                System.out.println(
                        "Current Initial Scan is the Baseline."
                );

                System.out.println(
                        "Regression analysis skipped for this scan."
                );

                System.out.println(
                        "===================================================="
                );
            }

            // =====================================================
            // ANALYZE-ONLY MODE
            // =====================================================
            //
            // Used by the JavaFX GUI Start Analysis button.
            //
            // IMPORTANT:
            // Stop here before the Fix Engine.
            //
            // This guarantees that pressing Start Analysis
            // does NOT modify the Java source files.
            //
            // Fixes are applied only through the selected
            // issue -> Apply Fix workflow in the GUI.
            // =====================================================
            if (analyzeOnly) {

                System.out.println();

                System.out.println(
                        "================ ANALYSIS ONLY =================="
                );

                System.out.println(
                        "Auto Fix       : DISABLED"
                );

                System.out.println(
                        "Files Modified : NO"
                );

                System.out.println(
                        "Issues Found   : "
                        + initialWarnings.size()
                );

                System.out.println(
                        "=================================================="
                );

                return;
            }

            // =====================================================
            // STEP 10 - FIX ENGINE
            // =====================================================
            System.out.println();

            System.out.println(
                    "================ FIX ENGINE ======================="
            );

            C_FixEngine fixEngine
                    = new C_FixEngine();

            int totalFixes = 0;

            for (Path javaFile : javaFiles) {

                List<C_FixResult> fixResults
                        = fixEngine.applyFixes(
                                javaFile
                        );

                if (!fixResults.isEmpty()) {

                    for (C_FixResult result
                            : fixResults) {

                        System.out.println(
                                "File  : "
                                + result.getFileName()
                        );

                        System.out.println(
                                "Rule  : "
                                + result.getRuleName()
                        );

                        System.out.println(
                                "Fixed : "
                                + (result.isFixed()
                                ? "YES"
                                : "NO")
                        );

                        System.out.println(
                                "------------------------------------------------------------"
                        );

                        if (result.isFixed()) {
                            totalFixes++;
                        }
                    }
                }
            }

            System.out.println(
                    "Total Fixes Applied : "
                    + totalFixes
            );

            System.out.println(
                    "=================================================="
            );

            // =====================================================
            // STEP 11 - AFTER FIX SCAN
            // =====================================================
            System.out.println();

            System.out.println(
                    "================ AFTER FIX SCAN =================="
            );

            javaFiles
                    = scanner.scan(projectPath);

            List<C_Warning> finalWarnings
                    = new ArrayList<>();

            for (int i = 0;
                    i < javaFiles.size();
                    i++) {

                analyzeFile(
                        scanner,
                        engine,
                        javaFiles.get(i),
                        finalWarnings,
                        i + 1
                );
            }

            // =====================================================
            // STEP 12 - FINAL SCORE
            // =====================================================
            int finalScore
                    = qualityScore.calculate(
                            finalWarnings
                    );

            int resolvedInRun
                    = initialWarnings.size()
                    - finalWarnings.size();

            int scoreDifference
                    = finalScore - initialScore;

            System.out.println();

            System.out.println(
                    "================ FIX RESULT ====================="
            );

            System.out.println(
                    "Before Score : "
                    + initialScore
                    + " / 100"
            );

            System.out.println(
                    "After Score  : "
                    + finalScore
                    + " / 100"
            );

            System.out.println(
                    "Score Change : "
                    + (scoreDifference >= 0
                            ? "+"
                            : "")
                    + scoreDifference
            );

            System.out.println(
                    "Before Issues: "
                    + initialWarnings.size()
            );

            System.out.println(
                    "After Issues : "
                    + finalWarnings.size()
            );

            System.out.println(
                    "Issues Removed: "
                    + resolvedInRun
            );

            System.out.println(
                    "=================================================="
            );

            // =====================================================
            // STEP 13 - SAVE FINAL SCAN
            // =====================================================
            int finalScanId
                    = scanRepository.saveScan(
                            projectId,
                            finalScore,
                            finalWarnings.size(),
                            qualityScore.countCritical(
                                    finalWarnings
                            ),
                            qualityScore.countWarnings(
                                    finalWarnings
                            ),
                            qualityScore.countInfo(
                                    finalWarnings
                            )
                    );

            if (finalScanId == -1) {

                System.out.println(
                        "Unable to save final scan to database."
                );

                return;
            }

            // =====================================================
            // STEP 14 - SAVE FINAL ISSUES
            // =====================================================
            C_IssueRepository finalIssueRepository
                    = new C_IssueRepository();

            for (C_Warning warning : finalWarnings) {

                finalIssueRepository.saveIssue(
                        finalScanId,
                        warning.getFileName(),
                        warning.getLineNumber(),
                        warning.getRuleName(),
                        warning.getSeverity().toString(),
                        warning.getMessage(),
                        warning.getSuggestion()
                );
            }

            System.out.println();

            System.out.println(
                    "Database saved successfully!"
            );

            System.out.println(
                    "Project ID          : "
                    + projectId
            );

            System.out.println(
                    "Baseline Scan ID    : "
                    + baselineScanId
            );

            System.out.println(
                    "Historical Scan ID  : "
                    + historicalScanId
            );

            System.out.println(
                    "Initial Scan ID     : "
                    + currentInitialScanId
            );

            System.out.println(
                    "Final Scan ID       : "
                    + finalScanId
            );

            // =====================================================
            // STEP 15 - FIX VERIFICATION
            // =====================================================
            System.out.println();

            System.out.println(
                    "================ FIX VERIFICATION ================"
            );

            C_IssueComparisonService fixComparisonService
                    = new C_IssueComparisonService();

            C_IssueComparisonService.ComparisonResult fixComparisonResult
                    = fixComparisonService.compareScans(
                            currentInitialScanId,
                            finalScanId
                    );

            System.out.println(
                    "===================================================="
            );

            // =====================================================
            // STEP 16 - QUALITY EVOLUTION
            // =====================================================
            System.out.println();

            System.out.println(
                    "================ QUALITY EVOLUTION ==============="
            );

            C_ScanComparisonService scanComparisonService
                    = new C_ScanComparisonService();

            scanComparisonService.compareLatestScans(
                    projectId
            );

            // =====================================================
            // STEP 17 - QUALITY GATE
            // =====================================================
            System.out.println();

            System.out.println(
                    "================ QUALITY GATE ================="
            );

            if (baselineScanId == -1
                    || baselineIsCurrentScan) {

                System.out.println(
                        "Status         : SKIPPED"
                );

                System.out.println(
                        "Reason         : "
                        + "Current scan established the baseline."
                );

            } else {

                Integer baselineQualityScore
                        = getScanScore(
                                baselineScanId
                        );

                if (baselineQualityScore == null) {

                    System.out.println(
                            "Status         : SKIPPED"
                    );

                    System.out.println(
                            "Reason         : "
                            + "Unable to read baseline score."
                    );

                } else {

                    int previousQualityScore
                            = baselineQualityScore;

                    int currentQualityScore
                            = finalScore;

                    int newCriticalIssues
                            = regressionNewCriticalIssues;

                    C_QualityGate qualityGate
                            = new C_QualityGate(
                                    previousQualityScore,
                                    currentQualityScore,
                                    newCriticalIssues
                            );

                    System.out.println(
                            "Baseline Score : "
                            + qualityGate
                                    .getPreviousScore()
                    );

                    System.out.println(
                            "Current Score  : "
                            + qualityGate
                                    .getCurrentScore()
                    );

                    System.out.println(
                            "New Critical   : "
                            + qualityGate
                                    .getNewCriticalIssues()
                    );

                    System.out.println(
                            "Final Critical : "
                            + qualityScore
                                    .countCritical(
                                            finalWarnings
                                    )
                    );

                    System.out.println(
                            "Status         : "
                            + qualityGate
                                    .getStatus()
                    );

                    System.out.println(
                            "Reason         : "
                            + qualityGate
                                    .getReason()
                    );
                }
            }

            System.out.println(
                    "================================================"
            );

            // =====================================================
            // STEP 18 - QUALITY TREND
            // =====================================================
            System.out.println();

            System.out.println(
                    "================ QUALITY TREND ===================="
            );

            C_ScanHistoryRepository historyRepository
                    = new C_ScanHistoryRepository();

            historyRepository.printQualityTrend(
                    projectId
            );

            // =====================================================
            // STEP 19 - RISK HOTSPOTS
            // =====================================================
            System.out.println();

            C_RiskHotspotService riskHotspotService
                    = new C_RiskHotspotService();

            riskHotspotService.printRiskHotspots(
                    initialWarnings
            );

            System.out.println(
                    "================================================"
            );

            // =====================================================
            // STEP 20 - HTML REPORT
            // =====================================================
            final List<String> fileNames
                    = new ArrayList<>();

            for (Path file : javaFiles) {

                fileNames.add(
                        file.getFileName().toString()
                );
            }

            final C_HtmlReportGenerator htmlReport
                    = new C_HtmlReportGenerator();

            final String reportPath
                    = htmlReport.generate(
                            PROJECT_NAME,
                            fileNames,
                            finalWarnings,
                            initialWarnings,
                            baselineScanId != -1
                                    ? getScanScore(
                                            baselineScanId
                                    )
                                    : null,
                            initialScore,
                            regressionNewIssues,
                            regressionResolvedIssues,
                            regressionUnchangedIssues,
                            fixComparisonResult
                                    .getNewIssues(),
                            fixComparisonResult
                                    .getResolvedIssues(),
                            fixComparisonResult
                                    .getUnchangedIssues(),
                            finalScore,
                            projectId
                    );

            System.out.println();

            System.out.println(
                    "HTML report generated: "
                    + reportPath
            );

        } catch (IOException exception) {

            System.out.println(
                    "Unable to read project files: "
                    + exception.getMessage()
            );
        }
    }

    // =============================================================
    // GET SCAN SCORE
    // =============================================================
    private static Integer getScanScore(
            int scanId) {

        String sql
                = """
                SELECT QualityScore
                FROM Scan
                WHERE ScanId = ?
                """;

        try (
                java.sql.Connection connection
                = com.mycompany.cleancheck.database.C_DatabaseConnection
                        .getConnection();
                java.sql.PreparedStatement statement
                = connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    scanId
            );

            try (
                    java.sql.ResultSet resultSet
                    = statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getInt(
                            "QualityScore"
                    );
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to get scan score!"
            );

            e.printStackTrace();
        }

        return null;
    }

    // =============================================================
    // ANALYZE FILE
    // =============================================================
    private static void analyzeFile(
            C_JavaFileScanner scanner,
            C_RuleEngine engine,
            Path file,
            List<C_Warning> allWarnings,
            int fileNumber)
            throws IOException {

        final String fileName
                = file.getFileName().toString();

        final List<C_Warning> warnings
                = engine.analyze(
                        fileName,
                        scanner.readFile(file)
                );

        allWarnings.addAll(
                warnings
        );

        System.out.println();

        System.out.println(
                SECTION_LINE
        );

        System.out.println(
                ">> FILE "
                + fileNumber
                + " : "
                + fileName
        );

        System.out.println(
                "Status: "
                + (warnings.isEmpty()
                ? "No issues found"
                : warnings.size()
                + " issue(s) found")
        );

        System.out.println(
                SECTION_LINE
        );

        for (C_Warning warning
                : warnings) {

            System.out.println(
                    warning
            );

            System.out.println();
        }
    }

    // =============================================================
    // PRINT HEADER
    // =============================================================
    private static void printHeader(
            Path projectPath,
            int fileCount) {

        System.out.println(
                LINE
        );

        System.out.println(
                "|                      CLEAN CHECK                             |"
        );

        System.out.println(
                "|                 Java Code Quality Analyzer                   |"
        );

        System.out.println(
                LINE
        );

        System.out.println(
                "  Project Folder : "
                + projectPath.toAbsolutePath()
        );

        System.out.println(
                "  Java Files     : "
                + fileCount
        );

        System.out.println(
                LINE
        );
    }
}

