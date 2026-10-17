package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** LSP heuristic: override that only throws UnsupportedOperationException. */
public class SolidLspRule implements Rule {
    @Override public String getName() { return "SOLID LSP"; }
    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        JavaAst.parse(source).ifPresent(cu -> {
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                boolean isOverride = method.getAnnotations().stream()
                        .anyMatch(a -> a.getNameAsString().equals("Override"));
                if (!isOverride || method.getBody().isEmpty()) continue;
                BlockStmt body = method.getBody().get();
                if (body.getStatements().size() == 1
                        && body.getStatement(0) instanceof ThrowStmt ts
                        && ts.toString().contains("UnsupportedOperationException")) {
                    warnings.add(new Warning(fileName, JavaAst.line(method), getName(), Severity.WARNING,
                            "Override of " + method.getNameAsString()
                                    + " only throws UnsupportedOperationException.",
                            "Subtypes should honour the base contract (Liskov Substitution Principle)."));
                }
            }
        });
        return warnings;
    }
}
