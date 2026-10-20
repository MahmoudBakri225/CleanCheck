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

public final class UnsafeDeserializationRule extends SecurityRuleBase {


    public UnsafeDeserializationRule() {
        super(SecurityCatalog.UNSAFE_DESERIALIZATION);
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        String text = cu.toString();
        boolean filtered = text.contains("ObjectInputFilter");
        for (ObjectCreationExpr creation : cu.findAll(ObjectCreationExpr.class)) {
            String type = creation.getType().getNameAsString();
            if (type.equals("ObjectInputStream") && !filtered) {
                boolean restrictsClasses = creation.getAnonymousClassBody().isPresent()
                        && creation.getAnonymousClassBody().get().toString().contains("resolveClass");
                if (!restrictsClasses) {
                    add(out, fileName, creation, Severity.CRITICAL,
                            "ObjectInputStream reads Java objects without a class filter. Untrusted bytes can run code.");
                }
            } else if (type.equals("XMLDecoder")) {
                add(out, fileName, creation, Severity.CRITICAL,
                        "XMLDecoder can instantiate any class named in the XML. Untrusted XML can run code.");
            }
        }
    }

}
