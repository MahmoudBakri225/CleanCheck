package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects empty if statements in Java source code.
 * An empty if is identified when a line matches the pattern
 * "if (...) {" and the immediately following line is just "}".
 */
public class C_EmptyIfRule implements L_Rule {

    @Override
    public String getName() {
        return "Empty If";
    }

    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String current = lines[i].trim();
            String next = lines[i + 1].trim();

            // إصلاح الـ regex
            if (current.matches(".*if\\s*\\(.*\\)\\s*\\{") && next.equals("}")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.WARNING,
                        "Empty if statement detected.",
                        "Remove the empty if or add the missing logic."
                ));
            }
        }
        return warnings;
    }
}