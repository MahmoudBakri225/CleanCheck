# CleanCheck User Guide

1. Start the JavaFX application.
2. Select the Java project directory.
3. Run **Start Analysis**.
4. Review the quality score and issue counts.
5. Open **Issues** to inspect a warning, its file/line, severity and suggested action.
6. Click an eligible issue to apply its verified automatic fix. CleanCheck re-runs analysis and restores the original file if the finding is not resolved. Findings that need a behavior decision remain manual.
7. Review the refreshed findings and run analysis again whenever you make a manual change.
8. Open **Metrics** to review file/rule hotspots.
9. Review regression and historical quality information.
10. Click **Report** in the header to save a standalone HTML report (also available from the CLI with `CiRunner --html`).

### Safety principle

The normal GUI analysis path is analysis-only. It should not silently rewrite the user's project. Fixes are explicit actions.

CleanCheck excludes `.cleancheck-backups`, `.git`, `target`, `build`, and `out`
directories from project scans. This prevents rollback copies and generated
classes from appearing as duplicate source findings.

Some findings cannot be solved safely without knowing the intended behavior.
For those findings, the fix action is disabled and the suggested change must be
made manually. A TODO comment is not considered a repair.
