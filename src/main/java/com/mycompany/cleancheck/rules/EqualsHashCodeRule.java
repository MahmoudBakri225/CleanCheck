package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** A class that overrides equals(Object) without hashCode() (or the other way round). */
public class EqualsHashCodeRule implements Rule {

    @Override
    public String getName() {
        return "Equals HashCode";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (type.isInterface()) {
                    continue;
                }
                var equals = type.getMethods().stream()
                        .filter(m -> m.getNameAsString().equals("equals")
                                && m.getParameters().size() == 1
                                && m.getParameter(0).getType().asString().matches("(java\\.lang\\.)?Object"))
                        .findFirst();
                var hashCode = type.getMethods().stream()
                        .filter(m -> m.getNameAsString().equals("hashCode") && m.getParameters().isEmpty())
                        .findFirst();

                if (equals.isPresent() && hashCode.isEmpty()) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(equals.get()),
                            getName(),
                            Severity.CRITICAL,
                            "Class '" + type.getNameAsString() + "' overrides equals() but not hashCode().",
                            "Override hashCode() with the same fields, or use Objects.hash(...)."
                    ));
                } else if (hashCode.isPresent() && equals.isEmpty()) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(hashCode.get()),
                            getName(),
                            Severity.WARNING,
                            "Class '" + type.getNameAsString() + "' overrides hashCode() but not equals().",
                            "Override equals(Object) consistently with hashCode()."
                    ));
                }
            }
        });

        return warnings;
    }
}
