# CleanCheck Architecture

## Layers

### 1. Scanner
`scanner/` discovers Java source files in the selected project.

### 2. Rules
`rules/` contains independent rule implementations and `RuleEngine` coordinates them.
For each file, the engine parses one JavaParser AST and shares that read-only tree across the enabled rules. The cache is scoped to the current thread and cleared when that file finishes, so concurrent analyses do not share parser state or retain source files between scans.

### 3. Analysis model
`analysis/` contains warnings and severity information.

### 4. Quality and risk
`quality/` calculates the quality score and quality gate. `risk/` identifies file/rule hotspots.

### 5. Fix
`fix/` contains the controlled fix engine and fix result model.

### 6. Persistence
`database/` stores projects, scans, issues and comparison/history information in SQLite.
`ScanPersistenceService` commits a scan and all of its findings in one transaction; if any finding fails to save,
the scan is rolled back too. Set `cleancheck.database.path` to isolate a run or test from the default
`database/cleancheck.db` file. Each SQLite connection enables foreign-key checks and a bounded wait for concurrent writers.

`AnalysisResult` carries the total line count captured from the same decoded source snapshot as its findings. Quality
scores and exports therefore do not reread files after analysis or drift when a file changes during report generation.

### 7. Reporting
`report/` generates HTML, JSON, SARIF and CSV reports. Exporters validate that output cannot replace analyzed source, then write a complete UTF-8 temporary sibling and move it into place so a failed export leaves the previous report intact.

### 8. UI
`ui/` contains the JavaFX desktop interface.

### 9. Application orchestration
`application/` coordinates the end-to-end CLI workflow.
