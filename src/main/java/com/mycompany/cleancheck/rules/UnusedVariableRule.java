package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** Local variables that are declared but never read. */
public class UnusedVariableRule implements Rule {

    @Override
    public String getName() {
        return "Unused Variable";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (VariableDeclarator variable : cu.findAll(VariableDeclarator.class)) {
                if (!(variable.getParentNode().orElse(null) instanceof VariableDeclarationExpr declaration)) {
                    continue; // fields and the like
                }
                Node owner = declaration.getParentNode().orElse(null);
                if (owner instanceof ForEachStmt || owner instanceof TryStmt) {
                    continue; // loop variables and try-with-resources are used by their own syntax
                }
                BlockStmt scope = declaration.findAncestor(BlockStmt.class).orElse(null);
                if (scope == null) {
                    continue;
                }
                String name = variable.getNameAsString();
                if (name.equals("_")) {
                    continue;
                }
                boolean read = scope.findAll(NameExpr.class).stream()
                        .filter(use -> use.getNameAsString().equals(name))
                        .anyMatch(use -> !isPlainAssignmentTarget(use));
                if (!read) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(variable),
                            getName(),
                            Severity.WARNING,
                            "Local variable '" + name + "' is never used.",
                            "Remove it, or use it."
                    ));
                }
            }
        });

        return warnings;
    }

    private static boolean isPlainAssignmentTarget(NameExpr use) {
        return use.getParentNode().orElse(null) instanceof AssignExpr assign
                && assign.getTarget() == use
                && assign.getOperator() == AssignExpr.Operator.ASSIGN;
    }
}
