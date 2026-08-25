package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

public class EmptyMethodRule implements Rule {

    @Override
    public String getName() {
        return "Empty Method";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        for (int i = 0; i < lines.length - 1; i++) {
            String current = lines[i].trim();
            String next = lines[i + 1].trim();

            if (current.endsWith("{") && next.equals("}")) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.INFO,
                        "Method body appears to be empty.",
                        "Remove the method or implement its functionality."
                ));
            }
        }
        return warnings;
    }
}
