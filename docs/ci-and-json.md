# CI mode, JSON and HTML export

Run CleanCheck in a pipeline (read-only, no database, no GUI):

    mvn -q exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.CiRunner \
        -Dexec.args="path/to/project --json report.json --html report.html --min-score 70 --fail-on-critical"

| Exit code | Meaning |
|---|---|
| 0 | Quality gate passed |
| 1 | Quality gate failed (score below `--min-score`, or critical issues with `--fail-on-critical`) |
| 2 | Usage or I/O error |

The gate is the existing `QualityGate` class. `report.json` contains the score, rating, severity counts
and every issue (file, line, rule, severity, message, suggestion).

Disable rules with `cleancheck.properties` (see `cleancheck.properties.example`).

`--html report.html` writes the same standalone HTML report as the GUI **Report** button (no database needed).
