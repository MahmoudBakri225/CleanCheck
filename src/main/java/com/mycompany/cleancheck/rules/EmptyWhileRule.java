package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/**
 * Empty {@code while} bodies with a real condition.
 * {@code while (true) { }} is left to {@link InfiniteLoopRule} so the same loop is not
 * reported twice; Infinite Loop is the stronger signal for that case.
 */
public class EmptyWhileRule implements Rule {

    @Override
    public String getName() {
        return "Empty While";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (WhileStmt loop : cu.findAll(WhileStmt.class)) {
                boolean whileTrue = loop.getCondition() instanceof BooleanLiteralExpr literal
                        && literal.getValue();
                // while(true) with empty body → InfiniteLoopRule only (by design)
                if (!whileTrue
                        && loop.getBody() instanceof BlockStmt block
                        && block.getStatements().isEmpty()) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(loop),
                            getName(),
                            Severity.WARNING,
                            "Empty while-loop body detected.",
                            "Add loop logic or remove the loop if it is not needed."
                    ));
                }
            }
        });

        return warnings;
    }
}
