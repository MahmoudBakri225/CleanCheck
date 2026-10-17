# Parser + UI structure

## JavaParser
Most structural rules use `JavaAst` + JavaParser AST:
Encapsulation, Empty Method, Empty If, Empty Catch, Too Many Parameters, Long Method.

Heuristic rules still used where AST is less natural:
Naming Convention, Data Type, Magic Number, Infinite Loop (control-flow check).

## UI split
- `GuiTheme` – colors as theme variables (`-cc-bg`, `-cc-card`, ...); `rootStyle(dark)` switches light/dark mode
- `IssueRow` / `HotspotRow` / `HistoryRow` – table models
- `UiKit` – shared factory helpers (cards, buttons, titles, scroll panes)
- `RulesPage` / `AboutPage` / `AnalyzePage` / `RegressionPage` / `QualityHistoryPage` / `FixVerificationPage` / `ScanResultsPage` / `MetricsPage` – standalone screens (the last two refresh automatically after each analysis)
- `cleancheck.css` (resources) – hover, table, scrollbar and input styling
- `CleanCheckGUI` – main window and the remaining screens

SQL for the regression and history screens lives in `database/repository/ScanSummaryRepository`, not in the UI.

## Themes
The header has a Dark/Light button. Every color that depends on the theme is a JavaFX looked-up color defined on the root node, so one `setStyle` call switches the whole window.

## HTML report
`report/ReportStyles` holds the report stylesheet (light, automatic dark mode, print). `HtmlReportGenerator` only builds the markup.

