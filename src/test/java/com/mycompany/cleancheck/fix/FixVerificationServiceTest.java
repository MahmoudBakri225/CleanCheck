package com.mycompany.cleancheck.fix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FixVerificationServiceTest {

    @Test
    void refusesToBackUpAFileOutsideTheSelectedProject(@TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path externalFile = Files.writeString(temporaryDirectory.resolve("Outside.java"), "class Outside {}\n");

        assertThrows(IOException.class, () -> FixVerificationService.createBackup(project, externalFile));
        assertFalse(Files.exists(project.resolve(".cleancheck-backups")));
    }

    @Test
    void createsDistinctBackupLocationsForRapidConsecutiveFixes(@TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path source = Files.writeString(project.resolve("Example.java"), "class Example {}\n");

        Path first = FixVerificationService.createBackup(project, source);
        Path second = FixVerificationService.createBackup(project, source);

        assertNotEquals(first, second);
        assertEquals(-1, Files.mismatch(source, first));
    }

    @Test
    void discardingOneSnapshotKeepsOtherSnapshotsIntact(@TempDir Path temporaryDirectory) throws IOException {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Path source = Files.writeString(project.resolve("Example.java"), "class Example {}\n");

        Path first = FixVerificationService.createBackup(project, source);
        Path second = FixVerificationService.createBackup(project, source);
        FixVerificationService.discardBackup(project, first);

        assertFalse(Files.exists(first));
        assertEquals(-1, Files.mismatch(source, second));
    }
}
