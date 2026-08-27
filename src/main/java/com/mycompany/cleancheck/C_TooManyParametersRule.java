package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects methods that have more parameters than the allowed maximum.
 * Analyzes the source code, counts method parameters, and generates
 * a warning when a method contains more than four parameters.
 * Control statements (if, for, while, switch, catch) are ignored.
 */
public class C_TooManyParametersRule implements L_Rule {

    private static final int MAX = 4;

    /**
     * Returns the name of this rule.
     *
     * @return the rule name "Too Many Parameters"
     */
    @Override
    public String getName() {
        return "Too Many Parameters";
    }

    /**
     * Analyzes the given Java source code to find methods with
     * more than four parameters.
     * Ignores control statements such as if, for, while, switch, and catch.
     * When a method exceeds the limit, a warning is added with
     * the file name, line number, severity, and suggestion.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for methods that have too many parameters
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b(\\w+)\\s*\\(([^)]*)\\)");
        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            String methodName = matcher.group(1);
            String params = matcher.group(2).trim();

            // Skip control statements
            if (methodName.equals("if") || methodName.equals("for")
                    || methodName.equals("while") || methodName.equals("switch")
                    || methodName.equals("catch")) {
                continue;
            }

            if (params.isEmpty()) continue;

            int count = params.split(",").length;

            if (count > MAX) {
                int lineNumber = source.substring(0, matcher.start()).split("\\R").length;
                warnings.add(new C_Warning(
                        fileName, lineNumber, getName(),
                        E_Severity.WARNING,
                        "Method '" + methodName + "' has " + count + " parameters.",
                        "Consider grouping related parameters into a class."
                ));
            }
        }

        return warnings;
    }
}