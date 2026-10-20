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

public final class SqlInjectionRule extends SecurityRuleBase {


    private static final Set<String> SQL_CALLS = Set.of(
            "executeQuery", "executeUpdate", "execute", "executeLargeUpdate", "prepareStatement",
            "prepareCall", "createQuery", "createNativeQuery", "addBatch", "queryForObject", "queryForList");
    private static final Set<String> SQL_RECEIVER_TYPES = Set.of(
            "Statement", "PreparedStatement", "CallableStatement", "Connection",
            "JdbcTemplate", "NamedParameterJdbcTemplate", "JdbcOperations",
            "EntityManager", "Query", "SQLQuery", "DSLContext");

    public SqlInjectionRule() {
        super(SecurityCatalog.SQL_INJECTION);
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (!SQL_CALLS.contains(call.getNameAsString()) || call.getArguments().isEmpty()
                    || !hasSqlReceiver(call, cu)) {
                continue;
            }
            Expression query = call.getArgument(0);
            if (!SecurityAst.isBuiltDynamically(query, call)) {
                continue;
            }
            Optional<String> source = new SecurityAst.Flow(SecurityAst.callableOf(call)).sourceIn(query);
            // Dynamic SQL is always reported; parameter/user taint makes the message clearer.
            String message = "The query passed to " + call.getNameAsString()
                    + "() is built by joining strings"
                    + source.map(s -> ", and untrusted input (" + s + ") reaches it.")
                            .orElse(" (use a PreparedStatement with placeholders).");
            add(out, fileName, call, Severity.CRITICAL, message);
        }
    }

    private static boolean hasSqlReceiver(MethodCallExpr call, CompilationUnit unit) {
        if (call.getScope().isEmpty()) {
            return false;
        }
        Expression scope = call.getScope().get();
        if (scope.isMethodCallExpr()) {
            String factory = scope.asMethodCallExpr().getNameAsString();
            if (factory.equals("createStatement") || factory.equals("prepareStatement")
                    || factory.equals("prepareCall")) {
                return true;
            }
        }
        String variable = null;
        if (scope.isNameExpr()) {
            variable = scope.asNameExpr().getNameAsString();
        } else if (scope.isFieldAccessExpr()) {
            variable = scope.asFieldAccessExpr().getNameAsString();
        }
        if (variable == null) {
            return false;
        }
        final String receiverName = variable;
        boolean declaredSqlVariable = unit.findAll(VariableDeclarator.class).stream()
                .filter(declaration -> declaration.getNameAsString().equals(receiverName))
                .anyMatch(declaration -> isSqlReceiverType(declaration.getType().asString()));
        if (declaredSqlVariable) {
            return true;
        }
        return unit.findAll(Parameter.class).stream()
                .filter(parameter -> parameter.getNameAsString().equals(receiverName))
                .anyMatch(parameter -> isSqlReceiverType(parameter.getType().asString()));
    }

    private static boolean isSqlReceiverType(String type) {
        int genericStart = type.indexOf('<');
        String erased = genericStart < 0 ? type : type.substring(0, genericStart);
        int packageSeparator = erased.lastIndexOf('.');
        String simpleName = packageSeparator < 0 ? erased : erased.substring(packageSeparator + 1);
        return SQL_RECEIVER_TYPES.contains(simpleName);
    }

}
