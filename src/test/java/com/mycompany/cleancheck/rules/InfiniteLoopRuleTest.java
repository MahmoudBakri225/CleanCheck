package com.mycompany.cleancheck.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mycompany.cleancheck.analysis.Severity;
import org.junit.jupiter.api.Test;

class InfiniteLoopRuleTest {

    private final InfiniteLoopRule rule = new InfiniteLoopRule();

    @Test
    void breakInNestedLoopDoesNotMarkOuterLoopAsHavingAnExit() {
        String source = "class Example { void run() { while (true) { while (true) { break; } } } }";

        var findings = rule.analyze("Example.java", source);

        assertEquals(1, findings.stream().filter(warning -> warning.getSeverity() == Severity.CRITICAL).count());
        assertEquals(1, findings.stream().filter(warning -> warning.getSeverity() == Severity.INFO).count());
    }

    @Test
    void breakInSwitchDoesNotMarkOuterLoopAsHavingAnExit() {
        String source = "class Example { void run() { while (true) { switch (1) { case 1: break; } } } }";

        var findings = rule.analyze("Example.java", source);

        assertEquals(1, findings.size());
        assertEquals(Severity.CRITICAL, findings.get(0).getSeverity());
    }

    @Test
    void returnInsideNestedLambdaDoesNotMarkOuterLoopAsHavingAnExit() {
        String source = "class Example { void run() { while (true) { Runnable task = () -> { return; }; } } }";

        var findings = rule.analyze("Example.java", source);

        assertEquals(1, findings.size());
        assertEquals(Severity.CRITICAL, findings.get(0).getSeverity());
    }

    @Test
    void detectsUnboundedDoWhileLoop() {
        String source = "class Example { void run() { do { work(); } while (true); } void work() {} }";

        var findings = rule.analyze("Example.java", source);

        assertEquals(1, findings.size());
        assertEquals(Severity.CRITICAL, findings.get(0).getSeverity());
    }
}
