package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects empty method and constructor bodies in Java source code.
 * A method or constructor is considered empty when its declaration line
 * ends with '{' and the very next non-blank line is just '}'.
 *
 * NOTE: this file was referenced by C_RuleEngine but was not included
 * in the uploaded project, so it has been reconstructed to match the
 * style/behavior of the other line-based rules (e.g. C_EmptyIfRule).
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
     * Analyzes the source code line by line looking for empty method
     * or constructor bodies.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for empty methods/constructors found
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String current = lines[i].trim();
            String next = lines[i + 1].trim();

            if (isMethodOrConstructorSignature(current) && next.equals("}")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.INFO,
                        "Empty method or constructor body detected.",
                        "Implement the method or remove it if it is not needed."
                ));
            }
        }
        return warnings;
    }

    /**
     * Checks whether a line looks like a method or constructor declaration
     * (ends with '{' and contains parentheses), while excluding control
     * statements such as if/for/while/switch/catch, which are handled by
     * other rules.
     *
     * @param line the trimmed line to check
     * @return true if the line looks like a method/constructor signature
     */
    private boolean isMethodOrConstructorSignature(String line) {
        if (!line.endsWith("{")) {
            return false;
        }
        if (!line.contains("(") || !line.contains(")")) {
            return false;
        }
        if (line.matches(".*\\b(if|for|while|switch|catch)\\s*\\(.*")) {
            return false;
        }
        return true;
    }
}
