/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TooManyParametersRule implements Rule {

    private static final int MAX = 4;

    @Override
    public String getName() {
        return "Too Many Parameters";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\b(\\w+)\\s*\\(([^)]*)\\)");
        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            String methodName = matcher.group(1);
            String params = matcher.group(2).trim();

            if (methodName.equals("if") || methodName.equals("for")
                    || methodName.equals("while") || methodName.equals("switch")
                    || methodName.equals("catch")) {
                continue;
            }

            if (params.isEmpty()) continue;

            int count = params.split(",").length;

            if (count > MAX) {
                int lineNumber = source.substring(0, matcher.start()).split("\\R").length;
                warnings.add(new Warning(
                        fileName, lineNumber, getName(),
                        Severity.WARNING,
                        "Method '" + methodName + "' has " + count + " parameters.",
                        "Consider grouping related parameters into a class."
                ));
            }
        }
        return warnings;
    }
}