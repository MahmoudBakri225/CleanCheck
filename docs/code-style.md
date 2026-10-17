# CleanCheck Code Style

CleanCheck code should read like a careful review: clear names, visible evidence, and behavior that can be checked.

## Java source

- Use Java 26, UTF-8, four spaces, and one top-level type per file.
- Use PascalCase for types, camelCase for methods and fields, and `UPPER_SNAKE_CASE` for constants.
- Name methods for their action and subject (`analyzeProject`, `restoreBackup`); avoid vague names such as `runTask`.
- Keep methods focused. Extract a helper when it gives a repeated or non-obvious decision a clear name.
- Use records for immutable result data and enums for finite states. Avoid mutable global state.
- Keep control flow explicit: validate inputs at the boundary, return clear outcomes, and preserve exception causes.
- Pass paths, streams, and repositories into operations that need them. Never replace process-wide streams to capture one run.
- Keep comments focused on intent, assumptions, and limitations. Do not narrate obvious statements.

## Analyzer rules and fixes

- Inspect syntax or masked source so comments and literals do not create findings by accident.
- Every rule change needs positive and negative examples, plus a test for an important boundary case.
- A finding should identify its file, line, rule, severity, reason, and actionable next step.
- State the limits of heuristic security checks. A clean scan is not proof that a project is secure.
- A source fix must be narrow and deterministic, create a backup, report its verification, and restore on failure.

## Reliability

- Keep reads and edits inside the selected project; do not follow symbolic links during scan or fix operations.
- Escape data for its output format and protect spreadsheet exports from formula execution.
- Do not turn database or analysis errors into empty history or a successful scan with missing results.
- Run `mvn --batch-mode verify` before merging. CI checks Windows, macOS, and Linux.
- Review behavior, failure paths, diagnostics, accessibility, and documentation as well as compilation.
