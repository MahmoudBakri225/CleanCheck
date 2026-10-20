package com.mycompany.cleancheck.security;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.*;
import java.util.regex.Pattern;

public final class InsecureRandomRule extends SecurityRuleBase {


    private static final Pattern SECURITY_CONTEXT = Pattern.compile(
            "token|password|passwd|secret|session|nonce|salt|csrf|apikey|api_key|privatekey|verification",
            Pattern.CASE_INSENSITIVE);

    public InsecureRandomRule() {
        super(SecurityCatalog.INSECURE_RANDOM);
    }

    private static String contextOf(Node node) {
        StringBuilder context = new StringBuilder();
        Node current = node;
        while (current != null) {
            if (current instanceof VariableDeclarator declarator) {
                context.append(' ').append(declarator.getNameAsString());
            } else if (current instanceof MethodDeclaration method) {
                context.append(' ').append(method.getNameAsString());
            } else if (current instanceof com.github.javaparser.ast.body.TypeDeclaration<?> type) {
                context.append(' ').append(type.getNameAsString());
            } else if (current instanceof AssignExpr assign && assign.getTarget().isNameExpr()) {
                context.append(' ').append(assign.getTarget().asNameExpr().getNameAsString());
            }
            current = current.getParentNode().orElse(null);
        }
        return context.toString();
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
            if (creation.getType().getNameAsString().equals("Random")
                    && SECURITY_CONTEXT.matcher(contextOf(creation)).find()) {
                add(out, fileName, creation, Severity.WARNING,
                        "java.util.Random is predictable, but this looks like a security value (token, password, salt...).");
            }
        }
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            boolean math = call.getNameAsString().equals("random") && SecurityHelpers.scopeIs(call, "Math");
            boolean threadLocal = call.getNameAsString().equals("current") && SecurityHelpers.scopeIs(call, "ThreadLocalRandom");
            if ((math || threadLocal) && SECURITY_CONTEXT.matcher(contextOf(call)).find()) {
                add(out, fileName, call, Severity.WARNING,
                        (math ? "Math.random()" : "ThreadLocalRandom")
                                + " is predictable, but this looks like a security value (token, password, salt...).");
            }
        }
    }

}
