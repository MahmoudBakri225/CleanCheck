package com.mycompany.cleancheck.security;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.rules.Rule;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for all preliminary security rules.
 * Each rule scans a single file via the Java AST and produces Warning leads (not proofs).
 */
public abstract class SecurityRuleBase implements Rule {

    private final String name;

    protected SecurityRuleBase(String name) {
        this.name = name;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final List<Warning> analyze(String fileName, String source) {
        List<Warning> found = new ArrayList<>();
        JavaAst.parse(source).ifPresent(cu -> scan(cu, fileName, found));
        return found;
    }

    /** Implement the actual pattern search for this rule. */
    protected abstract void scan(CompilationUnit cu, String fileName, List<Warning> out);

    /** Add a finding with a custom fix suggestion. */
    protected final void add(List<Warning> out, String fileName, Node at, Severity severity, String message, String fix) {
        String cwe = SecurityCatalog.info(name).map(SecurityCatalog.Info::cwe).orElse("CWE");
        out.add(new Warning(fileName, JavaAst.line(at), name, severity, "[" + cwe + "] " + message, fix));
    }

    /** Add a finding using the catalog's default fix text. */
    protected final void add(List<Warning> out, String fileName, Node at, Severity severity, String message) {
        String cwe = SecurityCatalog.info(name).map(SecurityCatalog.Info::cwe).orElse("CWE");
        String fix = SecurityCatalog.info(name).map(SecurityCatalog.Info::fix).orElse("");
        out.add(new Warning(fileName, JavaAst.line(at), name, severity, "[" + cwe + "] " + message, fix));
    }
}
