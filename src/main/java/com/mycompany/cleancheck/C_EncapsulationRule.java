package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks encapsulation of instance fields inside classes.
 *
 * Flags non-private instance fields at class body level.
 * Skips interfaces/enums/records, methods, annotations,
 * and static final constants.
 */
public class C_EncapsulationRule implements L_Rule {

    private static final Pattern TYPE_START = Pattern.compile(
            "^(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed)\\s+)*"
                    + "(class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)"
    );

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^(?:(?:public|protected|private|static|final|volatile|transient)\\s+)+"
                    + "(?:[A-Za-z_$][\\w$]*(?:\\s*<[^;{]+>)?(?:\\s*\\[\\s*\\])*)\\s+"
                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*"
                    + "(?:=[^;]*)?;\\s*$"
    );

    // package-private field: Type name;
    private static final Pattern PACKAGE_FIELD = Pattern.compile(
            "^([A-Za-z_$][\\w$]*(?:\\s*<[^;{]+>)?(?:\\s*\\[\\s*\\])*)\\s+"
                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:=[^;]*)?;\\s*$"
    );

    @Override
    public String getName() {
        return "Encapsulation";
    }

    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R", -1);

        int braceDepth = 0;
        boolean inBlockComment = false;
        String enclosingTypeKind = null;
        int enclosingTypeDepth = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = stripLineComment(lines[i]);

            if (inBlockComment) {
                if (line.contains("*/")) {
                    inBlockComment = false;
                    line = line.substring(line.indexOf("*/") + 2);
                } else {
                    continue;
                }
            }
            if (line.contains("/*")) {
                int start = line.indexOf("/*");
                int end = line.indexOf("*/", start + 2);
                if (end >= 0) {
                    line = line.substring(0, start) + " " + line.substring(end + 2);
                } else {
                    inBlockComment = true;
                    line = line.substring(0, start);
                }
            }

            line = line.trim();

            if (line.isEmpty() || line.startsWith("*") || line.startsWith("@")) {
                braceDepth += countBraces(line);
                if (enclosingTypeDepth >= 0 && braceDepth <= enclosingTypeDepth) {
                    enclosingTypeKind = null;
                    enclosingTypeDepth = -1;
                }
                continue;
            }

            Matcher typeMatcher = TYPE_START.matcher(line);
            if (typeMatcher.find()) {
                enclosingTypeKind = typeMatcher.group(1);
                enclosingTypeDepth = braceDepth;
            }

            int depthBefore = braceDepth;
            braceDepth += countBraces(line);

            if (enclosingTypeDepth >= 0 && braceDepth <= enclosingTypeDepth) {
                enclosingTypeKind = null;
                enclosingTypeDepth = -1;
            }

            if (!"class".equals(enclosingTypeKind)) {
                continue;
            }
            if (depthBefore != enclosingTypeDepth + 1) {
                continue;
            }

            // Skip methods / constructors
            if (line.contains("(") && !isFieldWithParensInInitializer(line)) {
                continue;
            }
            if (line.endsWith("{")) {
                continue;
            }

            // static final constant → allowed (e.g. public static final int MAX)
            if (hasWord(line, "static") && hasWord(line, "final")) {
                continue;
            }

            // already private
            if (hasWord(line, "private")) {
                continue;
            }

            String fieldName = extractFieldName(line);
            if (fieldName == null) {
                continue;
            }

            warnings.add(new C_Warning(
                    fileName,
                    i + 1,
                    getName(),
                    E_Severity.WARNING,
                    "Field '" + fieldName + "' should be private.",
                    "Declare the field as private and expose it via getters/setters if needed."
            ));
        }

        return warnings;
    }

    private String extractFieldName(String line) {
        Matcher m = FIELD_PATTERN.matcher(line);
        if (m.matches()) {
            return m.group(1);
        }
        Matcher p = PACKAGE_FIELD.matcher(line);
        if (p.matches()) {
            String type = p.group(1);
            // skip keywords mistaken as types
            if (type.equals("return") || type.equals("throw") || type.equals("new")) {
                return null;
            }
            return p.group(2);
        }
        return null;
    }

    private boolean isFieldWithParensInInitializer(String line) {
        int eq = line.indexOf('=');
        int paren = line.indexOf('(');
        return eq >= 0 && eq < paren;
    }

    private boolean hasWord(String line, String word) {
        return Pattern.compile("\\b" + word + "\\b").matcher(line).find();
    }

    private String stripLineComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString && c == '/' && line.charAt(i + 1) == '/') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private int countBraces(String line) {
        int change = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && !inChar) {
                if (i == 0 || line.charAt(i - 1) != '\\') {
                    inString = !inString;
                }
            } else if (c == '\'' && !inString) {
                if (i == 0 || line.charAt(i - 1) != '\\') {
                    inChar = !inChar;
                }
            } else if (!inString && !inChar) {
                if (c == '{') {
                    change++;
                } else if (c == '}') {
                    change--;
                }
            }
        }
        return change;
    }
}