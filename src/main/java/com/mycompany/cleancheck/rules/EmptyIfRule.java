package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class EmptyIfRule implements Rule {

    @Override
    public String getName() {
        return "Empty If";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (IfStmt ifStmt : cu.findAll(IfStmt.class)) {
                if (ifStmt.getThenStmt() instanceof BlockStmt block
                        && block.getStatements().isEmpty()) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(ifStmt),
                            getName(),
                            Severity.WARNING,
                            "Empty if-block detected.",
                            "Add the missing logic or remove the condition."
                    ));
                }
            }
        });

        return warnings;
    }
}
