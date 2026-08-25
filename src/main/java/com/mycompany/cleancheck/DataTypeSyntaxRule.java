package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTypeSyntaxRule implements Rule {

    @Override
    public String getName() {
        return "Data Type Syntax";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        String[] lines = source.split("\\R");

        String[] correctTypes = {"byte", "short", "int", "long", "float", "double", "char", "boolean"};

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
        }
        return warnings;
    }
}
