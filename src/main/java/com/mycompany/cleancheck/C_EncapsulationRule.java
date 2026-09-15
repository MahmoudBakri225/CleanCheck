package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks for proper encapsulation in Java classes.
 * Detects non-private instance fields declared at class level
 * and suggests making them private with getters/setters.
 */
public class C_EncapsulationRule implements L_Rule {

    /**
     * Pattern that matches common field declarations.
     * Supports public/protected/no modifier, optional static/final,
     * and allows comments after the semicolon.
     */
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^(public|protected)?\\s*"
            + "(static\\s+|final\\s+)*"
            + "(int|double|float|String|boolean|char|long|short|byte)\\s+"
            + "[A-Za-z_$][A-Za-z0-9_$]*\\s*(=.*)?;"
    );

    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Encapsulation"
     */
    @Override
    public String getName() {
        return "Encapsulation";
    }

    /**
     * Analyzes the source code to find fields that violate encapsulation.
     * Uses a simple flag to know when we are inside the class body
     * and reports any non-private field declarations.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for fields that should be private
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        boolean insideClass = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")) {
                continue;
            }

            // Detect class start
            if (line.contains("class ") && line.contains("{")) {
                insideClass = true;
                continue;
            }

            // Detect class end (simple check)
            if (insideClass && line.equals("}")) {
                insideClass = false;
                continue;
            }

            if (!insideClass) {
                continue;
            }

            // Skip methods and constructors
            if (line.contains("(")) {
                continue;
            }

            // Report non-private fields
            if (FIELD_PATTERN.matcher(line).find() && !line.startsWith("private")) {
                warnings.add(new C_Warning(
                        fileName,
                        i + 1,
                        getName(),
                        E_Severity.WARNING,
                        "Field should be private.",
                        "Use private access modifier and provide getters/setters."
                ));
            }
        }

        return warnings;
    }
}