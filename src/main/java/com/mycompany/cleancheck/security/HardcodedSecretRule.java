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

public final class HardcodedSecretRule extends SecurityRuleBase {


    private static final List<String> SUFFIXES = List.of(
            "password", "passwd", "pwd", "passphrase", "secret", "secretkey", "apikey", "accesskey",
            "privatekey", "token", "credentials");
    private static final Pattern AWS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN ([A-Z]+ )?PRIVATE KEY-----");

    public HardcodedSecretRule() {
        super(SecurityCatalog.HARDCODED_SECRET);
    }

    private static boolean secretName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        for (String suffix : SUFFIXES) {
            if (normalized.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean realValue(Expression value) {
        if (!value.isStringLiteralExpr()) {
            return false;
        }
        String text = value.asStringLiteralExpr().asString();
        return !text.isBlank() && !text.contains(" ")
                && !text.startsWith("${") && !text.startsWith("<") && !text.startsWith("{") && !text.startsWith("%");
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        Set<Integer> reported = new HashSet<>();

        for (VariableDeclarator declarator : cu.findAll(VariableDeclarator.class)) {
            if (secretName(declarator.getNameAsString()) && declarator.getInitializer().isPresent()
                    && realValue(declarator.getInitializer().get()) && reported.add(JavaAst.line(declarator))) {
                add(out, fileName, declarator, Severity.CRITICAL, "'" + declarator.getNameAsString()
                        + "' is set to a literal value in the source code (value hidden).");
            }
        }
        for (AssignExpr assign : cu.findAll(AssignExpr.class)) {
            String target = assign.getTarget().isNameExpr()
                    ? assign.getTarget().asNameExpr().getNameAsString()
                    : assign.getTarget().isFieldAccessExpr()
                            ? assign.getTarget().asFieldAccessExpr().getNameAsString() : "";
            if (!target.isEmpty() && secretName(target) && realValue(assign.getValue())
                    && reported.add(JavaAst.line(assign))) {
                add(out, fileName, assign, Severity.CRITICAL,
                        "'" + target + "' is assigned a literal value in the source code (value hidden).");
            }
        }
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            String name = call.getNameAsString();
            boolean connection = name.equals("getConnection") && call.getArguments().size() == 3
                    && realValue(call.getArgument(2));
            boolean setter = name.startsWith("set") && secretName(name.substring(3))
                    && call.getArguments().size() == 1 && realValue(call.getArgument(0));
            if ((connection || setter) && reported.add(JavaAst.line(call))) {
                add(out, fileName, call, Severity.CRITICAL,
                        "A literal secret is passed to " + name + "() (value hidden).");
            }
        }
        for (StringLiteralExpr literal : cu.findAll(StringLiteralExpr.class)) {
            String text = literal.asString();
            if ((AWS_KEY.matcher(text).find() || PRIVATE_KEY.matcher(text).find())
                    && reported.add(JavaAst.line(literal))) {
                add(out, fileName, literal, Severity.CRITICAL,
                        "The text looks like a cloud access key or a private key (value hidden).");
            }
        }
    }

}
