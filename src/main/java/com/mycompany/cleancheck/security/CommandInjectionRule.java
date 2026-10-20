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

public final class CommandInjectionRule extends SecurityRuleBase {


    public CommandInjectionRule() {
        super(SecurityCatalog.COMMAND_INJECTION);
    }

    private void check(List<Warning> out, String fileName, Node at, List<Expression> arguments, String what) {
        SecurityAst.Flow flow = new SecurityAst.Flow(SecurityAst.callableOf(at));
        boolean built = false;
        boolean dynamic = false;
        Optional<String> source = Optional.empty();
        for (Expression argument : arguments) {
            if (SecurityAst.isDynamic(argument)) {
                dynamic = true;
            }
            if (SecurityAst.isBuiltDynamically(argument, at)) {
                built = true;
            }
            if (source.isEmpty()) {
                source = flow.sourceIn(argument);
            }
        }
        if (source.isPresent() || built) {
            add(out, fileName, at, Severity.CRITICAL, what + " runs a command assembled from variable data"
                    + source.map(s -> " that includes user input (" + s + ").").orElse("."));
        } else if (dynamic) {
            add(out, fileName, at, Severity.WARNING, what
                    + " runs a command taken from a variable. Make sure it can never contain user input.");
        }
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (call.getNameAsString().equals("exec") && SecurityHelpers.scopeText(call).endsWith("getRuntime()")
                    && !call.getArguments().isEmpty()) {
                Expression first = call.getArgument(0);
                List<Expression> arguments = new ArrayList<>();
                if (first.isArrayCreationExpr() && first.asArrayCreationExpr().getInitializer().isPresent()) {
                    arguments.addAll(first.asArrayCreationExpr().getInitializer().get().getValues());
                } else if (first.isArrayInitializerExpr()) {
                    arguments.addAll(first.asArrayInitializerExpr().getValues());
                } else {
                    arguments.add(first);
                }
                check(out, fileName, call, arguments, "Runtime.exec()");
            }
        }
        for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
            if (creation.getType().getNameAsString().equals("ProcessBuilder")
                    && !creation.getArguments().isEmpty()) {
                check(out, fileName, creation, new ArrayList<>(creation.getArguments()), "ProcessBuilder");
            }
        }
    }

}
