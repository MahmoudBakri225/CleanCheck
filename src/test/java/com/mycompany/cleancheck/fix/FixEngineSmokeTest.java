package com.mycompany.cleancheck.fix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixEngineSmokeTest {

    @Test
    void stringComparisonIsAutoFixable() {
        assertTrue(FixEngine.supportsAutomaticFix("String Comparison", "compared with =="));
    }

    @Test
    void securityIsNotAutoFixable() {
        assertFalse(FixEngine.supportsAutomaticFix("Hardcoded Secret", "API key"));
        assertFalse(FixEngine.supportsAutomaticFix("XML External Entity", "XXE"));
    }
}

