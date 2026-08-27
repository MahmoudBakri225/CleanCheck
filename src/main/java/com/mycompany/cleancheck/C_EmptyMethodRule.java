package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects empty methods in Java source code.
 * An empty method is identified when a line ends with '{'
 * and the immediately following line is just '}'.
 */
public class C_EmptyMethodRule implements L_Rule {

    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Empty Method"
     */
    @Override
    public String getName() {
        return "Empty Method";
    }

    /**
     * Analyzes the source code line by line looking for empty method bodies.
     * Checks pairs of consecutive lines: if the first ends with '{' and
     * the second is exactly '}', a warning is generated.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for empty methods found
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String current = lines[i].trim();
            String next = lines[i + 1].trim();

            // Detect empty body: line ending with { followed by a line that is only }
            if (current.endsWith("{") && next.equals("}")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.INFO,
                        "Method body appears to be empty.",
                        "Remove the method or implement its functionality."
                ));
            }
        }
        return warnings;
    }
}