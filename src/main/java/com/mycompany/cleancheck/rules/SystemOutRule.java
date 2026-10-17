package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class SystemOutRule implements Rule {

    @Override
    public String getName() {
        return "System Out";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (MethodCallExpr call : cu.findAll(MethodCallExpr.class)) {
                String code = call.toString();
                if (code.startsWith("System.out.") || code.startsWith("System.err.")) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(call),
                            getName(),
                            Severity.INFO,
                            "Direct console output: " + call.getNameAsString() + "().",
                            "Prefer a logger in real projects instead of System.out/err."
                    ));
                }
            }
        });

        return warnings;
    }
}
