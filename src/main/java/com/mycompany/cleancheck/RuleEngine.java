package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

public class RuleEngine {

    private final List<Rule> rules;

    public RuleEngine() {
        rules = new ArrayList<>();
        rules.add(new EncapsulationRule());
        rules.add(new EmptyMethodRule());
        rules.add(new TooManyParametersRule());
        rules.add(new NameConventionRule());
        
        //rules.add(new DataTypeSyntaxRule());
        rules.add(new DataTypeRule());
     //rules.add(new VariableNameRule());
    rules.add(new InfiniteLoopRule());   // جديدة
        rules.add(new EmptyIfRule());        // جديدة
    }

    public List<Warning> analyze(String fileName, String source) {
        List<Warning> warnings = new ArrayList<>();
        for (Rule rule : rules) {
            warnings.addAll(rule.analyze(fileName, source));
        }
        return warnings;
    }
}
