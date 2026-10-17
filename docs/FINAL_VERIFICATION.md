# Release Verification

This document records the checks performed for the reviewable CleanCheck package.

## Verified

- Sources and tests compiled with JDK 26.0.2 and JavaParser 3.28.2 using `javac --release 26`.
- 82 JUnit tests were discovered by the project test runner: 81 passed, 1 was aborted because the Windows environment did not permit creating a symbolic link; there were no failed tests.
- The CI analyzer completed a self-scan over 162 Java files. It reported 394 findings, including the intentional vulnerable security demonstration, and passed with score 97.
- A CLI smoke test analyzed a fixture and wrote HTML, JSON, SARIF 2.1.0, and CSV in one run. The machine-readable reports parsed successfully and no temporary files remained.
- Export tests cover UTF-8 output, replacing an existing report, preserving an existing destination when replacement fails, and rejecting report paths that alias analyzed source files.
- The final ZIP was inspected to confirm it contains the project and does not contain `target/`, `work/`, or `.git/` build and scratch files.

## Environment limits

- Maven `test` was attempted with JDK 26.0.1 and 26.0.2. In this managed Windows environment, the compiler failed while closing JARs from the local Maven repository with `java.nio.file.AccessDeniedException` in `jdk.zipfs`; the subsequent Maven test compilation consequently could not resolve project classes. The Maven lifecycle is therefore not claimed as passing here.
- The JavaFX desktop window could not be launched for an interactive smoke test because the environment rejected background process creation. The source and tests compile manually, but this package does not claim a visual desktop QA pass.

Run the normal release checks on a standard developer machine with JDK 26 and Maven 3.9+: `mvn clean verify` and `mvn javafx:run`.
