package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnusedImportRule implements Rule {


    @Override
    public String getName() {
        return "Unused Import";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            Set<String> used = new HashSet<>();
            for (SimpleName name : cu.findAll(SimpleName.class)) {
                if (name.findAncestor(ImportDeclaration.class).isEmpty()) {
                    used.add(name.getIdentifier());
                }
            }
            for (Name name : cu.findAll(Name.class)) {
                if (name.findAncestor(ImportDeclaration.class).isEmpty()
                        && name.findAncestor(PackageDeclaration.class).isEmpty()) {
                    used.add(name.getIdentifier());
                }
            }
            for (ImportDeclaration declaration : cu.getImports()) {
                if (declaration.isAsterisk()) {
                    continue;
                }
                String simple = declaration.getName().getIdentifier();
                if (!used.contains(simple)) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(declaration),
                            getName(),
                            Severity.INFO,
                            "Import '" + declaration.getNameAsString() + "' is never used.",
                            "Remove the unused import."
                    ));
                }
            }
        });

        return warnings;
    }
}
