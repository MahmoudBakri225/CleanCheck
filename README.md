# CleanCheck

**CleanCheck** is a local Java static code quality analyzer and explainable improvement coach.

**Analyze → Explain → Fix safely → Re-analyze → Verify → Track quality**

## Run the JavaFX application

```bash
mvn clean javafx:run
```

## Analyze a project from the CLI

```bash
mvn exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.CleanCheck -Dexec.args="path/to/your-project --analyze-only"
```

## Export an HTML report

GUI: click **Report** in the header. CLI (read-only, no database):

```bash
mvn exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.CiRunner -Dexec.args="path/to/your-project --html report.html"
```

## Rules

22 quality rules, 10 security rules (see below) and one project-wide rule (Duplicate Code) are included, and a "Parse Error" finding is reported for any file that cannot be parsed:

Encapsulation, Empty Method, Empty If, Empty While, Infinite Loop,
Too Many Parameters, Naming Convention, Data Type, Empty Catch,
Magic Number, Long Method, System Out, Missing Package, Unused Import, Unused Variable,
Cyclomatic Complexity, Deep Nesting, String Comparison, Generic Catch, Unclosed Resource,
Equals HashCode, Class Length, and (across files) Duplicate Code.

### Security Review (preliminary)

The **Security Review** tab lists findings from 10 security rules, each with its CWE and OWASP Top 10 (2021) category:
SQL Injection (CWE-89), Hardcoded Secret (CWE-798), Weak Cryptography (CWE-327), Insecure Random (CWE-338),
Command Injection (CWE-78), Path Traversal (CWE-22), Unsafe Deserialization (CWE-502), Disabled Certificate Check
(CWE-295), Information Leak (CWE-209) and XML External Entity (CWE-611).

Limits, also shown in the app and the reports: it analyzes one file at a time (no tracking of data between files),
so it can raise false alarms and can miss flaws that cross classes; it does not check dependencies for known
vulnerable versions. It is a first review, not a security guarantee. Automatic fix: only `printStackTrace()`.

Most structural rules use JavaParser/AST analysis.

## Main architecture

```text
                 ProjectAnalyzer
                       ↓
                 AnalysisResult
                       ↓
          ┌────────────┼────────────┐
          ↓            ↓            ↓
      Dashboard    Class Mode    HTML Report
          ↓
      Issue Details
          ↓
     Controlled Fix
          ↓
       Re-analyze
          ↓
      Verification
          ↓
   Resolved / New / Regression
          ↓
     Quality History
```

## Project structure

```text
analysis/      AST/source analysis models
application/   shared project analysis + CLI
rules/         quality rules + RuleEngine
fix/           controlled fixes + rollback verification
ui/            JavaFX dashboard and pages
classmode/     class/project ranking and export
database/      SQLite persistence and comparisons
report/        HTML reporting
docs/          committee/demo documentation
```

JDK 26 + Maven.

Engineering conventions and reliability requirements are documented in [docs/code-style.md](docs/code-style.md).


## CI and configuration (v2)

- CI mode with exit codes and JSON export: see [docs/ci-and-json.md](docs/ci-and-json.md).
- Rule on/off switches: copy `cleancheck.properties.example` to `cleancheck.properties`.
- Rule reference in Arabic and English plus limitations: [docs/rules-ar.md](docs/rules-ar.md).
- History: [CHANGELOG.md](CHANGELOG.md).
- GitHub Actions workflow in `.github/workflows/ci.yml`.

## Configuration, ignoring, exports and packaging (v6)

- `cleancheck.properties` (see `cleancheck.properties.example`): turn rules off, change thresholds (`method.maxLines`, `method.maxParameters`, `method.maxComplexity`, `nesting.maxDepth`, `class.maxLines`, `duplicate.minStatements`), override a rule's severity (`severity.Magic_Number = INFO`; `_` stands for the space in the rule name) and exclude paths (`exclude = **/generated/**`).
- Silence a line in code with `// cleancheck:ignore [Rule Name, ...]` (same line or the line above), or a whole file with `// cleancheck:ignore-file`.
- A file that cannot be parsed is reported as a CRITICAL **Parse Error** instead of silently counting as clean.
- Automatic fixes now also run a javac compile check (when a JDK is present) and are rolled back if they introduce a new compile error. Fixable rules: Data Type, Encapsulation, System Out, Naming Convention, Missing Package, Magic Number, Unused Import, String Comparison, Unused Variable.
- Exports: HTML, JSON, **SARIF** (GitHub Code Scanning) and **CSV**: `CiRunner <project> --sarif r.sarif --csv r.csv`, or the GUI export dialog.
- `CleanCheck.main` now exits with code 2 on failure (the GUI calls `CleanCheck.execute`, which never exits, and reads the analysis object directly).
- Runnable jar: `mvn package`, then `java -jar target/cleancheck.jar` (`gui`, `ci <project> ...`, `scan <project>`). `mvn -Pall-platforms package` builds one jar that runs on Windows, Linux and macOS (Intel + Apple Silicon); without the profile the jar contains the natives for the build OS. CI runs `mvn verify` on Windows, macOS, and Linux. Packaging is available locally through `package-installer.sh` / `.bat` and `jpackage`.

## Running

- `CleanCheck` started with no arguments (for example Run in NetBeans) opens the GUI; with a project path it analyzes it from the command line.
- CleanCheck works on whatever folder you choose (one project or a folder of projects). It does not depend on any bundled example folder.


## Web UI (local browser)

```bash
mvn -q exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.WebServer -Dexec.args="sample-projects/problematic-project 8787"
```

Open http://localhost:8787/

The server binds to loopback, accepts read-only `GET` requests on `/`, `/api/analyze`, and `/report`, and uses a bounded worker pool.

## IDE bridge

```bash
mvn -q exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.IdeBridge -Dexec.args="sample-projects/problematic-project"
```

See `ide/README.md`.

## SOLID rules (educational)

SOLID SRP, OCP, LSP, ISP, DIP — heuristic checks for teaching, not formal verification.
