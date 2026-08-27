/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

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
     * Pattern that matches field declarations which are not private.
     * Matches fields with public or protected modifiers (or no modifier),
     * optional static/final, common types, and an optional initializer.
     */
    private static final Pattern NON_PRIVATE_FIELD = Pattern.compile(
            "^(?:(public|protected)\\s+)?"
            + "(?:static\\s+|final\\s+)*"
            + "(int|double|float|String|boolean|char|long|short|byte)\\s+"
            + "[A-Za-z_$][A-Za-z0-9_$]*\\s*(=[^;]*)?;$"
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
     * Tracks brace depth to determine when we are inside the class body
     * (depth == 1) and checks whether any field declared there is not private.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for fields that should be private
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        int depth = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // Only check fields when we are directly inside the class body
            if (depth == 1 && NON_PRIVATE_FIELD.matcher(line).matches()) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.WARNING,
                        "Field should be private.",
                        "Use private access modifier and provide getters/setters."
                ));
            }
            
            // Update brace depth to track nesting level
            depth += countChar(line, '{') - countChar(line, '}');
            if (depth < 0) depth = 0;
        }
        return warnings;
    }
    
    /**
     * Counts how many times a specific character appears in the given text.
     * Used to track the number of opening and closing braces so we can
     * determine the current nesting depth of the code.
     *
     * @param text   the line of code to examine
     * @param target the character to count (usually '{' or '}')
     * @return the number of occurrences of the target character
     */
    private int countChar(String text, char target) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == target) count++;
        }
        return count;
    }
}