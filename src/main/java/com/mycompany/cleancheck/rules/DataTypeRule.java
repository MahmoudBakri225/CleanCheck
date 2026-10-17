package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.JavaSourceMask;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTypeRule implements Rule {

    private static final Pattern WRONG_TOKEN = Pattern.compile("\\b(Int|Char|string)\\b");

    @Override
    public String getName() {
        return "Data Type";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        // Comments and string/char literals are blanked first (line numbers are preserved), so
        // text such as "Enter a string" is not mistaken for a mistyped type.
        String[] lines = JavaSourceMask.maskNonCode(source).split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("import ")) {
                continue;
            }
            Matcher matcher = WRONG_TOKEN.matcher(line);
            while (matcher.find()) {
                String token = matcher.group(1);
                String fix = switch (token) {
                    case "string" -> "Use String (capital S).";
                    case "Int" -> "Use int (lowercase).";
                    case "Char" -> "Use char (lowercase).";
                    default -> "Fix the type spelling.";
                };
                warnings.add(new Warning(
                        fileName,
                        i + 1,
                        getName(),
                        Severity.CRITICAL,
                        "'" + token + "' is not a valid Java type as written.",
                        fix
                ));
            }
        }

        JavaAst.parse(source).ifPresent(cu -> {
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                String typeName = field.getElementType().asString();
                for (VariableDeclarator variable : field.getVariables()) {
                    checkMoney(fileName, warnings, typeName, variable.getNameAsString(), JavaAst.line(field));
                }
            }
            for (Parameter parameter : cu.findAll(Parameter.class)) {
                checkMoney(
                        fileName,
                        warnings,
                        parameter.getType().asString(),
                        parameter.getNameAsString(),
                        JavaAst.line(parameter)
                );
            }
        });

        return warnings;
    }

    private void checkMoney(
            String fileName,
            List<Warning> warnings,
            String typeName,
            String varName,
            int line
    ) {
        String type = typeName.toLowerCase(Locale.ROOT);
        String name = varName.toLowerCase(Locale.ROOT);
        boolean floating = type.equals("double") || type.equals("float");
        boolean money = name.contains("price")
                || name.contains("salary")
                || name.contains("money")
                || name.contains("amount")
                || name.contains("cost")
                || name.contains("balance");

        if (floating && money) {
            warnings.add(new Warning(
                    fileName,
                    line,
                    getName(),
                    Severity.WARNING,
                    "Floating type used for money-related name '" + varName + "'.",
                    "Prefer BigDecimal for money values."
            ));
        }
    }
}
