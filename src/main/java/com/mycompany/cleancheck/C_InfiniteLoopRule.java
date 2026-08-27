/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cleancheck;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects potential infinite loops in Java source code.
 * Currently checks for:
 * - while(true)
 * - for(;;)
 */
public class C_InfiniteLoopRule implements L_Rule {
    
    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Infinite Loop"
     */
    @Override
    public String getName() {
        return "Infinite Loop";
    }
    
    /**
     * Analyzes the given source code and returns a list of warnings
     * for potential infinite loops.
     *
     * @param fileName the name of the source file
     * @param source   the source code content
     * @return a list of warnings found
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");
        
        // Detect while(true)
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim().toLowerCase();

            if (line.matches(".*while\\s*\\(\\s*true\\s*\\).*")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.CRITICAL,
                        "Possible infinite loop detected (while(true)).",
                        "Make sure there is a break condition inside the loop."
                ));
            }
            
            // Detect for(;;)
            if (line.matches(".*for\\s*\\(\\s*;\\s*;\\s*\\).*")) {
                warnings.add(new C_Warning(
                        fileName, i + 1, getName(),
                        E_Severity.CRITICAL,
                        "Possible infinite loop detected (for(;;)).",
                        "Make sure there is a break condition inside the loop."
                ));
            }
        }
        return warnings;
    }
}
