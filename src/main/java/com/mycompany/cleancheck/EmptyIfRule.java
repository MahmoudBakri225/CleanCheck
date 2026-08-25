/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cleancheck;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author KA
 */
public class EmptyIfRule implements Rule {

    @Override
    public String getName() {
        return "Empty If";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String current = lines[i].trim();
            String next = lines[i + 1].trim();
            if (current.matches(".*if\\s*\\(.*\\)\\s*\\{") && next.equals("}")) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.WARNING,
                        "Empty if statement detected.",
                        "Remove the empty if or add the missing logic."
                ));
            }
        }
        return warnings;
    }
}


