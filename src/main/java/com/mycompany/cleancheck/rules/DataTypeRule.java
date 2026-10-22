package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

public class DataTypeRule implements Rule {

    @Override
    public String getName() {
        return "Data Type";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        Set<String> reported = new HashSet<>();

        // Inspect parsed type nodes rather than searching raw lines. This avoids reporting
        // identifiers named string/Int/Char (for example: String string or int Int).
        JavaAst.parse(source).ifPresent(cu -> {
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                checkWrongType(fileName, warnings, reported, field.getElementType(), JavaAst.line(field));
                String typeName = field.getElementType().asString();
                for (VariableDeclarator variable : field.getVariables()) {
                    checkMoney(fileName, warnings, typeName, variable.getNameAsString(), JavaAst.line(field));
                }
            }
            for (Parameter parameter : cu.findAll(Parameter.class)) {
                checkWrongType(fileName, warnings, reported, parameter.getType(), JavaAst.line(parameter));
                checkMoney(fileName, warnings, parameter.getType().asString(),
                        parameter.getNameAsString(), JavaAst.line(parameter));
            }
            for (VariableDeclarator variable : cu.findAll(VariableDeclarator.class)) {
                if (variable.getParentNode().orElse(null) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr) {
                    checkWrongType(fileName, warnings, reported, variable.getType(), JavaAst.line(variable));
                }
            }
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                checkWrongType(fileName, warnings, reported, method.getType(), JavaAst.line(method));
            }
        });
        return warnings;
    }

    private void checkWrongType(String fileName, List<Warning> warnings, Set<String> reported,
            Type type, int line) {
        String token = type.asString().replace("[]", "").trim();
        if (!token.equals("Int") && !token.equals("Char") && !token.equals("string")) {
            return;
        }
        String key = line + ":" + token;
        if (!reported.add(key)) {
            return;
        }
        String fix = switch (token) {
            case "string" -> "Use String (capital S).";
            case "Int" -> "Use int (lowercase).";
            case "Char" -> "Use char (lowercase).";
            default -> "Use a valid Java type.";
        };
        warnings.add(new Warning(fileName, line, getName(), Severity.WARNING,
                "'" + token + "' is not a valid Java type as written.", fix));
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
