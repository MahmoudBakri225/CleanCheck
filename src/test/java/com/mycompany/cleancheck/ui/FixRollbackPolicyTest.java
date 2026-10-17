package com.mycompany.cleancheck.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FixRollbackPolicyTest {

    private static final Path PROJECT = Path.of("work", "project");
    private static final Path SOURCE = PROJECT.resolve("src/App.java");
    private static final Path BACKUP = Path.of("work", "backup", "source.java");

    @Test
    void allowsRollbackOnlyForTheProjectThatOwnsTheFix() {
        assertTrue(FixRollbackPolicy.canRollback(PROJECT, PROJECT, SOURCE, BACKUP, false));
        assertFalse(FixRollbackPolicy.canRollback(Path.of("work", "other"), PROJECT, SOURCE, BACKUP, false));
    }

    @Test
    void blocksRollbackWhileAnOperationIsRunningOrBackupDataIsMissing() {
        assertFalse(FixRollbackPolicy.canRollback(PROJECT, PROJECT, SOURCE, BACKUP, true));
        assertFalse(FixRollbackPolicy.canRollback(PROJECT, PROJECT, SOURCE, null, false));
    }
}
