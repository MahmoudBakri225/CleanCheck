# Rule reference

CleanCheck includes 22 per-file quality rules, five educational SOLID heuristics, ten preliminary security rules,
one project-wide duplicate-code rule, and a critical parse-error finding when JavaParser cannot analyze a file.
Thresholds for complexity, method length, class length, nesting, parameter count, and duplicate statements are
configurable in `cleancheck.properties`.

## Quality rules

| Rule | What it checks |
|------|----------------|
| Encapsulation | Fields that are not private |
| Empty Method | Methods with an empty body |
| Empty If | `if` statements with an empty body |
| Empty While | `while` statements with an empty body |
| Infinite Loop | Unbounded loops, distinguishing loops with visible exits |
| Too Many Parameters | Methods over the configured parameter threshold |
| Naming Convention | Java type, method, and field naming heuristics |
| Data Type | Common misspelled built-in types and risky floating-point money types |
| Empty Catch | Catch blocks that discard exceptions |
| Magic Number | Unnamed numeric literals, with common values allowed |
| Long Method | Methods exceeding the configured source-line threshold |
| System Out | Direct `System.out` / `System.err` usage |
| Missing Package | Source files without a package declaration |
| Unused Import | Imports not referenced in the source file |
| Unused Variable | Locals that appear not to be read |
| Cyclomatic Complexity | Methods with high decision complexity |
| Deep Nesting | Excessively nested control flow |
| String Comparison | String values compared with `==` or `!=` |
| Generic Catch | Catch clauses for overly broad exception types |
| Unclosed Resource | Resource handling patterns that may leave resources open |
| Equals HashCode | `equals` implemented without a matching `hashCode` |
| Class Length | Classes exceeding the configured source-line threshold |

## SOLID heuristics

These five rules are educational signals, not formal verification:

| Rule | Heuristic |
|------|-----------|
| SOLID SRP | A class appears to have several unrelated responsibilities |
| SOLID OCP | A design may require edits in existing code for each new case |
| SOLID LSP | An override only throws `UnsupportedOperationException` |
| SOLID ISP | An interface appears to impose unused operations |
| SOLID DIP | High-level code appears coupled to concrete implementations |

## Project rule

| Rule | What it checks |
|------|----------------|
| Duplicate Code | Identical method or constructor bodies across the project, ignoring comments and formatting while preserving literal contents |

## Preliminary security rules

| Rule | CWE | OWASP Top 10 (2021) | What it checks |
|------|-----|---------------------|----------------|
| SQL Injection | CWE-89 | A03 Injection | Dynamically built query strings passed to common SQL/query APIs, including same-method builder appends |
| Hardcoded Secret | CWE-798 | A07 Identification and Authentication Failures | Secret-like variables and recognized key formats assigned literal values |
| Weak Cryptography | CWE-327 | A02 Cryptographic Failures | Weak hashes/ciphers and risky cipher modes |
| Insecure Random | CWE-338 | A02 Cryptographic Failures | Non-cryptographic random values used in security-sensitive contexts |
| Command Injection | CWE-78 | A03 Injection | Dynamic commands passed to process execution APIs |
| Path Traversal | CWE-22 | A01 Broken Access Control | Request-like input reaching file I/O without visible normalization and base-folder containment checks |
| Unsafe Deserialization | CWE-502 | A08 Software and Data Integrity Failures | Deserialization APIs without visible filtering |
| Disabled Certificate Check | CWE-295 | A07 Identification and Authentication Failures | Permissive certificate or hostname verification patterns |
| Information Leak | CWE-209 | A04 Insecure Design | Stack traces or exception details exposed through output/response paths |
| XML External Entity | CWE-611 | A05 Security Misconfiguration | XML parsing without visible external-entity hardening |

Security findings are leads for review, not proof of exploitability. The analyzer uses AST and bounded local
data-flow heuristics. It does not resolve dependencies for known vulnerable versions and cannot reliably follow
data across arbitrary files, libraries, reflection, or framework behavior.

## Parse errors and automatic fixes

A file that cannot be parsed receives a critical **Parse Error** finding instead of appearing clean. Rules that
require an AST cannot analyze that file until its syntax is corrected.

Automatic fixes are available for Data Type, Encapsulation, System Out, Naming Convention, Missing Package,
Magic Number, Unused Import, String Comparison, and Unused Variable. A narrow Information Leak fix converts a lone
`printStackTrace()` call to a logger call. Each attempted fix is analyzed again; new findings or new compile errors
cause the source to be restored. A successful fix keeps a local rollback point.
