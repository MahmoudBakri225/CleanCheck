/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VariableNameRule implements Rule {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long",
            "native", "new", "package", "private", "protected",
            "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null",
            "var", "record", "sealed", "permits", "yield"
    ));

    private static final Set<String> DATA_TYPES = new HashSet<>(Arrays.asList(
            "byte",
            "short",
            "int",
            "long",
            "float",
            "double",
            "boolean",
            "char",
            "String"
    ));

    @Override
    public String getName() {
        return "Variable Name";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {

        List<Warning> warnings = new ArrayList<>();

        String[] lines = source.split("\\R");

        boolean insideMethodParameters = false;
        int braceDepth = 0;

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            // Ignore empty lines
            if (line.isEmpty()) {
                continue;
            }

            // Ignore single-line comments
            if (line.startsWith("//")) {
                continue;
            }

            // Ignore block comments
            if (line.startsWith("/*")
                    || line.startsWith("*")
                    || line.startsWith("*/")) {
                continue;
            }

            /*
             * Detect method parameter section.
             *
             * Example:
             *
             * public void setData(
             *      String name,
             *      int age,
             *      String email) {
             */
            if (line.contains("(")) {

                int open = countCharacter(line, '(');
                int close = countCharacter(line, ')');

                if (open > close) {
                    insideMethodParameters = true;
                }

                continue;
            }

            if (insideMethodParameters) {

                if (line.contains(")")) {
                    insideMethodParameters = false;
                }

                continue;
            }

            // Ignore annotations
            if (line.startsWith("@")) {
                continue;
            }

            // Ignore control statements
            if (isControlStatement(line)) {
                continue;
            }

            /*
             * Ignore lines that obviously contain
             * method/class declarations.
             */
            if (line.startsWith("public class")
                    || line.startsWith("private class")
                    || line.startsWith("protected class")
                    || line.startsWith("class ")
                    || line.startsWith("interface ")
                    || line.startsWith("enum ")) {

                continue;
            }

            /*
             * Remove common modifiers.
             */
            String cleanLine = removeModifiers(line);

            /*
             * Detect variable data type.
             */
            String detectedType = null;

            for (String type : DATA_TYPES) {

                if (cleanLine.startsWith(type + " ")) {
                    detectedType = type;
                    break;
                }
            }

            if (detectedType == null) {
                continue;
            }

            /*
             * Remove data type.
             *
             * Example:
             *
             * int age = 20;
             *
             * becomes:
             *
             * age = 20;
             */
            String variablesPart = cleanLine
                    .substring(detectedType.length())
                    .trim();

            // Remove semicolon
            variablesPart = variablesPart.replace(";", "");

            /*
             * Support multiple variables.
             *
             * Example:
             *
             * int age, salary;
             */
            String[] variables = variablesPart.split(",");

            for (String variable : variables) {

                String variableName = extractVariableName(variable);

                if (variableName.isEmpty()) {
                    continue;
                }

                checkVariableName(
                        warnings,
                        fileName,
                        i + 1,
                        variableName
                );
            }
        }

        return warnings;
    }

    /**
     * Remove Java modifiers from the beginning of a line.
     */
    private String removeModifiers(String line) {

        String result = line.trim();

        boolean changed = true;

        while (changed) {

            changed = false;

            String[] modifiers = {
                "public",
                "private",
                "protected",
                "static",
                "final",
                "volatile",
                "transient"
            };

            for (String modifier : modifiers) {

                if (result.startsWith(modifier + " ")) {

                    result = result
                            .substring(modifier.length())
                            .trim();

                    changed = true;
                }
            }
        }

        return result;
    }

    /**
     * Extract variable name from declaration.
     *
     * Example:
     *
     * age = 20
     *
     * returns:
     *
     * age
     */
    private String extractVariableName(String variable) {

        String name = variable
                .split("=")[0]
                .trim();

        /*
         * Remove array brackets.
         *
         * Example:
         *
         * names[]
         *
         * becomes:
         *
         * names
         */
        name = name.replace("[]", "").trim();

        /*
         * If something unexpected remains,
         * take only the first token.
         */
        if (name.contains(" ")) {
            name = name.split("\\s+")[0];
        }

        return name;
    }

    /**
     * Check variable naming rules.
     */
    private void checkVariableName(
            List<Warning> warnings,
            String fileName,
            int lineNumber,
            String name) {

        /*
         * Check empty name.
         */
        if (name.isEmpty()) {
            return;
        }

        /*
         * Check Java keyword.
         */
        if (KEYWORDS.contains(name)) {

            warnings.add(new Warning(
                    fileName,
                    lineNumber,
                    getName(),
                    Severity.CRITICAL,
                    "Variable name '" + name
                    + "' is a reserved Java keyword.",
                    "Choose a different variable name."
            ));

            return;
        }

        /*
         * Check first character.
         */
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {

            warnings.add(new Warning(
                    fileName,
                    lineNumber,
                    getName(),
                    Severity.CRITICAL,
                    "Invalid variable name '" + name + "'.",
                    "Variable names must start with a valid Java identifier character."
            ));

            return;
        }

        /*
         * Check remaining characters.
         */
        for (int i = 1; i < name.length(); i++) {

            if (!Character.isJavaIdentifierPart(name.charAt(i))) {

                warnings.add(new Warning(
                        fileName,
                        lineNumber,
                        getName(),
                        Severity.CRITICAL,
                        "Invalid variable name '" + name + "'.",
                        "Use only letters, digits, '_' or '$'."
                ));

                return;
            }
        }

        /*
         * Constants are normally written in UPPER_CASE.
         *
         * Example:
         *
         * static final int MAX_SIZE = 100;
         *
         * So we don't report them as camelCase violations.
         */
        if (isConstantName(name)) {
            return;
        }

        /*
         * Variable names should normally start
         * with a lowercase letter.
         *
         * Example:
         *
         * StudentName  ❌
         * studentName  ✅
         */
        if (Character.isUpperCase(name.charAt(0))) {

            warnings.add(new Warning(
                    fileName,
                    lineNumber,
                    getName(),
                    Severity.WARNING,
                    "Variable name '" + name
                    + "' should use camelCase.",
                    "Start variable names with a lowercase letter."
            ));

            return;
        }

        /*
         * Avoid unnecessary leading underscore.
         */
        if (name.startsWith("_")) {

            warnings.add(new Warning(
                    fileName,
                    lineNumber,
                    getName(),
                    Severity.WARNING,
                    "Variable name '" + name
                    + "' starts with an underscore.",
                    "Use a descriptive camelCase name instead."
            ));

            return;
        }

        /*
         * Avoid unnecessary '$'.
         */
        if (name.startsWith("$")) {

            warnings.add(new Warning(
                    fileName,
                    lineNumber,
                    getName(),
                    Severity.WARNING,
                    "Variable name '" + name
                    + "' starts with '$'.",
                    "Use a descriptive camelCase name instead."
            ));
        }
    }

    /**
     * Detect constants.
     *
     * Example:
     *
     * MAX_SIZE
     * MAX_VALUE
     */
    private boolean isConstantName(String name) {

        if (name.length() < 2) {
            return false;
        }

        return name.equals(name.toUpperCase())
                && name.contains("_");
    }

    /**
     * Detect if line is a control statement.
     */
    private boolean isControlStatement(String line) {

        return line.startsWith("if ")
                || line.startsWith("if(")
                || line.startsWith("for ")
                || line.startsWith("for(")
                || line.startsWith("while ")
                || line.startsWith("while(")
                || line.startsWith("switch ")
                || line.startsWith("switch(")
                || line.startsWith("catch ")
                || line.startsWith("catch(");
    }

    /**
     * Count a specific character inside a string.
     */
    private int countCharacter(String text, char character) {

        int count = 0;

        for (int i = 0; i < text.length(); i++) {

            if (text.charAt(i) == character) {
                count++;
            }
        }

        return count;
    }
}