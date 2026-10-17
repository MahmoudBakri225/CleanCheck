package com.mycompany.cleancheck.security;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.mycompany.cleancheck.analysis.JavaAst;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AST helpers for security rules, including a simple taint check.
 * Flow tracks user input inside a method and also class fields that were
 * assigned from sources in the same class (same-class inter-method leakage).
 */
final class SecurityAst {

    private static final Set<String> SOURCES = Set.of(
            "nextLine", "next", "nextInt", "readLine", "read", "getParameter",
            "getHeader", "getQueryString", "getInputStream", "readAllBytes", "readString"
    );

    private SecurityAst() {
    }

    static int line(Node node) {
        return JavaAst.line(node);
    }

    /** Method, constructor, or initializer that contains the node. */
    static Node callableOf(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof MethodDeclaration
                    || current instanceof ConstructorDeclaration
                    || current instanceof InitializerDeclaration) {
                return current;
            }
            current = current.getParentNode().orElse(null);
        }
        return node;
    }

    /** Enclosing class/interface for same-class field taint. */
    static Optional<ClassOrInterfaceDeclaration> classOf(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof ClassOrInterfaceDeclaration c) {
                return Optional.of(c);
            }
            current = current.getParentNode().orElse(null);
        }
        return Optional.empty();
    }

    static boolean isDynamic(Expression expression) {
        return !expression.isStringLiteralExpr()
                && !expression.isIntegerLiteralExpr()
                && !expression.isBooleanLiteralExpr()
                && !expression.isNullLiteralExpr()
                && !expression.isCharLiteralExpr()
                && !expression.isDoubleLiteralExpr()
                && !expression.isLongLiteralExpr();
    }

    /** True when the expression is assembled dynamically (concatenation / format), not a plain literal. */
    static boolean isBuiltDynamically(Expression expression, Node context) {
        return built(expression, context, 0);
    }

    static boolean built(Expression e, Node context, int depth) {
        if (depth > 4) {
            return false;
        }
        if (e.isStringLiteralExpr() || e.isNullLiteralExpr()) {
            return false;
        }
        if (e.isBinaryExpr()) {
            BinaryExpr binary = e.asBinaryExpr();
            if (binary.getOperator() == BinaryExpr.Operator.PLUS) {
                return built(binary.getLeft(), context, depth + 1)
                        || built(binary.getRight(), context, depth + 1);
            }
        }
        if (e.isMethodCallExpr()) {
            MethodCallExpr call = e.asMethodCallExpr();
            String name = call.getNameAsString();
            if (name.equals("format") || name.equals("formatted")) {
                return call.getArguments().stream().anyMatch(SecurityAst::isDynamic);
            }
            if (name.equals("toString") && call.getScope().isPresent()) {
                return dynamicBuilder(call.getScope().get(), context, depth + 1);
            }
            if (name.equals("append")) {
                return call.getArguments().stream().anyMatch(SecurityAst::isDynamic)
                        || call.getScope().map(scope -> dynamicBuilder(scope, context, depth + 1)).orElse(false);
            }
            return false;
        }
        if (e.isNameExpr()) {
            // A variable/parameter inside string building is always dynamic
            // (e.g. "SELECT ... '" + name + "'"). Without this, SQL/command injection
            // on method parameters is missed.
            if (depth > 0) {
                return true;
            }
            String variable = e.asNameExpr().getNameAsString();
            Node scope = callableOf(context);
            // Method parameters are untrusted input for injection rules.
            if (scope instanceof MethodDeclaration method) {
                for (com.github.javaparser.ast.body.Parameter parameter : method.getParameters()) {
                    if (parameter.getNameAsString().equals(variable)) {
                        return true;
                    }
                }
            }
            for (VariableDeclarator declarator : scope.findAll(VariableDeclarator.class)) {
                if (declarator.getNameAsString().equals(variable)
                        && declarator.getInitializer().isPresent()
                        && built(declarator.getInitializer().get(), context, depth + 1)) {
                    return true;
                }
            }
            for (AssignExpr assign : scope.findAll(AssignExpr.class)) {
                if (assign.getTarget().isNameExpr()
                        && assign.getTarget().asNameExpr().getNameAsString().equals(variable)
                        && assign.getOperator() == AssignExpr.Operator.ASSIGN
                        && built(assign.getValue(), context, depth + 1)) {
                    return true;
                }
            }
            Optional<ClassOrInterfaceDeclaration> type = classOf(context);
            if (type.isPresent()) {
                for (FieldDeclaration field : type.get().findAll(FieldDeclaration.class)) {
                    for (VariableDeclarator declarator : field.getVariables()) {
                        if (declarator.getNameAsString().equals(variable)
                                && declarator.getInitializer().isPresent()
                                && built(declarator.getInitializer().get(), context, depth + 1)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Follows a StringBuilder/StringBuffer expression and appends in the current callable only. */
    private static boolean dynamicBuilder(Expression builder, Node context, int depth) {
        if (depth > 6) {
            return false;
        }
        if (builder.isObjectCreationExpr()) {
            ObjectCreationExpr creation = builder.asObjectCreationExpr();
            String type = creation.getType().getNameAsString();
            return (type.equals("StringBuilder") || type.equals("StringBuffer"))
                    && creation.getArguments().stream().anyMatch(SecurityAst::isDynamic);
        }
        if (builder.isMethodCallExpr()) {
            MethodCallExpr call = builder.asMethodCallExpr();
            if (call.getNameAsString().equals("append")) {
                return call.getArguments().stream().anyMatch(SecurityAst::isDynamic)
                        || call.getScope().map(scope -> dynamicBuilder(scope, context, depth + 1)).orElse(false);
            }
            return false;
        }
        if (!builder.isNameExpr()) {
            return false;
        }

        String name = builder.asNameExpr().getNameAsString();
        Node callable = callableOf(context);
        boolean isBuilder = callable.findAll(VariableDeclarator.class).stream()
                .filter(variable -> variable.getNameAsString().equals(name))
                .anyMatch(variable -> {
                    String type = variable.getType().asString();
                    return type.equals("StringBuilder") || type.equals("StringBuffer");
                });
        if (!isBuilder) {
            return false;
        }
        for (MethodCallExpr append : callable.findAll(MethodCallExpr.class)) {
            if (append.getNameAsString().equals("append")
                    && append.getScope().filter(Expression::isNameExpr).map(Expression::asNameExpr)
                            .map(scope -> scope.getNameAsString().equals(name)).orElse(false)
                    && append.getArguments().stream().anyMatch(SecurityAst::isDynamic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Variables holding user input in one method, plus class fields tainted by
     * sources anywhere in the same class.
     */
    static final class Flow {

        private final Map<String, String> tainted = new HashMap<>();
        private final java.util.Set<String> taintedReturns = new java.util.HashSet<>();

        Flow(Node scope) {
            Optional<ClassOrInterfaceDeclaration> type = classOf(scope);
            // Class fields first (same-class inter-method)
            if (type.isPresent()) {
                ClassOrInterfaceDeclaration clazz = type.get();
                for (int pass = 0; pass < 2; pass++) {
                    for (FieldDeclaration field : clazz.findAll(FieldDeclaration.class)) {
                        for (VariableDeclarator declarator : field.getVariables()) {
                            if (declarator.getInitializer().isPresent()) {
                                Optional<String> origin = sourceIn(declarator.getInitializer().get());
                                if (origin.isPresent()) {
                                    tainted.putIfAbsent(declarator.getNameAsString(), origin.get());
                                }
                            }
                        }
                    }
                    for (MethodDeclaration method : clazz.findAll(MethodDeclaration.class)) {
                        collectFromScope(method);
                    }
                    for (ConstructorDeclaration ctor : clazz.findAll(ConstructorDeclaration.class)) {
                        collectFromScope(ctor);
                    }
                }
            }
            collectFromScope(scope);
            // Methods whose return expression is tainted → calling them taints the destination
            if (type.isPresent()) {
                for (MethodDeclaration method : type.get().findAll(MethodDeclaration.class)) {
                    if (method.getBody().isEmpty()) {
                        continue;
                    }
                    method.getBody().get().findAll(com.github.javaparser.ast.stmt.ReturnStmt.class).forEach(ret -> {
                        if (ret.getExpression().isPresent()
                                && sourceIn(ret.getExpression().get()).isPresent()) {
                            taintedReturns.add(method.getNameAsString());
                        }
                    });
                }
                for (MethodCallExpr call : type.get().findAll(MethodCallExpr.class)) {
                    if (taintedReturns.contains(call.getNameAsString())) {
                        // caller site itself is a source-like expression when assigned
                        // handled via sourceIn MethodCallExpr check below
                    }
                }
            }
        }

        private void collectFromScope(Node scope) {
            // Formal parameters are treated as untrusted (typical for SQL / command injection demos).
            if (scope instanceof MethodDeclaration method) {
                for (com.github.javaparser.ast.body.Parameter parameter : method.getParameters()) {
                    String pname = parameter.getNameAsString();
                    if (!pname.equals("this") && !parameter.getType().isPrimitiveType()) {
                        tainted.putIfAbsent(pname, "parameter '" + pname + "'");
                    }
                }
            }
            for (int pass = 0; pass < 3; pass++) {
                for (VariableDeclarator declarator : scope.findAll(VariableDeclarator.class)) {
                    if (declarator.getInitializer().isPresent()) {
                        Optional<String> origin = sourceIn(declarator.getInitializer().get());
                        if (origin.isPresent()) {
                            tainted.putIfAbsent(declarator.getNameAsString(), origin.get());
                        }
                    }
                }
                for (AssignExpr assign : scope.findAll(AssignExpr.class)) {
                    if (assign.getTarget().isNameExpr()) {
                        Optional<String> origin = sourceIn(assign.getValue());
                        if (origin.isPresent()) {
                            tainted.putIfAbsent(assign.getTarget().asNameExpr().getNameAsString(), origin.get());
                        }
                    }
                }
            }
        }

        Optional<String> sourceIn(Expression expression) {
            for (MethodCallExpr call : expression.findAll(MethodCallExpr.class)) {
                if (isLocalFileRead(call)) {
                    continue;
                }
                if (SOURCES.contains(call.getNameAsString())) {
                    return Optional.of(call.getNameAsString() + "() on line " + JavaAst.line(call));
                }
                if (taintedReturns.contains(call.getNameAsString())) {
                    return Optional.of("return of " + call.getNameAsString() + "() on line " + JavaAst.line(call));
                }
            }
            for (ArrayAccessExpr access : expression.findAll(ArrayAccessExpr.class)) {
                if (access.getName().isNameExpr()
                        && access.getName().asNameExpr().getNameAsString().equals("args")) {
                    return Optional.of("a command-line argument on line " + JavaAst.line(access));
                }
            }
            for (NameExpr name : expression.findAll(NameExpr.class)) {
                String origin = tainted.get(name.getNameAsString());
                if (origin != null) {
                    return Optional.of(origin);
                }
            }
            return Optional.empty();
        }

        private static boolean isLocalFileRead(MethodCallExpr call) {
            if (!Set.of("readString", "readAllBytes").contains(call.getNameAsString())
                    || call.getScope().isEmpty()) {
                return false;
            }
            String scope = call.getScope().get().toString();
            return scope.equals("Files") || scope.endsWith(".Files");
        }
    }
}
