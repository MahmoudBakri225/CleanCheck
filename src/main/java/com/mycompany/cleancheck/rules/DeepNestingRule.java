package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** Control structures nested deeper than the limit (an else-if chain does not add depth). */
public class DeepNestingRule implements Rule {

    private final int max;

    public DeepNestingRule() {
        this(4);
    }

    public DeepNestingRule(int max) {
        this.max = max;
    }

    @Override
    public String getName() {
        return "Deep Nesting";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            List<CallableDeclaration<?>> callables = new ArrayList<>();
            callables.addAll(cu.findAll(MethodDeclaration.class));
            callables.addAll(cu.findAll(ConstructorDeclaration.class));
            for (CallableDeclaration<?> callable : callables) {
                Node[] deepest = new Node[1];
                int[] depth = new int[1];
                walk(callable, 0, deepest, depth);
                if (depth[0] > max) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(deepest[0]),
                            getName(),
                            Severity.WARNING,
                            "'" + callable.getNameAsString() + "' nests control structures " + depth[0]
                                    + " levels deep (limit " + max + ").",
                            "Use early returns or extract the inner block into its own method."
                    ));
                }
            }
        });

        return warnings;
    }

    private static void walk(Node node, int depth, Node[] deepest, int[] deepestDepth) {
        for (Node child : node.getChildNodes()) {
            if (child instanceof BodyDeclaration<?>) {
                continue; // nested classes and methods are measured on their own
            }
            int childDepth = depth + (nests(child) ? 1 : 0);
            if (childDepth > deepestDepth[0]) {
                deepestDepth[0] = childDepth;
                deepest[0] = child;
            }
            walk(child, childDepth, deepest, deepestDepth);
        }
    }

    private static boolean nests(Node node) {
        if (node instanceof IfStmt ifStmt) {
            Node parent = ifStmt.getParentNode().orElse(null);
            return !(parent instanceof IfStmt outer && outer.getElseStmt().orElse(null) == ifStmt);
        }
        return node instanceof ForStmt || node instanceof ForEachStmt || node instanceof WhileStmt
                || node instanceof DoStmt || node instanceof SwitchStmt || node instanceof TryStmt
                || node instanceof SynchronizedStmt;
    }
}
