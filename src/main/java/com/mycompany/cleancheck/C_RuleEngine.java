package com.mycompany.cleancheck;

import java.util.ArrayList;
import java.util.List;

/**
 * RuleEngine is responsible for managing and executing all CleanCheck rules.
 * It registers the available rules and applies each rule to the provided
 * Java source code, collecting all detected warnings.
 */
public class C_RuleEngine {

    private final List<L_Rule> rules;

    /**
     * Creates the RuleEngine and registers all rules
     * that will be used to check the source code.
     */
    public C_RuleEngine() {
        rules = new ArrayList<>();
        rules.add(new C_EncapsulationRule());
        rules.add(new C_EmptyMethodRule());
        rules.add(new C_TooManyParametersRule());
        rules.add(new C_NameConventionRule());
        rules.add(new C_DataTypeRule());
        rules.add(new C_InfiniteLoopRule());
        rules.add(new C_EmptyIfRule());
    }

    /**
     * Runs every registered rule on the given Java source code.
     * Each rule checks for a specific problem and returns its warnings.
     * All warnings are then combined and returned as one list.
     *
     * @param fileName the name of the source file being analyzed
     * @param source   the full source code content as a string
     * @return a combined list of all warnings found by the registered rules
     */
    public List<C_Warning> analyze(String fileName, String source) {
        List<C_Warning> warnings = new ArrayList<>();
        for (L_Rule rule : rules) {
            warnings.addAll(rule.analyze(fileName, source));
        }
        return warnings;
    }
}
