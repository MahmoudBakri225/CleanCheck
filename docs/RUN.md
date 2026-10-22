# Running CleanCheck (demo machine)

## Requirements

- **JDK 26** (project `maven.compiler.release` is 26)
- **Maven 3.9+**
- **Internet only for the first** `mvn` dependency download; after that the demo works offline

## GUI (recommended for committee)

```bash
cd CleanCheck
mvn -q javafx:run
```

Windows (NetBeans JDK path example):

```bat
set JAVA_HOME=C:\Program Files\Apache NetBeans\jdk
mvn javafx:run
```

Main class (GUI): `com.mycompany.cleancheck.ui.CleanCheckGUI`  
CLI analyzer: `com.mycompany.cleancheck.application.CleanCheck`

## Web UI (optional, localhost only)

Listens on **127.0.0.1** only — not reachable from other machines on the LAN.

```bash
mvn -q exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.WebServer -Dexec.args="path/to/project 8787"
```

Open: http://localhost:8787/

## Tests

```bash
mvn -q test
```

The analyzer loads `cleancheck.properties` from the selected project root. Set `-Dcleancheck.config=/absolute/path/to/file`
to use one explicit configuration across projects. A configuration file that exists but cannot be read fails the scan
with its path; missing configuration files use the built-in defaults.

By default, SQLite history is stored in `~/.cleancheck/database/cleancheck.db`. To use another database file for an isolated run, set
the JVM property `cleancheck.database.path` to its absolute path (for example,
`-Dcleancheck.database.path=/tmp/cleancheck-test.db`).

## Offline tip for the venue

1. Run `mvn dependency:go-offline` once while online.
2. Copy the whole `CleanCheck` folder + local Maven repo if needed.
3. Demo with `CleanCheck-TestProject` (all secrets are **demo placeholders**, not real keys).
