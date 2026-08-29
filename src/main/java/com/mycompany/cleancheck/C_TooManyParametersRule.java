package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects methods that have more parameters than the allowed maximum.
 * Analyzes the source code, counts method parameters, and generates
 * a warning when a method contains more than four parameters.
 *
 * Only matches actual method DECLARATIONS (return type + modifiers
 * before the name, and a '{' or 'throws' right after the parameter
 * list). This avoids false positives from ordinary method CALLS such
 * as System.out.println(a, b, c, d, e) or constructor calls like
 * new SomeClass(a, b, c, d, e).
 */
public class C_TooManyParametersRule implements L_Rule {

    private static final int MAX = 4;

    /**
     * Matches a method declaration:
     * optional modifiers (public/private/protected/static/final/synchronized),
     * a return type (or constructor with no return type),
     * the method name, the parameter list, and either '{' or 'throws'
     * right after (ignoring whitespace/newlines) to prove it's a
     * declaration and not a call.
     */
    private static final Pattern METHOD_DECLARATION = Pattern.compile(
            "(?m)^[ \\t]*"
            + "(?:(?:public|private|protected|static|final|synchronized|abstract)\\s+)*"
            + "(?:[\\w<>\\[\\],\\s]+\\s+)?"
            + "(\\w+)\\s*\\(([^)]*)\\)\\s*"
            + "(?:throws\\s+[\\w.,\\s]+)?\\s*\\{"
    );

    private static final List<String> CONTROL_KEYWORDS = List.of(
            "if", "for", "while", "switch", "catch", "synchronized"
    );

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
     * Analyzes the given Java source code to find method DECLARATIONS with
     * more than four parameters. Method calls are ignored because the
     * pattern requires a '{' (or 'throws' clause) directly after the
     * parameter list, which only occurs at a declaration site.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a list of warnings for methods that have too many parameters
     */
    @Override
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        Matcher matcher = METHOD_DECLARATION.matcher(source);

        while (matcher.find()) {
            String methodName = matcher.group(1);
            String params = matcher.group(2).trim();

            if (CONTROL_KEYWORDS.contains(methodName)) {
                continue;
            }

            if (params.isEmpty()) {
                continue;
            }

            int count = countParameters(params);

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

    /**
     * Counts parameters while ignoring commas that appear inside generic
     * type arguments, e.g. Map&lt;String, Integer&gt; counts as ONE parameter,
     * not two.
     *
     * @param params the raw parameter list text (without outer parentheses)
     * @return the number of parameters
     */
    private int countParameters(String params) {
        int count = 1;
        int depth = 0;
        for (char c : params.toCharArray()) {
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth = Math.max(0, depth - 1);
            } else if (c == ',' && depth == 0) {
                count++;
            }
        }
        return count;
    }
}
