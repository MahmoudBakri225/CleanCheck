package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** SRP heuristic: many public behaviour methods on one concrete class. */
public class SolidSrpRule implements Rule {
    private final int maxPublicMethods;
    public SolidSrpRule() { this(12); }
    public SolidSrpRule(int maxPublicMethods) { this.maxPublicMethods = maxPublicMethods; }
    @Override public String getName() { return "SOLID SRP"; }
    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        JavaAst.parse(source).ifPresent(cu -> {
            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (type.isInterface() || type.isAbstract()) continue;
                long publicMethods = type.findAll(MethodDeclaration.class).stream()
                        .filter(m -> m.isPublic() && !m.isStatic())
                        .filter(m -> {
                            String n = m.getNameAsString();
                            return !n.startsWith("get") && !n.startsWith("set") && !n.startsWith("is");
                        })
                        .count();
                if (publicMethods > maxPublicMethods) {
                    warnings.add(new Warning(fileName, JavaAst.line(type), getName(), Severity.INFO,
                            "Class " + type.getNameAsString() + " has " + publicMethods
                                    + " public behaviour methods (limit " + maxPublicMethods + ").",
                            "Split responsibilities into smaller classes (Single Responsibility Principle)."));
                }
            }
        });
        return warnings;
    }
}
