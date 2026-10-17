package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class EncapsulationRule implements Rule {

    @Override
    public String getName() {
        return "Encapsulation";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                if (field.isPublic() && field.isStatic() && field.isFinal()) {
                    continue;
                }
                if (field.isPrivate()) {
                    continue;
                }

                String name = field.getVariables().isEmpty()
                        ? "field"
                        : field.getVariable(0).getNameAsString();

                warnings.add(new Warning(
                        fileName,
                        JavaAst.line(field),
                        getName(),
                        Severity.WARNING,
                        "Field '" + name + "' is not private.",
                        "Make it private and expose it with getters/setters only if needed."
                ));
            }
        });

        return warnings;
    }
}
