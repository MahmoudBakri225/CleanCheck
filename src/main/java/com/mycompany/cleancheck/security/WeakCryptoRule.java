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

public final class WeakCryptoRule extends SecurityRuleBase {


    private static final Set<String> WEAK_HASHES = Set.of("MD2", "MD4", "MD5", "SHA-1", "SHA1");
    private static final Set<String> WEAK_CIPHERS = Set.of("DES", "DESEDE", "RC2", "RC4", "ARCFOUR", "BLOWFISH");

    public WeakCryptoRule() {
        super(SecurityCatalog.WEAK_CRYPTO);
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (!call.getNameAsString().equals("getInstance")) {
                continue;
            }
            Optional<String> argument = SecurityHelpers.stringArg(call, 0);
            if (argument.isEmpty()) {
                continue;
            }
            String value = argument.get();
            String upper = value.toUpperCase(Locale.ROOT);
            String algorithm = upper.split("/")[0];
            if (SecurityHelpers.scopeIs(call, "MessageDigest") && WEAK_HASHES.contains(upper)) {
                add(out, fileName, call, Severity.WARNING,
                        value + " is a broken hash: collisions can be created on purpose.");
            } else if ((SecurityHelpers.scopeIs(call, "Cipher") || SecurityHelpers.scopeIs(call, "KeyGenerator")
                    || SecurityHelpers.scopeIs(call, "SecretKeyFactory")) && WEAK_CIPHERS.contains(algorithm)) {
                add(out, fileName, call, Severity.WARNING,
                        value + " is a weak or obsolete cipher.",
                        "Use AES with an authenticated mode, for example AES/GCM/NoPadding.");
            } else if (SecurityHelpers.scopeIs(call, "Cipher") && upper.contains("/ECB")) {
                add(out, fileName, call, Severity.WARNING,
                        "ECB mode (" + value + ") encrypts equal blocks to equal output and leaks patterns.",
                        "Use an authenticated mode such as AES/GCM/NoPadding with a fresh random IV.");
            } else if (SecurityHelpers.scopeIs(call, "Cipher") && upper.equals("AES")) {
                add(out, fileName, call, Severity.WARNING,
                        "\"AES\" without a mode defaults to ECB. Name the mode explicitly, for example AES/GCM/NoPadding.",
                        "Replace \"AES\" with \"AES/GCM/NoPadding\" and use a fresh random IV for every encryption.");
            }
        }
    }

}
