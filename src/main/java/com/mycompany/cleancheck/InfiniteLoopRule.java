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
public class InfiniteLoopRule implements Rule {

    @Override
    public String getName() {
        return "Infinite Loop";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toLowerCase();

            if (line.matches(".*while\\s*\\(\\s*true\\s*\\).*")) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.CRITICAL,
                        "Possible infinite loop detected (while(true)).",
                        "Make sure there is a break condition inside the loop."
                ));
            }

            if (line.matches(".*for\\s*\\(\\s*;\\s*;\\s*\\).*")) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.CRITICAL,
                        "Possible infinite loop detected (for(;;)).",
                        "Make sure there is a break condition inside the loop."
                ));
            }
        }
        return warnings;
    }
}
