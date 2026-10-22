## Delivery package (2026-10-22)
- Source tests (`src/test`) omitted from this package for the current handoff stage.

## Delivery hardening (2026-10-22)
- Web API no longer returns raw exception messages; details stay in the server log.
- Local Host checks accept loopback IPv6 forms (`::1`, `[::1]`) in addition to localhost/127.0.0.1.
- GUI analysis failures no longer dump stack traces into captured console output.
- SQLite foreign keys use ON DELETE CASCADE; connections enable WAL and shared cache.
- Source scan caps directory depth and Java file count; skips node_modules/.idea/.svn.
- ProjectAnalyzer reuses SourceDiscovery for exclude globs (single implementation).

## Static review follow-up (2026-10-20)
- Hardened Naming Convention auto-fix so multiline comments and text blocks are masked across the complete source before replacing identifiers.
- Tightened local web server Host validation to allow only localhost/127.0.0.1 with an optional numeric port in the valid TCP port range.

## Static-review corrections (2026-10-20)

- Scoped interactive data-type fixes to the selected line instead of silently changing every matching token in a file.
- Applied type-token replacements right-to-left so the longer `string` → `String` replacement cannot shift offsets and corrupt later matches.
- Made missing-package detection ignore comments and string literals.
- Reduced the required Java toolchain to Java 21 / JavaFX 21.0.8.
- Replaced stale release-verification claims with an explicit static-review-only status; no tests or build were run for this revision.

# CleanCheck changelog

## Unreleased - Reliability and analyzer hardening
- Verified fixes restore the original source if analysis or compilation verification throws; issue comparison now detects severity escalations and additional duplicate findings.
- Scans and automatic fixes reject symbolic links and paths outside the selected project; backup directories are unique even for rapid consecutive fixes.
- Ignore directives are recognized only in Java line comments, and infinite-loop checks distinguish exits from nested loops, switches, and lambdas.
- Parser fallback normalizes likely type misspellings in code while preserving comments, string literals, and text blocks.
- Database repository failures now propagate instead of masquerading as missing data; baseline selection checks scan ownership, and SQLite location can be overridden for isolated runs.
- A completed scan and its findings are committed in one transaction; any finding-write failure rolls the entire scan back.
- Every SQLite connection enables foreign-key enforcement and waits briefly for concurrent writers instead of failing immediately.
- Project exclude globs are validated up front; invalid patterns now identify themselves instead of surfacing a raw provider error.
- The analyzer resolves `cleancheck.properties` from the selected project root (or an explicit JVM override), so GUI and CLI scans use the project's actual settings.
- Existing configuration paths that are unreadable or not regular files now fail clearly instead of silently reverting to defaults.
- Duplicate-code fingerprints strip comments lexically while preserving string and text-block contents, avoiding false matches on URLs or embedded queries.
- Parser fallback now recognizes escaped triple quotes inside Java text blocks, so type normalization cannot alter later text-block content.
- Quality gates reject scores outside 0–100 and negative critical counts, keeping status and explanation consistent.
- Report exporters refuse destinations that resolve to an analyzed Java source file, including hard-link aliases, preventing accidental source destruction.
- Java parsing now uses a fresh JavaParser instance per call, avoiding shared mutable parser state when scans run concurrently.
- SQL injection checks now follow dynamic `StringBuilder` / `StringBuffer` appends into `toString()` queries within the same method while ignoring literal-only builders.
- Automatic fixes preserve the source file's detected UTF-8 or ISO-8859-1 encoding, matching scanner decoding and avoiding corruption in legacy files.
- Duplicate-code fingerprints now normalize whitespace only outside string, character, and text-block literals, avoiding false matches when literal contents differ by whitespace.
- Fix verification counts repeated compiler diagnostics, so adding another error with the same message in the same file is still treated as a regression.
- Verified fixes now discard their temporary snapshot after no-op, rejection, or successful restore; only accepted changes retain a rollback point.
- Rolling back an accepted UI fix also removes its now-obsolete snapshot; a cleanup failure is reported separately after the source is safely restored.
- Baseline ownership validation and insert/update now happen in one SQLite upsert, preventing competing scans from racing on baseline creation.
- Project registration is now an atomic path-keyed upsert; initialization merges legacy duplicate-path rows and preserves their scans and a valid baseline before enforcing uniqueness.
- CI report arguments are checked for colliding destinations before analysis begins, including distinct hard-link paths to the same file.
- The local web UI accepts only exact GET routes, limits worker concurrency/queue size, emits browser safety headers, and HTML-escapes project paths.
- SQL injection checks now require a recognized database receiver, eliminating false alarms on unrelated `execute(...)` application methods while retaining JDBC `Statement.execute` detection.
- Path traversal checks now focus on file-I/O sinks, do not treat a bare path factory or explicit CLI path as a traversal flaw, and require both normalization and base containment before suppressing a finding.
- Analysis results now carry source-line counts from the same decoded file snapshot, so scoring and regression reports do not reread potentially changed files.
- Each file's AST is now parsed once and reused by its enabled rules, with per-thread isolation and explicit cache cleanup after analysis.
- JavaParser now targets Java 26 language syntax, matching the compiler target and recognizing current compact source files.
- Scanning now rejects a symbolic-link project root explicitly instead of treating the unresolved link as an empty, apparently clean project.
- HTML, JSON, SARIF and CSV exports now write a complete UTF-8 sibling file before replacing the destination, preserving the prior report when generation or replacement fails.
- CI gate explanations now say whether the minimum score was missed or critical findings were present; they no longer imply an unavailable comparison with a prior scan.
- File scanning now prunes excluded build and Git directories before descending into them, while preserving source packages whose names match build folders.
- The GUI uses per-run output streams instead of changing `System.out`; CSV exports mark formula-like cells as text.
- Risk hotspot and legacy scan comparison output follows the active run stream; scan comparison failures now propagate instead of looking like successful empty output.
- UI rollback is now tied to the exact project and source file that received the fix, and is disabled during analysis or another fix.
- Added cross-platform CI, dependency update checks, engineering conventions, and regression coverage.

## v14 - Fixes from the first full test run
- Dashboard: a score of 0 now shows `0` instead of the empty-state dash.
- Metrics: "Riskiest rules / files" and "Where to start" rank by the worst severity first, then by total risk, and the
  bar colour follows the severity. Many Info findings can no longer outrank a critical one.
- Magic Number now also detects decimal literals such as `99.99` (0.0, 0.5, 1.0 and 2.0 stay allowed).
- Weak Cryptography: cipher findings (AES without a mode, ECB, DES/RC4...) get their own fix sentence instead of the hash advice.
- Security Review: the findings table keeps its full height instead of collapsing to one row.
- Issues: the Magic Number explanation no longer mentions a fixed number (18).

## v13 - Security Review, and no dependency on example folders
- **Security Review tab** next to Issues: a preliminary, AST-based security check with 10 rules. Every finding carries
  its CWE number and OWASP Top 10 (2021) category, how the weakness is exploited, the safe fix and a before/after example.
  Rules: SQL Injection (CWE-89), Hardcoded Secret (CWE-798), Weak Cryptography (CWE-327), Insecure Random (CWE-338),
  Command Injection (CWE-78), Path Traversal (CWE-22), Unsafe Deserialization (CWE-502), Disabled Certificate Check
  (CWE-295), Information Leak (CWE-209), XML External Entity (CWE-611).
- A security grade (A-F) and score from the open security findings, shown as "preliminary". Same severity levels as the
  other rules; the Issues filter has a new "Security only" entry.
- A small same-method taint check: when user input (request parameter, console line, command-line argument) visibly
  reaches a query or command in the same method, the message says where it came from. It never follows calls between
  methods or files; the UI and reports state this limit. Secret values are never printed in findings.
- Reports: HTML has a Security Review section; SARIF carries CWE and OWASP tags, `helpUri` and `security-severity`
  (GitHub Code Scanning ranks them); JSON adds `security` and per-issue `cwe` / `owasp`.
- One automatic fix: `e.printStackTrace();` becomes a `java.util.logging` call (verified by the usual compile check).
  The other security findings stay manual because they need a decision.
- Security rules are ordinary rules: turn them off with `rules.disabled`, change severity, or silence a line with
  `// cleancheck:ignore SQL Injection`.
- **Removed everything tied to `sample-projects`:** the "Try a sample" button, `--verify-samples`, `VerificationRunner`
  and `SamplesLocator`. CleanCheck now works only on the folder you choose (one project or several). Running
  `CleanCheck` without arguments still opens the GUI. CI no longer runs the sample verification and smoke-checks the jar
  on `src/main/java` instead.
- `docs/security-demo/VulnerableDemo.java`: an intentionally insecure file for demonstrating the tab. It is not used by
  any code or build step.
- NOT verified here: this change was written without a JDK, Maven or JavaFX available, so it has not been compiled or
  run. Build it and click through Security Review once.

## v11 - Home and code review
- **Home redesigned.** Gradient hero card with the promise, the main button and a preview of what a result looks like
  (score, counts, three typical findings), a four-step "How it works" row, and the recent projects. A project folder can
  be dropped anywhere on the hero card.
- **Card rows fixed on every page.** Rows of cards (Home, Analyze, Metrics, Regression, Class Mode) used an HBox whose
  cards were measured at the wrong width, so wrapped text made them very tall. They now use a GridPane with percentage
  columns (`UiKit.cardRow`, `UiKit.split`).
- **Code review on the Issues page.** Replaces the plain text box: syntax-coloured Java with line numbers, the finding's
  line highlighted by severity, the rule's message and fix right under the line, gutter dots for the other findings in the
  same file, "Show more / Show less" (a wide window around the finding, scrollable) and a Copy button. It now sits under
  the table, next to the explanation panel. Logic is UI-free and tested (`JavaSyntax`, `CodeSnippet`,
  `CodeReviewLogicTest`); the Java tokenizer keeps block-comment state even when the window starts inside a comment.
- Verified here: compiles against JavaFX stubs; tokenizer and snippet logic ran in a plain-Java harness. NOT run here:
  the GUI itself, Maven, JUnit. Click through Home and Issues once to confirm the look.

## v10 - interface redesign
- **Issues explain themselves.** The details panel now shows, for the selected finding: what is wrong (the rule's own
  message plus a plain explanation), why it matters, numbered fix steps, a before/after code example and who can fix it
  (safe automatic fix or manual). All text lives in `RuleKnowledge` and covers all 24 rules; before, the 11 newer rules had
  no learning note at all and the wrapped note was cut off with an ellipsis.
- **Home:** "Recent Projects" now lists projects you really opened (saved in `~/.cleancheck/recent-projects.txt`, newest
  first, max 6) instead of the two hard-coded samples. The bundled sample is offered only while the history is empty.
  Drag and drop of a project folder now works (it was advertised but never implemented). Feature tiles added.
- **Analyze:** shows the selected project, the three steps, the rule areas and a safety model whose auto-fix list is
  generated from the code (it was out of date: three fixable rules were missing).
- **Metrics:** stat cards, bar lists for the riskiest files and rules, and a "Where to start" card.
- **Regression:** verdict banner (improved / regressed / stable / first scan), previous vs latest score, score and issue
  change, and a score trend. Logic is in `RegressionInsight` (tested).
- **Class Mode:** class summary cards, medal ranking with score bars and "needs attention" flags, the mistakes the class
  repeats ("teach next"), and per-student findings with their fix. Logic is in `ClassInsights` (tested).
- **Rules, Fix Verification, About:** generated from `RuleKnowledge` (24 rules in 6 categories, 9 with verified auto-fix),
  so they cannot go stale again.
- Tests: `UiLogicTest` (rule coverage against `RuleEngine`, auto-fix flags against `FixEngine`, recent projects,
  regression verdicts, class insights).
- Verified here: all new UI code compiles against hand-written JavaFX stubs, and the UI-free logic ran in a plain-Java
  harness. NOT run here: the GUI itself (no JavaFX), Maven, JUnit. Run `mvn -B verify`, then `mvn javafx:run` and click
  through every page. Dashboard, Quality History and Scan Results keep their previous look.

## v9
- **HTML report shows the baseline comparison.** `AnalysisResult` now carries an optional `RegressionSummary`
  (baseline scan id and score, new / resolved / unchanged / moved / changed / new-critical counts). `CleanCheck.execute`
  fills it from the same comparison it prints, so the report and the console show the same numbers. Before, the
  standalone report always said "Analysis Only" even when a baseline existed, because it never saw the comparison.
- GUI **Report** button: re-analyzes read-only, then attaches the last Analyze run's comparison only when the findings
  are identical (`AnalysisResult.withRegressionFrom`), so stale regression numbers can never be exported.
- Regression and Fix Verification are now independent sections: a standalone report shows Regression when it has a
  comparison and never shows Fix Verification (no fix run). Reports without a comparison keep the "Analysis Only" note.
- `CiRunner --html` is unchanged and database-free, so its report is always "Analysis Only".
- Tests: 2 new in `HtmlReportTest`. Verified here with a plain-Java harness (compiled with `-Xlint:all`, no warnings):
  report with and without comparison, stale comparison rejected, legacy `generateTo` path unchanged.
  NOT run here: Maven, JUnit, JavaParser-dependent code, JavaFX, SQLite. Run `mvn -B verify` first.
- Docs: removed the duplicated v5 changelog section and duplicated status paragraph; corrected the rule count (22 + Duplicate
  Code), the fix-verification limitation (javac check exists) and the GUI size.

## v7
- **Report fix**: the standalone HTML report (GUI Report button, CLI `--html`, CI) no longer prints zero-filled
  "Regression Analysis" and "Fix Verification" cards. Without a baseline or a fix run it shows an "Analysis Only" note.
- **New automatic fix: Unused Variable.** Literal / plain-name initializers are removed with the variable; a method call or
  `new` initializer is kept as a statement so side effects still run. Skipped when the name appears elsewhere in the file,
  when the declaration is a braceless if/else/loop body, or for multi-variable declarations.
- Tests added for both (`HtmlReportTest`, `FixEngineTest`).
- **GUI split (2244 -> 1716 lines in `CleanCheckGUI.java`)**: removed two unused methods (`scoreCard`, `statCard`);
  the Dashboard (page, clear, update, counters) moved to `DashboardController`; the code preview moved to the
  JavaFX-free `CodePreview` (new `CodePreviewTest`); unused imports removed. Behaviour is unchanged.

## v6
- Parse failures are reported as a CRITICAL "Parse Error" instead of a silent "No issues found".
- 9 new rules (Unused Import, Unused Variable, Cyclomatic Complexity, Deep Nesting, String Comparison, Generic Catch, Unclosed Resource, Equals HashCode, Class Length) and a cross-file Duplicate Code rule.
- cleancheck.properties: thresholds, severity overrides, exclude globs; `// cleancheck:ignore` comments.
- Verified fixes also run a javac compile check; new fixers for Unused Import and String Comparison (logic in `VerifiedFix`).
- `CleanCheck.run/execute` with exit codes; the GUI reads the analysis object instead of parsing printed text.
- SARIF and CSV exports, runnable jar (`Launcher`, shade plugin), run-gui.sh, jpackage scripts.

## v5 — review fixes (static review of v4)

- **JavaParser language level**: `JavaAst` now sets `LanguageLevel.JAVA_21` explicitly. Under the library default, files
  using records, arrow-switch or text blocks could fail to parse and were silently reported as "no issues"
  (CleanCheck's own source uses all three).
- **Data Type rule**: the line scan read raw text, so `"Enter a string"` was reported as a CRITICAL mistyped type.
  It now scans `JavaSourceMask.maskNonCode(source)` (comments and literals blanked, line numbers kept). That class
  existed but nothing called it.
- **Data Type fix**: `FixEngine` replaced `string`/`Int`/`Char` anywhere on the line, including inside string literals and
  comments, which changed user-facing text. It now edits code only.
- **JavaFileScanner**: results are sorted (`Files.walk` order is unspecified); `build`/`out`/`target` are skipped only
  outside a `src` tree, so a package called `build` or `out` is no longer ignored (`.git` and `.cleancheck-backups` are
  still skipped everywhere); `readFile` falls back to ISO-8859-1 for non-UTF-8 files instead of aborting the whole scan.
- **CiRunner**: any `RuntimeException` now exits with 2 (error). Before, a crash exited with 1, the same as "quality gate failed".
- **Reproducible verification**: `ProjectAnalyzer` accepts a `RuleEngine`; `--verify-samples` uses all rules, and surefire
  points `cleancheck.config` at a missing file, so a local `cleancheck.properties` can no longer change test or
  verification results.
- `HtmlReportGenerator.safeFileName` keeps letters of any script (an Arabic project folder no longer becomes `_____`).
- New tests: 3 in `RulesTest`, 2 in `FixEngineTest`, 4 in `JavaFileScannerTest`, 1 in `CiAndConfigTest`.
- Executed here with minimal JUnit stubs: `FixEngineTest` (23), `JavaFileScannerTest` (6), `QualityTest` (8): 37 passed.
  NOT executed (need JavaParser, absent here): the 3 new `RulesTest` tests and the new `CiAndConfigTest` test.
  `JavaAst` and `DataTypeRule` were checked only for compile errors unrelated to the missing library.
- Not changed, noted for later: `InfiniteLoopRule` counts any `break` in the body, including one that only leaves a
  nested `switch` or loop; the `CleanCheck` CLI prints `ANALYSIS_STATUS : FAILED` but exits 0 (the GUI calls `main`
  in-process, so a real exit code needs a separate entry point like `CiRunner`).

## v4 — HTML report wired into the product

- `HtmlReportGenerator`: the report is no longer orphaned. New `generateTo(Path, ...)` writes to an explicit file, and
  `generateStandalone(AnalysisResult, Path)` exports a finished analysis in one call (no database, no baseline).
  The old `generate(...)` keeps its signature and default `reports/<timestamp>/html/` location.
- The Quality History section is skipped when `projectId <= 0`, so the report works without SQLite.
- Report file names are sanitized (`safeFileName`) because project names come from folder names.
- CLI: `CiRunner <project> --html report.html` (combines with `--json`, `--min-score`, `--fail-on-critical`).
- GUI: new **Report** button in the header opens a save dialog and exports the HTML report. It re-analyzes read-only
  and never edits source files.
- New `HtmlReportTest` (4 tests); `FixEngineTest` grew from 9 to 21 tests. Three of them (file written, history skipped, HTML escaping) were executed with
  minimal JUnit stubs and passed. The CiRunner `--html` test needs JavaParser and has not been run here.
- Docs: merged `CHANGES_V2.md`, `CHANGES_V3.md`, `FINAL_VERIFICATION.md`, `docs/final-version.md` and
  `docs/final-committee-status.md` into this file and `docs/delivery-status.md`. Corrected the stale "10 rules" claim (there are 13).
- **`mvn verify` would have failed**: `FixEngineTest.unsupportedRuleLeavesFileUntouched` used "Magic Number" as an
  unsupported rule, but `FixEngine` had gained a Magic Number fix after the test was written. The test now uses a rule
  that really is unsupported, and `supportsAutomaticFix...` asserts the six fixable rules.
- `FixEngine` hardened (each point has a test that fails on the old code and passes now):
  - Magic Number: digits inside strings/comments are no longer replaced; the constant is declared once (a second fix
    reuses `VALUE_n`); it is inserted after the class declaration, not after the first `{` in the file; skipped for
    files with several types, interfaces and enums.
  - System Out: not removed when it is the body of a braceless `if/else/for/while/do` (removal would have made the
    next statement the body).
  - Naming Convention: renames the field **and its uses** in the file (before, only the declaration changed and the
    file stopped compiling); skipped if the new name already exists, the old name is also a type/method name, it is
    read through another object, or a neighbouring class uses it; never rewrites statements like `return X;`.
  - Encapsulation: skipped when another class in the same folder reads the field as `obj.field`.
  - Missing Package: the folder-name fallback can no longer produce an illegal package such as `2024` or `default`.
- Known design weakness, not changed: the GUI runs `CleanCheck.main`, captures its printed text and parses it
  (`IssueOutputParser`). A structured result object would be sturdier than parsing console output.

## v3 — what changed (still NOT compiled or run by the author of these changes)

- Removed 14 NetBeans template headers.
- Replaced every e.printStackTrace() in src/main with java.util.logging (WARNING).
- Moved the 10 print-only database "tests" (they had main() but no @Test and wrote to the real DB) to dev-tools/manual-db-checks/ as .java.txt reference files.
- FinalCoreSmokeTest is now a real JUnit 5 test, so mvn test runs it.
- CI step that runs CleanCheck on its own source is labelled report-only.
- Split HtmlReportGenerator: sections moved to RiskHotspotsSection, IssuesSection, QualityHistorySection, ReportFormat. Verified: compiled with -Xlint:all and the generated HTML is byte-identical to the original on a sample.
- Split IssueComparisonService: matching logic -> IssueMatcher, SQL loading -> IssueLoader, IssueData / IssueChange are now top-level package-private classes. Public API (IssueComparisonService.ComparisonResult, compareScans) unchanged. Verified: the whole database package compiles; the methods were moved unchanged (only made static). No SQLite driver was available, so it was not run.
- CleanCheck.java (1042 -> about 600 lines): steps 10-20 (fix engine, final scan, quality gate, trend, risk hotspots, HTML report) were UNREACHABLE: `analyzeOnly` was always true and the code returned before them. That dead code was removed and the live steps became methods (printInitialQuality, saveInitialIssues, resolveBaseline, printRegression, printAnalysisOnlySummary). Printed text is unchanged. Verified: compiles (stubs for JavaParser classes) and the argument-validation outputs match the original; the database path could not be run (no SQLite driver here).
- IMPORTANT: because of that, HtmlReportGenerator is no longer called by anything in the product (it was only called from the dead code). Decide whether to wire an HTML export to the GUI/CLI.
- CleanCheckGUI (2522 -> 2376 lines): Class Mode moved to ClassModeController. Verified only by comparing javac error sets before/after with JavaFX absent: identical, so no new broken references, but the GUI was NOT compiled or run.
Not done: splitting the rest of CleanCheckGUI (header, sidebar, dashboard, issues, details, fix workflow) - needs JavaFX to verify. Long single methods remain in compareScans and HtmlReportGenerator.generate.
First thing to run: mvn -B verify

## Review fixes (static review + javac/sqlite checks)
- CleanCheck.runAnalysis: removed the second per-file analysis loop. ProjectAnalyzer.analyze already analyzed every file, so every issue was counted, scored and saved to the database twice (and with a bare file name instead of the relative path). Removed the now-unused analyzeFile method and imports.
- CleanCheck --verify-samples now exits with a non-zero code on failure (CI used to pass even when verification failed).
- IssueComparisonService: the previous->current pairing found in step 1 is now stored and reused in step 3. Before, step 3 searched again by fingerprint and could pair several previous issues with the same current issue when identical issues repeat. Removed the unused IssueMatcher.findMatchedCurrentIssue.
- Checked: all 17 SQL statements prepare successfully against the schema in DatabaseInitializer (sqlite3). Everything in src/main compiles except what needs JavaFX/JavaParser, which were not available here. mvn test and the GUI were NOT run.
- Added IssueMatcherTest (5 tests, package database.service). Executed here with minimal JUnit stubs: 5 passed. Not yet run by real JUnit/Maven.
- Renumbered the STEP comments in CleanCheck.runAnalysis.
- IssueComparisonService.compareScans split into: load -> compare (pure, package-private) -> printReport. Public API (compareScans, ComparisonResult) and printed text unchanged. Verified: old and new versions produced identical stdout and counters on 300 random scenarios (152 with moved issues, 90 with changed severity).
- Added IssueComparisonTest (5 tests). Both new test classes (10 tests) were run with minimal JUnit stubs and passed; not yet run by real JUnit/Maven.
- Still open: HtmlReportGenerator is not called by the CLI or GUI (generate() is one long linear HTML builder, left as is); the rest of CleanCheckGUI is not split (needs JavaFX to verify).

## v2 — what changed (not compiled or run by the author of these changes)

Fixed: JavaAst.normalizeCommonTypos used "\bInt\b" (backspace char) so the Int/Char retry never matched, and replaced "string" inside other words. Now uses "\\b" with word boundaries.
Added: JUnit 5 + surefire in pom.xml; RulesTest, QualityTest, JavaFileScannerTest, FixEngineTest (replaced the print-only version), ProjectAnalyzerTest, CiAndConfigTest.
Added: CiRunner (exit codes 0/1/2), JsonReportExporter, RuleConfig + config-aware RuleEngine, CI workflow, Arabic/English rules doc, limitations.
Not done: splitting CleanCheckGUI / HtmlReportGenerator / IssueComparisonService / CleanCheck, jpackage, screenshots. These need a compiler to do safely.
First thing to run: mvn -B verify

## Extended features
- SOLID heuristic rules (SRP/OCP/LSP/ISP/DIP)
- Deeper SARIF 2.1 (artifacts, properties, score)
- Local Web UI (WebServer on port 8787)
- IDE bridge (IdeBridge + ide/README.md)
- Security taint: same-class fields + tainted method returns
