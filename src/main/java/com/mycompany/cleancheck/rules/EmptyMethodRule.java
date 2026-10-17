package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class EmptyMethodRule implements Rule {

    @Override
    public String getName() {
        return "Empty Method";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                if (method.isAbstract() || method.getBody().isEmpty()) {
                    continue;
                }
                if (method.getBody().get().getStatements().isEmpty()) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(method),
                            getName(),
                            Severity.INFO,
                            "Empty method '" + method.getNameAsString() + "'.",
                            "Write the logic or remove the method if it is not used."
                    ));
                }
            }

            // Constructors are intentionally excluded. An empty constructor is often valid Java.
        });

        return warnings;
    }
}
