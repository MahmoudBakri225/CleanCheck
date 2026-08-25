package com.mycompany.cleancheck;

import java.util.*;

public class NameConventionRule implements Rule {


private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false", "null",
            "var", "record", "sealed", "permits", "yield"
    ));

    private static final Set<String> DATA_TYPES = new HashSet<>(Arrays.asList(
            "byte", "short", "int", "long", "float", "double",
            "boolean", "char", "String"
    ));

    @Override
    public String getName() {
        return "Naming Convention";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        boolean insideMethodParameters = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // تجاهل السطور الفاضية والتعليقات
            if (line.isEmpty() || line.startsWith("//")
                    || line.startsWith("/*") || line.startsWith("*") || line.startsWith("*/")) {
                continue;
            }

            // تجاهل parameters الـ method (حتى لو multi-line)
            if (line.contains("(")) {
                int open = countCharacter(line, '(');
                int close = countCharacter(line, ')');
                if (open > close) {
                    insideMethodParameters = true;
                }
                if (insideMethodParameters && close >= open) {
                    insideMethodParameters = false;
                }
                continue;
            }
            if (insideMethodParameters) {
                if (line.contains(")")) {
                    insideMethodParameters = false;
                }
                continue;
            }

            // تجاهل Annotations
            if (line.startsWith("@")) {
                continue;
            }

            // تجاهل if / for / while / switch / catch
            if (isControlStatement(line)) {
                continue;
            }

            // ===== 1. فحص اسم الـ Class =====
            if (line.contains(" class ") || line.startsWith("class ")) {
                String[] parts = line.split("\\s+");
                for (int j = 0; j < parts.length - 1; j++) {
                    if (parts[j].equals("class")) {
                        String className = parts[j + 1].replace("{", "").trim();
                        // لو فيه generic
                        if (className.contains("<")) {
                            className = className.substring(0, className.indexOf('<'));
                        }
                        if (!className.isEmpty() && Character.isLetter(className.charAt(0))
                                && !Character.isUpperCase(className.charAt(0))) {
                            warnings.add(new Warning(
                                    fileName, i + 1, getName(),
                                    Severity.WARNING,
                                    "Class name '" + className + "' should start with uppercase.",
                                    "Use PascalCase for class names."
                            ));
                        }
                        break;
                    }
                }
                continue;
            }

            // تجاهل interface و enum
            if (line.contains("interface ") || line.contains("enum ")) {
                continue;
            }

            // ===== 2. فحص أسماء المتغيرات =====
            String cleanLine = removeModifiers(line);

            String detectedType = null;
            for (String type : DATA_TYPES) {
                if (cleanLine.startsWith(type + " ") || cleanLine.startsWith(type + "[")) {
                    detectedType = type;
                    break;
                }
            }
            if (detectedType == null) {
                continue;
            }

            // شيل نوع البيانات
            String variablesPart = cleanLine.substring(detectedType.length()).trim();

            // شيل [] لو كانت بعد النوع مباشرة (int[] arr)
            if (variablesPart.startsWith("[]")) {
                variablesPart = variablesPart.substring(2).trim();
            }

            variablesPart = variablesPart.replace(";", "");

            // دعم أكثر من متغير في سطر واحد
            String[] variables = variablesPart.split(",");
            for (String variable : variables) {
                String variableName = extractVariableName(variable);
                if (!variableName.isEmpty()) {
                    checkVariableName(warnings, fileName, i + 1, variableName);
                }
            }
        }
        return warnings;
    }

    // ==================== Helper Methods ====================

    private String removeModifiers(String line) {
        String result = line.trim();
        boolean changed = true;
        String[] modifiers = {
            "public", "private", "protected",
            "static", "final", "volatile", "transient"
        };

        while (changed) {
            changed = false;
            for (String mod : modifiers) {
                if (result.startsWith(mod + " ")) {
                    result = result.substring(mod.length()).trim();
                    changed = true;
                }
            }
        }
        return result;
    }

    private String extractVariableName(String variable) {
        String name = variable.split("=")[0].trim();
        name = name.replace("[]", "").trim();
        if (name.contains(" ")) {
            name = name.split("\\s+")[0];
        }
        // تنظيف أي رموز زيادة
        name = name.replaceAll("[^a-zA-Z0-9_$]", "");
        return name;
    }

    private void checkVariableName(List<Warning> warnings, String fileName, int lineNumber, String name) {
        if (name.isEmpty()) return;

        // 1. كلمة محجوزة
        if (KEYWORDS.contains(name)) {
            warnings.add(new Warning(fileName, lineNumber, getName(), Severity.CRITICAL,
                    "Variable name '" + name + "' is a reserved Java keyword.",
                    "Choose a different variable name."));
            return;
        }

        // 2. يبدأ بحرف أو _ أو $ صح؟
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            warnings.add(new Warning(fileName, lineNumber, getName(), Severity.CRITICAL,
                    "Invalid variable name '" + name + "'.",
                    "Variable names must start with a letter, '_' or '$'."));
            return;
        }

        // 3. باقي الحروف مسموحة؟
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                warnings.add(new Warning(fileName, lineNumber, getName(), Severity.CRITICAL,
                        "Invalid character in variable name '" + name + "'.",
                        "Use only letters, digits, '_' or '$'."));
                return;
            }
        }

        // 4. Constants (MAX_SIZE) نسيبها
        if (isConstantName(name)) {
            return;
        }

        // 5. لازم يبدأ بحرف صغير
        if (Character.isUpperCase(name.charAt(0))) {
            warnings.add(new Warning(fileName, lineNumber, getName(), Severity.WARNING,
                    "Variable name '" + name + "' should use camelCase.",
                    "Start variable names with a lowercase letter."));
            return;
        }

        // 6. تحذير لو بدأ بـ _ أو $
        if (name.startsWith("_")) {
            warnings.add(new Warning(fileName, lineNumber, getName(), Severity.WARNING,
                    "Variable name '" + name + "' starts with an underscore.",
                    "Prefer a normal camelCase name."));
            return;
        }

        if (name.startsWith("$")) {
            warnings.add(new Warning(fileName, lineNumber, getName(), Severity.WARNING,
                    "Variable name '" + name + "' starts with '$'.",
                    "Prefer a normal camelCase name."));
        }
    }

    private boolean isConstantName(String name) {
        return name.length() > 1
                && name.equals(name.toUpperCase())
                && name.contains("_");
    }

    private boolean isControlStatement(String line) {
        return line.startsWith("if ") || line.startsWith("if(")
                || line.startsWith("for ") || line.startsWith("for(")
                || line.startsWith("while ") || line.startsWith("while(")
                || line.startsWith("switch ") || line.startsWith("switch(")
                || line.startsWith("catch ") || line.startsWith("catch(");
    }

    private int countCharacter(String text, char character) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == character) count++;
        }
        return count;
    }
}