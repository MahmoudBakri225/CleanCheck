package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flags unexplained numeric literals. Numbers used only as direct method/constructor
 * arguments are skipped to reduce noise (e.g. {@code processUser("test", 25)}).
 */
public class MagicNumberRule implements Rule {

    private static final Set<String> ALLOWED = Set.of(
            "0", "1", "2", "0L", "1L", "2L"
    );

    @Override
    public String getName() {
        return "Magic Number";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        Set<Integer> reported = new HashSet<>();

        JavaAst.parse(source).ifPresent(cu -> {
            Set<Integer> constantLines = new HashSet<>();
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                if (field.isFinal()) {
                    constantLines.add(JavaAst.line(field));
                }
            }

            Set<Integer> argumentLines = new HashSet<>();
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                markArgumentLines(call, argumentLines);
            }
            for (ObjectCreationExpr create : cu.findAll(ObjectCreationExpr.class)) {
                markArgumentLines(create, argumentLines);
            }

            for (IntegerLiteralExpr lit : cu.findAll(IntegerLiteralExpr.class)) {
                String value = lit.getValue();
                if (ALLOWED.contains(value)) {
                    continue;
                }
                try {
                    if (Math.abs(Integer.parseInt(value)) < 3) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                }

                int line = JavaAst.line(lit);
                if (constantLines.contains(line) || argumentLines.contains(line) || !reported.add(line)) {
                    continue;
                }
                // Prefer skipping when this exact node is a call argument (same line can mix cases)
                if (isDirectCallArgument(lit)) {
                    continue;
                }

                warnings.add(new Warning(
                        fileName,
                        line,
                        getName(),
                        Severity.INFO,
                        "Magic number " + value + " found.",
                        "Extract it to a named constant that explains its meaning."
                ));
            }

            for (DoubleLiteralExpr lit : cu.findAll(DoubleLiteralExpr.class)) {
                double number;
                try {
                    number = lit.asDouble();
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (number == 0.0 || number == 0.5 || number == 1.0 || number == 2.0) {
                    continue;
                }
                int line = JavaAst.line(lit);
                if (constantLines.contains(line) || !reported.add(line)) {
                    continue;
                }
                if (isDirectCallArgument(lit)) {
                    continue;
                }
                warnings.add(new Warning(
                        fileName,
                        line,
                        getName(),
                        Severity.INFO,
                        "Magic number " + lit.getValue() + " found.",
                        "Extract it to a named constant that explains its meaning."
                ));
            }
        });

        return warnings;
    }

    private static void markArgumentLines(NodeWithArguments<?> call, Set<Integer> argumentLines) {
        call.getArguments().forEach(arg -> argumentLines.add(JavaAst.line(arg)));
    }

    private static boolean isDirectCallArgument(com.github.javaparser.ast.Node literal) {
        return literal.getParentNode()
                .filter(parent -> parent instanceof MethodCallExpr || parent instanceof ObjectCreationExpr)
                .isPresent()
                || literal.getParentNode()
                .flatMap(com.github.javaparser.ast.Node::getParentNode)
                .filter(grand -> grand instanceof MethodCallExpr || grand instanceof ObjectCreationExpr)
                .isPresent();
    }
}
