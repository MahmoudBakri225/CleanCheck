package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTypeRule implements Rule {

    @Override
    public String getName() {
        return "Data Type";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        String[] correctTypes = {
            "byte", "short", "int", "long",
            "float", "double", "char", "boolean"
        };

        Pattern pattern = Pattern.compile("\\b([A-Za-z]+)\\s+\\w+\\s*(=|;)");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String written = matcher.group(1);

                for (String correct : correctTypes) {
                    if (written.equalsIgnoreCase(correct) && !written.equals(correct)) {
                        warnings.add(new Warning(
                                fileName, i + 1, getName(),
                                Severity.WARNING,
                                "Data type '" + written + "' is incorrectly formatted.",
                                "Use '" + correct + "' with the correct capitalization."
                        ));
                    }
                }

                if (written.equalsIgnoreCase("String") && !written.equals("String")) {
                    warnings.add(new Warning(
                            fileName, i + 1, getName(),
                            Severity.WARNING,
                            "Data type '" + written + "' is incorrectly formatted.",
                            "Use 'String' with a capital S."
                    ));
                }
            }

            if (line.matches(".*\\bfloat\\s+\\w+.*")) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.INFO,
                        "Float data type is being used.",
                        "Consider using double when higher precision is required."
                ));
            }

            if (line.matches(".*\\b(double)\\s+.*(price|salary|amount|balance|cost|money).*")) {
                warnings.add(new Warning(
                        fileName, i + 1, getName(),
                        Severity.WARNING,
                        "Double is used for a money-related value.",
                        "Consider using BigDecimal for financial calculations."
                ));
            }
        }
        return warnings;
    }
}
