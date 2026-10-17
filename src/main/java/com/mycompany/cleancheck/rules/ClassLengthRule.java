package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class ClassLengthRule implements Rule {

    private final int maxLines;

    public ClassLengthRule() {
        this(500);
    }

    public ClassLengthRule(int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public String getName() {
        return "Class Length";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            List<TypeDeclaration<?>> types = new ArrayList<>();
            types.addAll(cu.findAll(ClassOrInterfaceDeclaration.class));
            types.addAll(cu.findAll(EnumDeclaration.class));
            types.addAll(cu.findAll(RecordDeclaration.class));
            for (TypeDeclaration<?> type : types) {
                int begin = type.getBegin().map(p -> p.line).orElse(0);
                int end = type.getEnd().map(p -> p.line).orElse(begin);
                int lines = end - begin + 1;
                if (lines > maxLines) {
                    warnings.add(new Warning(
                            fileName,
                            begin,
                            getName(),
                            Severity.WARNING,
                            "Class '" + type.getNameAsString() + "' is " + lines + " lines long (limit " + maxLines + ").",
                            "Split responsibilities into smaller classes."
                    ));
                }
            }
        });

        return warnings;
    }
}
