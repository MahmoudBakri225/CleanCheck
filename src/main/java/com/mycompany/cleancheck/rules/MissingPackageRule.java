package com.mycompany.cleancheck.rules;

import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.List;

public class MissingPackageRule implements Rule {

    @Override
    public String getName() {
        return "Missing Package";
    }

    @Override
    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();

        JavaAst.parse(source).ifPresent(cu -> {
            if (fileName.endsWith("module-info.java")) {
                return;
            }
            if (cu.getPackageDeclaration().isEmpty()) {
                warnings.add(new Warning(
                        fileName,
                        1,
                        getName(),
                        Severity.WARNING,
                        "File has no package declaration.",
                        "Add a package statement at the top of the file."
                ));
            }
        });

        return warnings;
    }
}
