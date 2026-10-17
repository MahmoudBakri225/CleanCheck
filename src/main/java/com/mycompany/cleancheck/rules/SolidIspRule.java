package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** ISP heuristic: wide interfaces. */
public class SolidIspRule implements Rule {
    private final int maxMethods;
    public SolidIspRule() { this(10); }
    public SolidIspRule(int maxMethods) { this.maxMethods = maxMethods; }
    @Override public String getName() { return "SOLID ISP"; }
    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        JavaAst.parse(source).ifPresent(cu -> {
            for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
                if (!type.isInterface()) continue;
                int methods = type.findAll(MethodDeclaration.class).size();
                if (methods > maxMethods) {
                    warnings.add(new Warning(fileName, JavaAst.line(type), getName(), Severity.INFO,
                            "Interface " + type.getNameAsString() + " declares " + methods
                                    + " methods (limit " + maxMethods + ").",
                            "Split into smaller role-focused interfaces (Interface Segregation Principle)."));
                }
            }
        });
        return warnings;
    }
}
