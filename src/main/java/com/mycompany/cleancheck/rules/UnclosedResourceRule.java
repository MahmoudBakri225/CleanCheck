package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Streams, readers, sockets... stored in a local variable that is never closed. */
public class UnclosedResourceRule implements Rule {

    private static final Set<String> CLOSEABLE = Set.of(
            "FileInputStream", "FileOutputStream", "FileReader", "FileWriter",
            "BufferedReader", "BufferedWriter", "InputStreamReader", "OutputStreamWriter",
            "PrintWriter", "PrintStream", "ObjectInputStream", "ObjectOutputStream",
            "RandomAccessFile", "Scanner", "Socket", "ServerSocket", "ZipFile", "JarFile");

    @Override
    public String getName() {
        return "Unclosed Resource";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
                if (!CLOSEABLE.contains(creation.getType().getNameAsString())
                        || isSystemStream(creation)
                        || isTryResource(creation)) {
                    continue;
                }
                String variable = assignedLocal(creation);
                BlockStmt scope = creation.findAncestor(BlockStmt.class).orElse(null);
                if (variable == null || scope == null) {
                    continue; // returned, passed on, wrapped or stored in a field: someone else owns it
                }
                boolean closed = scope.findAll(MethodCallExpr.class).stream()
                        .anyMatch(call -> call.getNameAsString().equals("close")
                                && call.getScope().orElse(null) instanceof NameExpr n
                                && n.getNameAsString().equals(variable));
                if (!closed) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(creation),
                            getName(),
                            Severity.WARNING,
                            "'" + variable + "' (" + creation.getType().getNameAsString() + ") is never closed.",
                            "Open it in try-with-resources: try (" + creation.getType().getNameAsString()
                                    + " " + variable + " = ...) { ... }"
                    ));
                }
            }
        });

        return warnings;
    }

    private static boolean isSystemStream(ObjectCreationExpr creation) {
        return creation.getArguments().stream()
                .anyMatch(arg -> arg.toString().equals("System.in")
                        || arg.toString().equals("System.out")
                        || arg.toString().equals("System.err"));
    }

    /** True when the creation sits inside a try-with-resources resource declaration. */
    private static boolean isTryResource(ObjectCreationExpr creation) {
        Node child = creation;
        Node parent = creation.getParentNode().orElse(null);
        while (parent != null) {
            if (parent instanceof com.github.javaparser.ast.stmt.TryStmt tryStmt
                    && tryStmt.getResources().contains(child)) {
                return true;
            }
            child = parent;
            parent = parent.getParentNode().orElse(null);
        }
        return false;
    }

    /** The local variable that directly receives the creation, or null. */
    private static String assignedLocal(ObjectCreationExpr creation) {
        Node parent = creation.getParentNode().orElse(null);
        if (parent instanceof VariableDeclarator declarator
                && declarator.getParentNode().orElse(null) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr) {
            return declarator.getNameAsString();
        }
        if (parent instanceof AssignExpr assign && assign.getTarget() instanceof NameExpr target) {
            return target.getNameAsString();
        }
        return null;
    }
}
