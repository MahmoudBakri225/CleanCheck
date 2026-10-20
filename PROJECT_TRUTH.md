# CleanCheck — Project truth sheet (for submission alignment)

This file describes the **actual** state of this package. Official documents (SRS, presentation, test pack) should match these facts.

## Product capabilities (true)

- Static analysis for Java source (no execution of the analyzed project)
- **38 analysis rules**: 22 quality + 5 educational SOLID + 10 security + Duplicate Code; Parse Error when a file cannot be parsed
- Severities: INFO, WARNING, CRITICAL
- Quality score 0–100 (density-aware) and security grade A–F where implemented
- Security findings mapped to CWE and OWASP Top 10 (2021); review is **preliminary** (per-file / limited flow)
- Verified automatic fixes for supported rules (backup → change → re-analyze → keep or roll back)
- Fix engine prefers **AST-based** edits via `AstFixSupport` where possible, with regex fallback
- Local SQLite history, regression comparison, Class Mode, HTML/JSON/SARIF/CSV exports
- GUI (JavaFX), CLI scan/ci, local Web UI, IDE bridge
- Source stays on the machine (no upload of project source)

## This package facts (true)

| Item | Value |
|------|--------|
| Production `.java` files under `src/main` | **155** |
| Automated unit tests under `src/test` | **0** (removed from this delivery package) |
| Default `maven.compiler.release` | **26** |
| Default JavaFX version | **26** |
| Optional profile | `-Pjava21` (Java 26 + JavaFX 26) if the evaluation machine supports it |
| Security rule layout | Separate classes + `SecurityRuleBase` + thin `SecurityRules` factory |
| Class Mode navigation | `PageId.CLASS_MODE` registered; showPage uses exact page keys |

## What must NOT be claimed for this package

- “26 JUnit test classes” or “81 tests passed” — **not present** in this package
- “Default runtime is Java 26” — **default build is Java 21** unless `-Pjava26` is used and available
- “162 Java files including tests” — use **155 production files** for this archive
- That the Manual Test Pack’s 66 cases were executed — the pack is a **plan**; execution fields are empty until filled after real runs

## Recommended wording for the committee

> CleanCheck analyzes Java projects locally with 38 rules, explanations, optional verified fixes, security review (CWE/OWASP), history, and Class Mode.  
> This delivery targets **JDK 26** by default for wider lab compatibility; a **Java 26** Maven profile remains available.  
> Automated unit tests are not shipped in this archive; verification relies on manual scenarios (see Manual Test Pack) and demonstration on sample student folders.

## How to demo Class Mode

Use folder `CleanCheck-TestProjects` (five sub-projects: StudentA–StudentE). Select the **parent** folder in Class Mode, then analyze.
