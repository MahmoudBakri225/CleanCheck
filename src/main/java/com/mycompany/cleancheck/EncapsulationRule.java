/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class EncapsulationRule implements Rule {
    private static final Pattern NON_PRIVATE_FIELD = Pattern.compile(
            "^(?:(public|protected)\\s+)?"
            + "(?:static\\s+|final\\s+)*"
            + "(int|double|float|String|boolean|char|long|short|byte)\\s+"
            + "[A-Za-z_$][A-Za-z0-9_$]*\\s*(=[^;]*)?;$"
    );

    @Override
    public String getName() {
        return "Encapsulation";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        int depth = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (depth == 1 && NON_PRIVATE_FIELD.matcher(line).matches()) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.WARNING,
                        "Field should be private.",
                        "Use private access modifier and provide getters/setters."
                ));
            }

            depth += countChar(line, '{') - countChar(line, '}');
            if (depth < 0) depth = 0;
        }
        return warnings;
    }

    private int countChar(String text, char target) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == target) count++;
        }
        return count;
    }
}