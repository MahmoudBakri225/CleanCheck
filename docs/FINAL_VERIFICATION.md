# Release Verification Status

**Current package includes delivery hardening applied 2026-10-22** (safe web errors, Host loopback forms, no GUI stack dumps, CASCADE FKs, scan limits, unified discovery).

Build and tests were **not** executed in the packaging environment. On the developer machine (JDK 21+ / IDE with Java 26 supported for development):

```bash
mvn -q test
mvn -q -Pall-platforms package
```

Then smoke-test GUI, CI (`java -jar target/cleancheck.jar ci ...`), and optional WebServer against a sample project.

If an older SQLite file exists without CASCADE constraints, delete or relocate it once:

```bash
# optional reset of local history DB
rm -f ~/.cleancheck/database/cleancheck.db
```

See [`STATIC_REVIEW_REPORT_AR.md`](../STATIC_REVIEW_REPORT_AR.md) for the Arabic summary of delivery changes.
