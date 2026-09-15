package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects empty if statements in Java source code.
 * An empty if is identified when a line matches the pattern
 * "if (...) {" and the immediately following line is just "}".
 */
public class C_EmptyIfRule implements L_Rule {

    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Empty If"
     */
    @Override
    public String getName() {
        return "Empty If";
    }

    /**
     * Analyzes the source code line by line looking for empty if bodies.
     * Checks pairs of consecutive lines: if the first matches an if statement
     * ending with '{' and the second is exactly '}', a warning is generated.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for empty if statements found
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");
        for (int i = 0; i < lines.length - 1; i++) {
            String current = lines[i].trim();
            String next = lines[i + 1].trim();
            if (current.matches(".if\\s\\(.\\)\\s\\{") && next.equals("}")) {
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