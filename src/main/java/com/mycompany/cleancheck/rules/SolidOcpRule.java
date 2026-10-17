package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.stmt.SwitchStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

/** OCP heuristic: large switch statements. */
public class SolidOcpRule implements Rule {
    private final int maxEntries;
    public SolidOcpRule() { this(8); }
    public SolidOcpRule(int maxEntries) { this.maxEntries = maxEntries; }
    @Override public String getName() { return "SOLID OCP"; }
    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        JavaAst.parse(source).ifPresent(cu -> {
            for (SwitchStmt sw : cu.findAll(SwitchStmt.class)) {
                if (sw.getEntries().size() > maxEntries) {
                    warnings.add(new Warning(fileName, JavaAst.line(sw), getName(), Severity.INFO,
                            "Switch has " + sw.getEntries().size() + " branches (limit " + maxEntries + ").",
                            "Prefer polymorphism or strategies (Open/Closed Principle)."));
                }
            }
        });
        return warnings;
    }
}
