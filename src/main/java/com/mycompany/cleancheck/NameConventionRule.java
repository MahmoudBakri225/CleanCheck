package com.mycompany.cleancheck;

import java.util.*;
import java.util.regex.*;

public class NameConventionRule implements Rule {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null"
    ));

    // كلاس: class Name
    private static final Pattern CLASS_PATTERN
            = Pattern.compile("\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    // متغير: Type name = ... ; أو Type name;
    private static final Pattern VAR_PATTERN
            = Pattern.compile("^(?:public|private|protected|static|final|\\s)*"
                    + "(?:byte|short|int|long|float|double|boolean|char|String)\\s*(?:\\[\\])?\\s+"
                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:=.*)?;");

    @Override
    public String getName() {
        return "Naming Convention";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("*")) {
                continue;
            }

            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            if (classMatcher.find()) {
                checkClassName(warnings, fileName, i + 1, classMatcher.group(1));
                continue;
            }

            Matcher varMatcher = VAR_PATTERN.matcher(line);
            if (varMatcher.find()) {
                checkVariableName(warnings, fileName, i + 1, varMatcher.group(1));
            }
        }
        return warnings;
    }

    private void checkClassName(List<Warning> warnings, String file, int line, String name) {
        if (!Character.isUpperCase(name.charAt(0))) {
            warnings.add(new Warning(file, line, getName(), Severity.WARNING,
                    "Class name '" + name + "' should start with uppercase.",
                    "Use PascalCase for class names."));
        }
    }

    private void checkVariableName(List<Warning> warnings, String file, int line, String name) {
        if (KEYWORDS.contains(name)) {
            warnings.add(new Warning(file, line, getName(), Severity.CRITICAL,
                    "Variable name '" + name + "' is a reserved Java keyword.",
                    "Choose a different variable name."));
        } else if (isConstant(name)) {
            // ثوابت زي MAX_VALUE مقبولة
        } else if (Character.isUpperCase(name.charAt(0))) {
            warnings.add(new Warning(file, line, getName(), Severity.WARNING,
                    "Variable name '" + name + "' should use camelCase.",
                    "Start variable names with a lowercase letter."));
        }
    }

    private boolean isConstant(String name) {
        return name.equals(name.toUpperCase()) && name.contains("_");
    }
}
