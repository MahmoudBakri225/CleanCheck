package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** DIP heuristic: field wired with new ConcreteType(). */
public class SolidDipRule implements Rule {
    private static final Set<String> IGNORE = Set.of(
            "String", "StringBuilder", "ArrayList", "HashMap", "HashSet", "LinkedList",
            "Optional", "Path", "File", "Scanner", "Object");
    @Override public String getName() { return "SOLID DIP"; }
    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        JavaAst.parse(source).ifPresent(cu -> {
            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                field.getVariables().forEach(variable -> {
                    if (variable.getInitializer().isEmpty()) return;
                    if (!(variable.getInitializer().get() instanceof ObjectCreationExpr create)) return;
                    String typeName = create.getType().getNameAsString();
                    if (IGNORE.contains(typeName)) return;
                    if (field.getElementType() instanceof ClassOrInterfaceType declared
                            && declared.getNameAsString().equals(typeName)) {
                        warnings.add(new Warning(fileName, JavaAst.line(field), getName(), Severity.INFO,
                                "Field depends on concrete type " + typeName + " via new.",
                                "Depend on an interface or inject the dependency (Dependency Inversion Principle)."));
                    }
                });
            }
        });
        return warnings;
    }
}
