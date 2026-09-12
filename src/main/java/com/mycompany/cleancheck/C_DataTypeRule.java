package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects incorrect data type capitalization and potential misuse of numeric types.
 * Checks for:
 * - Primitive types written with wrong case (e.g. Int, BOOLEAN)
 * - "string" written in lowercase instead of "String"
 * - Usage of float (suggests double for better precision)
 * - Usage of double for money-related variables (suggests BigDecimal)
 *
 * Severity notes:
 * - "int" and "char" have no matching wrapper class of that exact
 *   miscapitalized name ("Int" / "Char" do not exist in java.lang —
 *   the real wrappers are "Integer" / "Character"), so miscapitalizing
 *   them is a compile-breaking error -> CRITICAL.
 * - "boolean", "double", "float", "long", "short", "byte" DO have a
 *   real wrapper class with that capitalized name ("Boolean", "Double",
 *   etc.), so writing them capitalized still compiles (just uses
 *   unnecessary autoboxing) -> WARNING (style issue only).
 * - "string" (lowercase) is never a valid type in Java (case-sensitive,
 *   no such class) -> CRITICAL.
 *
 * @author KA
 */
public class C_DataTypeRule implements L_Rule {

    // Primitive names whose miscapitalized form does NOT exist as any
    // real Java type and therefore breaks compilation.
    private static final Set<String> NON_EXISTENT_WHEN_MISCASED = Set.of("int", "char");

    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Data Type"
     */
    @Override
    public String getName() {
        return "Data Type";
    }

    /**
     * Analyzes the source code line by line looking for data type issues.
     * Uses regex to detect variable declarations and applies several checks:
     * wrong capitalization of primitives, incorrect "String", float usage,
     * and double used for financial values.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for data type problems found
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        String[] correctTypes = {
            "byte", "short", "int", "long",
            "float", "double", "char", "boolean"
        };

        Pattern pattern = Pattern.compile("\\b([A-Za-z]+)\\s+\\w+\\s*(=|;)");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                String written = matcher.group(1);

                // Check primitive types capitalization
                for (String correct : correctTypes) {
                    if (written.equalsIgnoreCase(correct) && !written.equals(correct)) {
                        boolean breaksCompilation = NON_EXISTENT_WHEN_MISCASED.contains(correct);

                        warnings.add(new C_Warning(
                                fileName, i + 1, getName(),
                                breaksCompilation ? E_Severity.CRITICAL : E_Severity.WARNING,
                                breaksCompilation
                                        ? "Data type '" + written + "' does not exist in Java and will not compile."
                                        : "Data type '" + written + "' is incorrectly formatted.",
                                "Use '" + correct + "' with the correct capitalization."
                        ));
                    }
                }

                // Check String capitalization ("string" does not exist as a type -> compile error)
                if (written.equalsIgnoreCase("String") && !written.equals("String")) {
                    warnings.add(new C_Warning(
                            fileName, i + 1, getName(),
                            E_Severity.CRITICAL,
                            "Data type '" + written + "' does not exist in Java (Java is case-sensitive).",
                            "Use 'String' with a capital S."
                    ));
                }
            }

            // Suggest double instead of float
            if (line.matches(".*\\bfloat\\s+\\w+.*")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.INFO,
                        "Float data type is being used.",
                        "Consider using double when higher precision is required."
                ));
            }

            // Suggest BigDecimal for money-related doubles
            if (line.matches(".*\\b(double)\\s+.*(price|salary|amount|balance|cost|money).*")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.WARNING,
                        "Double is used for a money-related value.",
                        "Consider using BigDecimal for financial calculations."
                ));
            }
        }

        return warnings;
    }
}
