# Score meaning and tool limits (committee)

## Quality score (e.g. 49/100)

- Internal **relative** score for one scan of one project.
- Used to compare **before/after** fixes and regression, not as an industry certification.
- Density-aware: larger codebases are not punished only for size the same way as tiny files.
- **Static analysis can miss issues** and can raise findings that need human judgment.

## Security review

- Preliminary, offline, AST-based leads (CWE / OWASP labels).
- Does **not** replace a full penetration test or commercial SAST with deep taint.
- Cross-file data flow is limited; treat findings as investigation starters.

## Auto-fix

- Only mechanical, low-risk rules (e.g. String Comparison, System Out).
- Always: backup → edit → re-analyze → compile check → keep or rollback.
- Security issues (secrets, injection, XXE, crypto) stay **manual**.
