package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.SimpleName;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnusedImportRule implements Rule {

    private static final Pattern WORD = Pattern.compile("[A-Za-z_$][\\w$]*");

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
            // A name that only appears in Javadoc ({@link Foo}) still justifies its import.
            for (Comment comment : cu.getAllComments()) {
                Matcher m = WORD.matcher(comment.getContent());
                while (m.find()) {
                    used.add(m.group());
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
