package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class TooManyParametersRule implements Rule {

    private final int max;

    public TooManyParametersRule() {
        this(4);
    }

    public TooManyParametersRule(int max) {
        this.max = max;
    }

    @Override
    public String getName() {
        return "Too Many Parameters";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                int count = method.getParameters().size();
                if (count > max) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(method),
                            getName(),
                            Severity.WARNING,
                            "Method '" + method.getNameAsString() + "' has " + count + " parameters.",
                            "Group related values into a small class or record."
                    ));
                }
            }
        });

        return warnings;
    }
}
