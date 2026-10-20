# CleanCheck — Submission package

See **PROJECT_TRUTH.md** for the authoritative fact sheet.

## Build

```bash
# Default (JDK 21)
mvn -q package
mvn javafx:run

# If the machine has JDK 26 + matching JavaFX
mvn -Pjava26 -q package
```

## Demo fixtures

Use `CleanCheck-TestProjects` (separate zip) for single-project scans and Class Mode.
