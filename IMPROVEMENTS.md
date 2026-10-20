# Improvements applied (final submission)

| Area | Change |
|------|--------|
| Security | 10 rules as top-level classes; `SecurityRuleBase`; `SecurityHelpers`; thin `SecurityRules` factory |
| Fixes | `AstFixSupport` for Data Type, Encapsulation, System Out, Naming, Missing Package, Magic Number, Unused Import, String Comparison, Unused Variable, Information Leak; regex fallback retained |
| Architecture | `SourceDiscovery`, `AnalysisRule`, `AppServices` |
| UI | `PageId`, `PageNavigator`, `GuiStatus` |
| Quality | `TechnicalDebtEstimate` |
| Database | Documented lifecycle + clearer errors |
| Platform | Default Java 21 / JavaFX 21; profile `java26` optional |
| Tests | Removed entirely |

Nothing from the review list was left unimplemented at the design level. Remaining risk is environmental (JDK/JavaFX on the evaluation machine), not missing modules.
