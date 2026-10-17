package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Strings compared with == or != (reference comparison) instead of equals(). */
public class StringComparisonRule implements Rule {

    @Override
    public String getName() {
        return "String Comparison";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            // name -> true when every declaration of that name in the file is a String
            Map<String, Boolean> stringNames = new HashMap<>();
            for (VariableDeclarator v : cu.findAll(VariableDeclarator.class)) {
                stringNames.merge(v.getNameAsString(), v.getType().asString().equals("String"), Boolean::logicalAnd);
            }
            for (Parameter p : cu.findAll(Parameter.class)) {
                stringNames.merge(p.getNameAsString(), p.getType().asString().equals("String"), Boolean::logicalAnd);
            }

            for (BinaryExpr expr : cu.findAll(BinaryExpr.class)) {
                if (expr.getOperator() != BinaryExpr.Operator.EQUALS
                        && expr.getOperator() != BinaryExpr.Operator.NOT_EQUALS) {
                    continue;
                }
                boolean leftString = isString(expr.getLeft(), stringNames);
                boolean rightString = isString(expr.getRight(), stringNames);
                boolean leftNull = expr.getLeft().isNullLiteralExpr();
                boolean rightNull = expr.getRight().isNullLiteralExpr();
                if ((leftString || rightString) && !leftNull && !rightNull) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(expr),
                            getName(),
                            Severity.CRITICAL,
                            "Strings are compared with '" + expr.getOperator().asString() + "', which compares references.",
                            "Use equals(), for example \"literal\".equals(value)."
                    ));
                }
            }
        });

        return warnings;
    }

    private static boolean isString(Expression expr, Map<String, Boolean> stringNames) {
        if (expr instanceof StringLiteralExpr) {
            return true;
        }
        return expr instanceof NameExpr name && stringNames.getOrDefault(name.getNameAsString(), false);
    }
}
