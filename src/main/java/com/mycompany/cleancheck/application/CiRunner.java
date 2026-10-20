package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.quality.QualityGate;
import com.mycompany.cleancheck.quality.QualityScore;
import com.mycompany.cleancheck.report.HtmlReportGenerator;
import com.mycompany.cleancheck.report.CsvReportExporter;
import com.mycompany.cleancheck.report.JsonReportExporter;
import com.mycompany.cleancheck.report.SarifReportExporter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only command line entry point for CI pipelines.
 * Usage: CiRunner &lt;project&gt; [--json file] [--html file] [--sarif file] [--csv file] [--min-score N] [--fail-on-critical]
 * Exit codes: 0 gate passed, 1 gate failed, 2 usage or I/O error.
 * Separate from CleanCheck on purpose: the GUI calls CleanCheck.main in-process, so it must never exit the JVM.
 * @author KA
 */
public final class CiRunner {
    public static final int EXIT_PASSED = 0;
    public static final int EXIT_GATE_FAILED = 1;
    public static final int EXIT_ERROR = 2;
    private CiRunner() {
    }
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println("Usage: CiRunner <project> [--json file] [--html file] [--sarif file] [--csv file] [--min-score N] [--fail-on-critical]");
            return EXIT_ERROR;
        }
        Path project = Path.of(args[0]);
        Path jsonFile = null;
        Path htmlFile = null;
        Path sarifFile = null;
        Path csvFile = null;
        int minScore = 0;
        boolean failOnCritical = false;
        try {
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--json" -> jsonFile = Path.of(args[++i]);
                    case "--html" -> htmlFile = Path.of(args[++i]);
                    case "--sarif" -> sarifFile = Path.of(args[++i]);
                    case "--csv" -> csvFile = Path.of(args[++i]);
                    case "--min-score" -> minScore = Integer.parseInt(args[++i]);
                    case "--fail-on-critical" -> failOnCritical = true;
                    default -> {
                        err.println("Unknown option: " + args[i]);
                        return EXIT_ERROR;
                    }
                }
            }
        } catch (RuntimeException ex) {
            err.println("Invalid arguments: " + ex.getMessage());
            return EXIT_ERROR;
        }
        try {
            rejectCollidingOutputs(jsonFile, htmlFile, sarifFile, csvFile);
        } catch (IOException ex) {
            err.println("Invalid report destinations: " + ex.getMessage());
            return EXIT_ERROR;
        }
        try {
            AnalysisResult result = AppServices.getInstance().get(ProjectAnalyzer.class).analyze(project);
            QualityScore quality = new QualityScore();
            int score = quality.calculate(result.warnings(), result.totalSourceLines());
            int critical = quality.countCritical(result.warnings());
            if (jsonFile != null) {
                JsonReportExporter.write(result, jsonFile);
            }
            if (sarifFile != null) {
                SarifReportExporter.write(result, sarifFile);
            }
            if (csvFile != null) {
                CsvReportExporter.write(result, csvFile);
            }
            if (htmlFile != null) {
                new HtmlReportGenerator().generateStandalone(result, htmlFile);
            }
            QualityGate gate = new QualityGate(minScore, score, failOnCritical ? critical : 0);
            out.println("CleanCheck CI: files=" + result.javaFiles().size()
                    + " issues=" + result.warnings().size()
                    + " critical=" + critical
                    + " score=" + score
                    + " gate=" + gate.getStatus());
            out.println(gate.getReason());
            return gate.isPassed() ? EXIT_PASSED : EXIT_GATE_FAILED;
        } catch (IOException | RuntimeException ex) {
            // A crash must not look like a failed quality gate (exit 1), so it is reported as exit 2.
            err.println("ERROR: " + ex.getMessage());
            return EXIT_ERROR;
        }
    }

    private static void rejectCollidingOutputs(Path... targets) throws IOException {
        List<Path> selected = new ArrayList<>();
        for (Path target : targets) {
            if (target == null) {
                continue;
            }
            if (target.getFileName() == null) {
                throw new IOException("Report output must be a file: " + target);
            }
            Path normalized = target.toAbsolutePath().normalize();
            for (Path previous : selected) {
                if (sameDestination(normalized, previous)) {
                    throw new IOException("Two report formats target the same file: " + target);
                }
            }
            selected.add(normalized);
        }
    }

    private static boolean sameDestination(Path first, Path second) throws IOException {
        if (first.equals(second)) {
            return true;
        }
        if (Files.exists(first) && Files.exists(second) && Files.isSameFile(first, second)) {
            return true;
        }
        Path firstParent = first.getParent();
        Path secondParent = second.getParent();
        return firstParent != null && secondParent != null
                && first.getFileName().equals(second.getFileName())
                && Files.exists(firstParent) && Files.exists(secondParent)
                && Files.isSameFile(firstParent, secondParent);
    }
}
