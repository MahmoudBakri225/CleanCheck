# CleanCheck IDE integration

Lightweight IDE bridge (not a full marketplace plugin).

## External Tool (IntelliJ / NetBeans)

1. `mvn -q -DskipTests package`
2. Program: `java`
3. Arguments: `-cp target/classes:target/dependency/* com.mycompany.cleancheck.application.IdeBridge $FilePath$`
4. Output regex: `^(.*):(\d+): (CRITICAL|WARNING|INFO): (.*): (.*)$`

## Output

```
Student.java:12: WARNING: Encapsulation: Public field ...
```

## Web UI

```bash
mvn -q exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.WebServer -Dexec.args="sample-projects/problematic-project 8787"
```

Open http://localhost:8787/

Full JetBrains Inspections plugin is a future step; this covers demos today.
