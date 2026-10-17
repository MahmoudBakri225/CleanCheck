package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class NameConventionRule implements Rule {

    @Override
    public String getName() {
        return "Naming Convention";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                String name = type.getNameAsString();
                if (!name.isEmpty() && !Character.isUpperCase(name.charAt(0))) {
                    warnings.add(warn(fileName, JavaAst.line(type),
                            "Type name '" + name + "' should start with uppercase.",
                            "Use PascalCase for class/interface names."));
                }
            }

            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                String name = method.getNameAsString();
                if (!name.isEmpty() && Character.isUpperCase(name.charAt(0))) {
                    warnings.add(warn(fileName, JavaAst.line(method),
                            "Method '" + name + "' should start with lowercase.",
                            "Use camelCase for method names."));
                }
            }

            for (FieldDeclaration field : cu.findAll(FieldDeclaration.class)) {
                for (VariableDeclarator variable : field.getVariables()) {
                    String name = variable.getNameAsString();
                    if (isConstantName(name) || name.isEmpty()) {
                        continue;
                    }
                    if (Character.isUpperCase(name.charAt(0))) {
                        warnings.add(warn(fileName, JavaAst.line(variable),
                                "Field '" + name + "' should start with lowercase.",
                                "Use camelCase for fields (unless it is a constant)."));
                    }
                }
            }

            for (Parameter parameter : cu.findAll(Parameter.class)) {
                String name = parameter.getNameAsString();
                if (!name.isEmpty() && Character.isUpperCase(name.charAt(0))) {
                    warnings.add(warn(fileName, JavaAst.line(parameter),
                            "Parameter '" + name + "' should start with lowercase.",
                            "Use camelCase for parameters."));
                }
            }
        });

        return warnings;
    }

    private boolean isConstantName(String name) {
        return name.equals(name.toUpperCase()) && name.length() > 1;
    }

    private Warning warn(String file, int line, String message, String suggestion) {
        return new Warning(file, line, getName(), Severity.WARNING, message, suggestion);
    }
}
