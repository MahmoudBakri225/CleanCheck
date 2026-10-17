package com.mycompany.cleancheck.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QualityGateTest {

    @Test
    void passesWhenScoreMeetsThresholdAndThereAreNoCriticalFindings() {
        QualityGate gate = new QualityGate(80, 80, 0);

        assertTrue(gate.isPassed());
        assertEquals("PASSED", gate.getStatus());
        assertEquals("Quality requirements satisfied.", gate.getReason());
    }

    @Test
    void failsForCriticalFindingsOrAScoreDrop() {
        QualityGate critical = new QualityGate(50, 100, 1);
        QualityGate scoreDrop = new QualityGate(80, 79, 0);

        assertFalse(critical.isPassed());
        assertEquals("Critical issues found (1).", critical.getReason());
        assertFalse(scoreDrop.isPassed());
        assertEquals("Quality score 79 is below required minimum 80.", scoreDrop.getReason());
    }

    @Test
    void rejectsScoresOutsideTheDocumentedRangeAndNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new QualityGate(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new QualityGate(0, 101, 0));
        assertThrows(IllegalArgumentException.class, () -> new QualityGate(0, 0, -1));
    }
}
