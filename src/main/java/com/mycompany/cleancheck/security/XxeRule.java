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

public final class XxeRule extends SecurityRuleBase {


    private static final Set<String> FACTORIES = Set.of(
            "DocumentBuilderFactory", "SAXParserFactory", "XMLInputFactory", "TransformerFactory",
            "SchemaFactory", "XMLReaderFactory");
    private static final Set<String> PARSER_BUILDERS = Set.of(
            "newDocumentBuilder", "newSAXParser", "createXMLStreamReader", "createXMLEventReader",
            "newTransformer", "createXMLReader");
    private static final List<String> HARDENING = List.of(
            "disallow-doctype-decl", "FEATURE_SECURE_PROCESSING", "IS_SUPPORTING_EXTERNAL_ENTITIES",
            "ACCESS_EXTERNAL_DTD", "SUPPORT_DTD", "external-general-entities",
            "http://apache.org/xml/features/disallow-doctype-decl",
            "http://xml.org/sax/features/external-general-entities");

    public XxeRule() {
        super(SecurityCatalog.XXE);
    }

    @Override
    protected void scan(CompilationUnit cu, String fileName, List<Warning> out) {
        // One finding per method (or initializer), not one per call in the chain
        // DocumentBuilderFactory.newInstance().newDocumentBuilder() must not produce two rows.
        Set<String> reported = new HashSet<>();

        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            String body = method.toString();
            if (HARDENING.stream().anyMatch(body::contains)) {
                continue;
            }
            MethodCallExpr hit = null;
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                String name = call.getNameAsString();
                if (PARSER_BUILDERS.contains(name) && (isFactoryChain(call) || isFactory(call))) {
                    hit = call; // prefer the builder call at the end of the chain
                    break;
                }
                if (hit == null && "newInstance".equals(name) && isFactory(call)) {
                    hit = call;
                }
            }
            if (hit != null) {
                String key = fileName + ":" + JavaAst.line(hit) + ":" + getName();
                if (reported.add(key)) {
                    add(out, fileName, hit, Severity.CRITICAL,
                            "XML parser created with default settings (XXE / CWE-611). "
                                    + "Disable DOCTYPE and external entities before parse.");
                }
            }
        }

        for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
            if (call.findAncestor(MethodDeclaration.class).isPresent()) {
                continue;
            }
            String name = call.getNameAsString();
            boolean match = ("newInstance".equals(name) && isFactory(call))
                    || (PARSER_BUILDERS.contains(name) && (isFactoryChain(call) || isFactory(call)));
            if (!match) {
                continue;
            }
            String key = fileName + ":" + JavaAst.line(call) + ":" + getName();
            if (reported.add(key)) {
                add(out, fileName, call, Severity.CRITICAL,
                        "XML parser factory used with default settings (XXE / CWE-611).");
            }
        }
    }

    private static boolean isFactory(MethodCallExpr call) {
        if (FACTORIES.stream().anyMatch(factory -> SecurityHelpers.scopeIs(call, factory))) {
            return true;
        }
        // DocumentBuilderFactory.newInstance() as TypeExpr scope
        return call.getScope().map(scope -> {
            String s = scope.toString();
            return FACTORIES.stream().anyMatch(f -> s.equals(f) || s.endsWith("." + f));
        }).orElse(false);
    }

    private static boolean isFactoryChain(MethodCallExpr call) {
        return call.getScope().map(scope -> {
            if (scope.isMethodCallExpr()) {
                MethodCallExpr inner = scope.asMethodCallExpr();
                return "newInstance".equals(inner.getNameAsString()) && isFactory(inner);
            }
            return FACTORIES.stream().anyMatch(f -> scope.toString().contains(f));
        }).orElse(false);
    }

}
