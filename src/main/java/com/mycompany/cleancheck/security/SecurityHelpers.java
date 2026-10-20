package com.mycompany.cleancheck.security;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Optional;

/** Small shared helpers used by multiple security rules. */
public final class SecurityHelpers {

    private SecurityHelpers() {}

    public static String scopeText(MethodCallExpr call) {
        return call.getScope().map(Expression::toString).orElse("");
    }

    public static boolean scopeIs(MethodCallExpr call, String simpleName) {
        String scope = scopeText(call);
        return scope.equals(simpleName) || scope.endsWith("." + simpleName);
    }

    public static Optional<String> stringArg(MethodCallExpr call, int index) {
        if (call.getArguments().size() > index && call.getArgument(index).isStringLiteralExpr()) {
            return Optional.of(call.getArgument(index).asStringLiteralExpr().asString());
        }
        return Optional.empty();
    }
}
