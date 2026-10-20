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

public final class InformationLeakRule extends SecurityRuleBase {


    private static final Set<String> RESPONSE_CALLS = Set.of("println", "print", "write", "sendError", "append");

    public InformationLeakRule() {
        super(SecurityCatalog.INFORMATION_LEAK);
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (call.getNameAsString().equals("printStackTrace") && call.getArguments().isEmpty()) {
                add(out, fileName, call, Severity.WARNING,
                        "printStackTrace() writes the full stack trace to the console instead of a log.");
                continue;
            }
            String scope = SecurityHelpers.scopeText(call).toLowerCase(Locale.ROOT);
            boolean toUser = RESPONSE_CALLS.contains(call.getNameAsString())
                    && (scope.contains("getwriter") || scope.contains("response")
                            || scope.contains("resp") || scope.contains("getoutputstream"));
            if (!toUser) {
                continue;
            }
            Optional<CatchClause> clause = call.findAncestor(CatchClause.class);
            if (clause.isEmpty()) {
                continue;
            }
            String exception = clause.get().getParameter().getNameAsString();
            boolean leaked = false;
            for (Expression argument : call.getArguments()) {
                for (NameExpr name : argument.findAll(NameExpr.class)) {
                    if (name.getNameAsString().equals(exception)) {
                        leaked = true;
                    }
                }
            }
            if (leaked) {
                add(out, fileName, call, Severity.WARNING,
                        "The exception '" + exception + "' is written to the response the user sees.");
            }
        }
    }

}
