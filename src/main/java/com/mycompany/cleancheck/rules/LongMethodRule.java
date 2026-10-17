package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class LongMethodRule implements Rule {

    private final int maxLines;

    public LongMethodRule() {
        this(40);
    }

    public LongMethodRule(int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public String getName() {
        return "Long Method";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                int begin = method.getBegin().map(p -> p.line).orElse(0);
                int end = method.getEnd().map(p -> p.line).orElse(begin);
                int lines = end - begin + 1;
                if (lines > maxLines) {
                    warnings.add(new Warning(
                            fileName,
                            begin,
                            getName(),
                            Severity.WARNING,
                            "Method '" + method.getNameAsString() + "' is too long (" + lines + " lines).",
                            "Split it into smaller methods with clear names (limit: " + maxLines + " lines)."
                    ));
                }
            }
        });

        return warnings;
    }
}
