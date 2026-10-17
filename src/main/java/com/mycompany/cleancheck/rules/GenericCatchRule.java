package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.type.Type;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** catch (Exception) / catch (Throwable) that handles everything the same way. */
public class GenericCatchRule implements Rule {

    private static final Set<String> TOO_BROAD = Set.of(
            "Exception", "Throwable", "java.lang.Exception", "java.lang.Throwable");

    @Override
    public String getName() {
        return "Generic Catch";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            for (CatchClause clause : cu.findAll(CatchClause.class)) {
                Type type = clause.getParameter().getType();
                List<Type> caught = type.isUnionType()
                        ? new ArrayList<>(type.asUnionType().getElements())
                        : List.of(type);
                boolean broad = caught.stream().anyMatch(t -> TOO_BROAD.contains(t.asString()));
                boolean rethrows = !clause.getBody().findAll(ThrowStmt.class).isEmpty();
                if (broad && !rethrows) {
                    warnings.add(new Warning(
                            fileName,
                            JavaAst.line(clause),
                            getName(),
                            Severity.WARNING,
                            "Catching '" + type.asString() + "' hides unrelated errors.",
                            "Catch the specific exceptions the code can throw."
                    ));
                }
            }
        });

        return warnings;
    }
}
