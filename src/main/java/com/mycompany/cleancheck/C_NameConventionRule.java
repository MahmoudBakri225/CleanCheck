package com.mycompany.cleancheck;

import java.util.*;
import java.util.regex.*;

/**
 * Checks Java source code for naming convention violations.
 * - Class names should use PascalCase (start with uppercase)
 * - Variable names should use camelCase (start with lowercase)
 * - Constants (all uppercase) are allowed
 * - Reserved Java keywords cannot be used as variable names
 * - Names should not start with an underscore
 */
public class C_NameConventionRule implements L_Rule {

    // Set of all reserved Java keywords, used to flag illegal variable names.
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null"
    ));
    // Matches a class declaration and captures the class name.
    private static final Pattern CLASS_PATTERN
            = Pattern.compile("\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    // Matches a simple field/variable declaration line and captures the variable name.
    private static final Pattern VAR_PATTERN
            = Pattern.compile("^(?:public|private|protected|static|final|\\s)*"
                    + "(?:byte|short|int|long|float|double|boolean|char|String)\\s*(?:\\[\\])?\\s+"
                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:=.*)?;");

    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Naming Convention"
     */

    @Override
    public String getName() {
        return "Naming Convention";
    }

    /**
     * Analyzes the given source code and returns a list of naming convention warnings.
     *
     * @param fileName the name of the source file
     * @param source   the source code content
     * @return a list of warnings found
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Skip empty lines and comment lines - nothing to check there.
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")) {
                continue;
            }

            // If the line declares a class, check its name and move on.
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                checkClassName(warnings, fileName, i + 1, classMatcher.group(1));
                continue;
            }

            // Otherwise, check if the line declares a variable.
            Matcher varMatcher = VAR_PATTERN.matcher(line);
            if (varMatcher.find()) {
                checkVariableName(warnings, fileName, i + 1, varMatcher.group(1));
            }
        }
        return warnings;
    }

   /**
     * Checks if the class name starts with an uppercase letter (PascalCase).
     *
     * @param warnings the list to add warnings to
     * @param file     the source file name
     * @param line     the line number
     * @param name     the extracted class name
     */
    private void checkClassName(List<C_Warning> warnings, String file, int line, String name) {
        if (!Character.isUpperCase(name.charAt(0))) {
            warnings.add(new C_Warning(file, line, getName(), E_Severity.WARNING,
                    "Class name '" + name + "' should start with uppercase.",
                    "Use PascalCase for class names."));
        }
    }

    /**
     * Checks the variable name for reserved keywords, leading underscores,
     * and camelCase convention. Constants (all uppercase) are allowed.
     *
     * @param warnings the list to add warnings to
     * @param file     the source file name
     * @param line     the line number
     * @param name     the extracted variable name
     */
    private void checkVariableName(List<C_Warning> warnings, String file, int line, String name) {
        if (KEYWORDS.contains(name)) {
            warnings.add(new C_Warning(file, line, getName(), E_Severity.CRITICAL,
                    "Variable name '" + name + "' is a reserved Java keyword.",
                    "Choose a different variable name."));
        } else if (isConstant(name)) {
            // Constants are exempt from the camelCase rule.
        } else if (name.startsWith("_")) {
            warnings.add(new C_Warning(file, line, getName(), E_Severity.WARNING,
                    "Variable name '" + name + "' should not start with an underscore.",
                    "Start variable names with a lowercase letter instead of '_'."));
        } else if (Character.isUpperCase(name.charAt(0))) {
            warnings.add(new C_Warning(file, line, getName(), E_Severity.WARNING,
                    "Variable name '" + name + "' should use camelCase.",
                    "Start variable names with a lowercase letter."));
        }
    }

    /**
     * Checks whether the given name is a constant (all characters uppercase,
     * e.g. "MAX" or "MAX_VALUE"). An underscore is no longer required —
     * a single-word constant like "MAX" is valid and should not be flagged.
     *
     * @param name the name to check
     * @return true if the name is considered a constant, false otherwise
     */
    private boolean isConstant(String name) {
        return name.equals(name.toUpperCase()) && !name.equals(name.toLowerCase());
    }
}
