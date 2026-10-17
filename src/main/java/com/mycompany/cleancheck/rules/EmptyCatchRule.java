package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.stmt.CatchClause;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class EmptyCatchRule implements Rule {

    @Override
    public String getName() {
        return "Empty Catch";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (CatchClause catchClause : cu.findAll(CatchClause.class)) {
                if (catchClause.getBody().getStatements().isEmpty()) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(catchClause),
                            getName(),
                            Severity.CRITICAL,
                            "Empty catch block swallows the exception.",
                            "Log the error, rethrow it, or handle it properly."
                    ));
                }
            }
        });

        return warnings;
    }
}
