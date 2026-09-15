# CleanCheck — Java Code Quality Analyzer

> A lightweight static code analyzer for Java that catches quality issues your compiler doesn't.

Your code can compile, run, and produce zero errors — and still be poorly written. Empty methods, broken encapsulation, unclear naming, methods with too many parameters, potential infinite loops... none of that stops the compiler, but all of it hurts readability and maintainability.

**CleanCheck** scans your Java source files, applies a set of quality rules, and produces a detailed report: what's wrong, where exactly it is, how severe it is, and how to fix it.

---

## ✨ Features

- **Static analysis** — inspects source code without executing it
- **7 built-in quality rules** covering encapsulation, naming, method design, and more
- **Severity levels** (`INFO` / `WARNING` / `CRITICAL`) for prioritizing fixes
- **Actionable suggestions** for every detected issue, not just a warning
- **Quality Score** (0–100) summarizing the overall health of a project
- **HTML report generation** for a shareable, readable output
- **Easily extensible** — add a new rule by implementing one interface

---

## 🚦 Quick Example

**Input** (`Student.java`):

```java
public class Student {
    String name;          // not private
    public int age;       // public field
    int _code;             // invalid naming

    public void emptyMethod() {
    }                       // empty method body

    public void configure(String a, int b, String c,
                           double d, String e, String f) {
    }                       // too many parameters
}
```

**CleanCheck output:**

| Rule | Line | Severity | Message |
|---|---|---|---|
| Encapsulation | 3 | WARNING | Field should be private |
| Naming Convention | 5 | WARNING | Variable name should not start with underscore |
| Empty Method | 8 | INFO | Method body appears to be empty |
| Too Many Parameters | 11 | WARNING | Method has 6 parameters |

**Quality Score:** `15 / 100` → Rating: **POOR**

---

## 🏗️ Architecture

CleanCheck is organized into four layers, each with a single responsibility:

```
Java Source Files
        │
        ▼
 JavaFileScanner   ──►  finds and reads all .java files
        │
        ▼
   RuleEngine       ──►  runs every rule against every file
        │
        ▼
     Rules (×7)      ──►  each rule detects one specific issue
        │
        ▼
     Warning          ──►  structured result: file, line, severity, message, suggestion
        │
        ▼
QualityScore + HtmlReportGenerator ──► final score + report
```

### Package structure

```
com.mycompany.cleancheck
├── C_CleanCheck.java              # entry point — orchestrates the pipeline
├── C_RuleEngine.java              # runs all rules against scanned files
├── L_Rule.java                    # interface every rule implements
├── C_EncapsulationRule.java       # detects non-private fields
├── C_EmptyMethodRule.java         # detects empty method bodies
├── C_EmptyIfRule.java             # detects empty if-statements
├── C_NameConventionRule.java      # detects naming convention violations
├── C_DataTypeRule.java            # detects data type issues
├── C_InfiniteLoopRule.java        # detects potential infinite loops
├── C_TooManyParametersRule.java   # detects methods with excess parameters
├── C_HtmlReportGenerator.java     # renders the HTML report
├── scanner/
│   └── C_JavaFileScanner.java     # discovers and reads .java files
└── quality/
    ├── E_Severity.java            # enum: INFO / WARNING / CRITICAL
    └── C_QualityScore.java        # computes the final quality score
```

### The `L_Rule` interface

Every rule implements the same contract, which keeps the engine decoupled from rule-specific logic (Open for Extension, Closed for Modification):

```java
public interface L_Rule {
    String getName();
    List<C_Warning> analyze(String fileName, String source);
}
```

Adding a new rule means creating one class that implements `L_Rule` — no changes needed anywhere else in the system.

---

## 🔍 Rules Reference

| # | Rule | What it detects |
|---|---|---|
| 1 | **Encapsulation** | Fields that should be `private` but aren't |
| 2 | **Empty Method** | Methods with an empty body |
| 3 | **Empty If** | `if` statements with an empty body |
| 4 | **Naming Convention** | Names that break `camelCase` / `PascalCase`, or use invalid characters |
| 5 | **Data Type** | Issues or mismatches in declared types |
| 6 | **Infinite Loop** | `while(true)` / `for(;;)` without a clear `break` |
| 7 | **Too Many Parameters** | Methods with more than 4 parameters |

Each detected issue is returned as a `C_Warning` object containing:

```java
fileName     // path of the affected file
lineNumber   // exact line of the issue
ruleName     // which rule flagged it
severity     // INFO | WARNING | CRITICAL
message      // human-readable description
suggestion   // practical recommendation
```

---

## 🆚 Why CleanCheck?

| Tool | Focus | How CleanCheck differs |
|---|---|---|
| Checkstyle | Style & formatting | Simpler explanations, beginner-friendly suggestions |
| PMD | Broad code smell detection | Smaller, easier learning curve |
| SpotBugs | Runtime bug detection | Clear, actionable recommendations you can act on immediately |
| SonarQube | Enterprise dashboards / CI-CD | Lightweight, standalone, zero server setup |

CleanCheck isn't a replacement for these tools — it's a simple, fast starting point best suited for learning and small-to-medium projects.

### CleanCheck + AI

AI assistants (ChatGPT, Claude, etc.) are great for deep explanations, but on their own they have no fixed baseline: results can vary between models and between runs, and code often has to leave your machine to be reviewed. CleanCheck runs 100% locally with deterministic rules — same input, same output, every time — giving you a stable baseline you can layer AI review on top of.

---

## 🛠️ Tech Stack

- **Java 26** — core language
- **Maven** — build and dependency management
- **NetBeans** — development IDE
- **OOP principles** — encapsulation, interfaces, polymorphism
- **Collections & Streams** — `List`, `Map`, Java Streams
- **NIO** — `Path` / `Files` API for file handling
- **Regex** — pattern matching for rule detection
- **Enums** — type-safe severity levels

---

## 🚀 Getting Started

```bash
# clone the repository
git clone https://github.com/<your-username>/CleanCheck.git
cd CleanCheck

# build with Maven
mvn clean install

# run against a target project
mvn exec:java -Dexec.args="path/to/your/java/project"
```

The tool will scan the target path for `.java` files, run all rules, print a summary to the console, and (optionally) generate an HTML report.

---

## 📈 Roadmap

- [ ] More quality rules
- [ ] SOLID principle violation detection
- [ ] Improved, more visual HTML report
- [ ] Full JUnit test coverage
- [ ] Persist analysis results to a SQL database
- [ ] Analysis history to track code quality over time
- [ ] Quality dashboard with visual insights
- [ ] CI/CD pipeline integration

---

## 🧩 Object-Oriented Design

CleanCheck is also a practical showcase of core OOP concepts:

- **Classes & Objects** — `Warning`, `RuleEngine`, `QualityScore`
- **Encapsulation** — private fields in the `Warning` model
- **Abstraction** — `L_Rule` hides each rule's internal logic
- **Interface** — a fixed contract shared by every rule
- **Polymorphism** — `analyze()` behaves differently per rule
- **Method Overriding** — each rule provides its own `analyze()` implementation
- **Enums** — type-safe severity levels
- **Single Responsibility Principle** — every class has one clear job

---

## 👥 Team — Code Titans

| Name | Role |
|---|---|
| Mahmoud Bakri | Team Leader |
| Nashwa Nassar | Developer |
| Ahmed Ramzy | Developer |
| Manar Galal | Developer |

Built as part of the **Java OOP Course**, under the supervision of **Dr. Mina Younan**, within **Digital Egypt Pioneers Initiative (DEPI) — Round 5**.

---

## 📄 License

This project was developed for educational purposes as part of the DEPI program. Add a license of your choice (e.g. MIT) before making the repository public, if needed.

---

> Don't just make your code work. Make your code clean.
