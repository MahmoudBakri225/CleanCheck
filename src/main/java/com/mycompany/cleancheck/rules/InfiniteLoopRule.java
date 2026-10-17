package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class InfiniteLoopRule implements Rule {

    @Override
    public String getName() {
        return "Infinite Loop";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (WhileStmt loop : cu.findAll(WhileStmt.class)) {
                if (loop.getCondition() instanceof BooleanLiteralExpr lit && lit.getValue()) {
                    addLoopWarning(fileName, hasExit(loop),
                            JavaAst.line(loop), warnings);
                }
            }

            for (ForStmt loop : cu.findAll(ForStmt.class)) {
                boolean noCondition = loop.getCompare().isEmpty();
                if (noCondition) {
                    addLoopWarning(fileName, hasExit(loop), JavaAst.line(loop), warnings);
                }
            }

            for (DoStmt loop : cu.findAll(DoStmt.class)) {
                if (loop.getCondition() instanceof BooleanLiteralExpr lit && lit.getValue()) {
                    addLoopWarning(fileName, hasExit(loop), JavaAst.line(loop), warnings);
                }
            }
        });

        return warnings;
    }

    private boolean hasExit(Node loop) {
        for (BreakStmt statement : loop.findAll(BreakStmt.class)) {
            if (breakExitsLoop(statement, loop)) {
                return true;
            }
        }
        for (ReturnStmt statement : loop.findAll(ReturnStmt.class)) {
            if (returnExitsMethod(statement, loop)) {
                return true;
            }
        }
        return false;
    }

    private boolean breakExitsLoop(BreakStmt statement, Node loop) {
        if (statement.getLabel().isPresent()) {
            String label = statement.getLabel().get().asString();
            return loop.findAncestor(LabeledStmt.class)
                    .filter(ancestor -> ancestor.getLabel().asString().equals(label))
                    .isPresent();
        }

        Node parent = statement.getParentNode().orElse(null);
        while (parent != null && parent != loop) {
            if (parent instanceof WhileStmt || parent instanceof ForStmt || parent instanceof DoStmt
                    || parent instanceof ForEachStmt || parent instanceof SwitchStmt
                    || parent instanceof LambdaExpr || parent instanceof CallableDeclaration<?>) {
                return false;
            }
            parent = parent.getParentNode().orElse(null);
        }
        return parent == loop;
    }

    private boolean returnExitsMethod(ReturnStmt statement, Node loop) {
        Node parent = statement.getParentNode().orElse(null);
        while (parent != null && parent != loop) {
            if (parent instanceof LambdaExpr || parent instanceof CallableDeclaration<?>) {
                return false;
            }
            parent = parent.getParentNode().orElse(null);
        }
        return parent == loop;
    }

    private void addLoopWarning(String fileName, boolean hasExit, int line, List<Warning> warnings) {
        warnings.add(new Warning(
                fileName,
                line,
                getName(),
                hasExit ? Severity.INFO : Severity.CRITICAL,
                hasExit
                        ? "Unbounded loop detected — verify the exit path is reachable."
                        : "Possible infinite loop: no loop condition or exit statement was found.",
                hasExit
                        ? "Keep a clear and reachable break or return."
                        : "Add a clear exit condition or a reachable break/return."
        ));
    }
}
