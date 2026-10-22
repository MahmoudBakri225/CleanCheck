package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** Cyclomatic complexity: 1 + the number of decision points in a method or constructor. */
public class CyclomaticComplexityRule implements Rule {

    private final int max;

    public CyclomaticComplexityRule() {
        this(10);
    }

    public CyclomaticComplexityRule(int max) {
        this.max = max;
    }

    @Override
    public String getName() {
        return "Cyclomatic Complexity";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            List<CallableDeclaration<?>> callables = new ArrayList<>();
            callables.addAll(cu.findAll(MethodDeclaration.class));
            callables.addAll(cu.findAll(ConstructorDeclaration.class));
            for (CallableDeclaration<?> callable : callables) {
                int complexity = complexity(callable);
                if (complexity > max) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(callable),
                            getName(),
                            Severity.WARNING,
                            "'" + callable.getNameAsString() + "' has cyclomatic complexity " + complexity
                                    + " (limit " + max + ").",
                            "Extract branches into smaller methods or simplify the conditions."
                    ));
                }
            }
        });

        return warnings;
    }

    static int complexity(CallableDeclaration<?> callable) {
        int count = 1;
        count += owned(callable, IfStmt.class);
        count += owned(callable, ForStmt.class);
        count += owned(callable, ForEachStmt.class);
        count += owned(callable, WhileStmt.class);
        count += owned(callable, DoStmt.class);
        count += owned(callable, CatchClause.class);
        count += owned(callable, ConditionalExpr.class);
        count += (int) callable.findAll(SwitchEntry.class).stream()
                .filter(entry -> entry.findAncestor(CallableDeclaration.class).orElse(null) == callable)
                .filter(entry -> !entry.getLabels().isEmpty())
                .count();
        count += (int) callable.findAll(BinaryExpr.class).stream()
                .filter(binary -> binary.findAncestor(CallableDeclaration.class).orElse(null) == callable)
                .filter(b -> b.getOperator() == BinaryExpr.Operator.AND
                        || b.getOperator() == BinaryExpr.Operator.OR)
                .count();
        return count;
    }

    private static int owned(CallableDeclaration<?> callable, Class<? extends com.github.javaparser.ast.Node> type) {
        return (int) callable.findAll(type).stream()
                .filter(node -> node.findAncestor(CallableDeclaration.class).orElse(null) == callable)
                .count();
    }
}
