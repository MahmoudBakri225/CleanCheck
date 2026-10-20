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

public final class CertificateCheckRule extends SecurityRuleBase {


    private static final Set<String> TRUST_METHODS = Set.of(
            "checkServerTrusted", "checkClientTrusted", "checkTrusted");
    private static final Set<String> TRUST_ALL_NAMES = Set.of(
            "ALLOW_ALL_HOSTNAME_VERIFIER", "NoopHostnameVerifier", "TrustAllStrategy",
            "InsecureTrustManagerFactory");

    public CertificateCheckRule() {
        super(SecurityCatalog.CERTIFICATE_CHECK);
    }

    private static boolean returnsOnlyTrue(Statement body) {
        if (body.isExpressionStmt()) {
            Expression expression = body.asExpressionStmt().getExpression();
            return expression.isBooleanLiteralExpr() && expression.asBooleanLiteralExpr().getValue();
        }
        List<ReturnStmt> returns = body.findAll(ReturnStmt.class);
        if (returns.isEmpty()) {
            return false;
        }
        for (ReturnStmt statement : returns) {
            if (statement.getExpression().isEmpty()) {
                return false;
            }
            Expression value = statement.getExpression().get();
            if (!(value.isBooleanLiteralExpr() && value.asBooleanLiteralExpr().getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            if (TRUST_METHODS.contains(method.getNameAsString()) && method.getBody().isPresent()
                    && method.getBody().get().getStatements().isEmpty()) {
                add(out, fileName, method, Severity.CRITICAL, method.getNameAsString()
                        + "() is empty, so this TrustManager accepts every certificate.");
            } else if (method.getNameAsString().equals("verify") && method.getParameters().size() == 2
                    && method.getParameter(1).getType().asString().endsWith("SSLSession")
                    && method.getBody().isPresent() && returnsOnlyTrue(method.getBody().get())) {
                add(out, fileName, method, Severity.CRITICAL,
                        "This HostnameVerifier always returns true, so any host name is accepted.");
            }
        }
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            boolean verifierSetter = call.getNameAsString().equals("setHostnameVerifier")
                    || call.getNameAsString().equals("setDefaultHostnameVerifier");
            if (verifierSetter && !call.getArguments().isEmpty() && call.getArgument(0).isLambdaExpr()) {
                LambdaExpr lambda = call.getArgument(0).asLambdaExpr();
                if (returnsOnlyTrue(lambda.getBody())) {
                    add(out, fileName, call, Severity.CRITICAL,
                            "This HostnameVerifier always returns true, so any host name is accepted.");
                }
            }
        }
        for (NameExpr name : cu.findAll(NameExpr.class)) {
            if (TRUST_ALL_NAMES.contains(name.getNameAsString())) {
                add(out, fileName, name, Severity.CRITICAL,
                        name.getNameAsString() + " turns certificate or host name checking off.");
            }
        }
        for (FieldAccessExpr field : cu.findAll(FieldAccessExpr.class)) {
            if (TRUST_ALL_NAMES.contains(field.getNameAsString())) {
                add(out, fileName, field, Severity.CRITICAL,
                        field.getNameAsString() + " turns certificate or host name checking off.");
            }
        }
        for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
            if (TRUST_ALL_NAMES.contains(creation.getType().getNameAsString())) {
                add(out, fileName, creation, Severity.CRITICAL,
                        creation.getType().getNameAsString() + " turns certificate or host name checking off.");
            }
        }
    }

}
