package com.mycompany.cleancheck.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportFileWriterTest {

    @Test
    void createsParentAndReplacesAnExistingReportWithCompleteUtf8(@TempDir Path temporaryDirectory)
            throws IOException {
        Path output = temporaryDirectory.resolve("nested").resolve("report.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, "older report\n");

        ReportFileWriter.writeUtf8(output, "π\ncomplete\n");

        assertEquals("π\ncomplete\n", Files.readString(output));
    }

    @Test
    void preservesExistingDestinationWhenReplacementCannotComplete(@TempDir Path temporaryDirectory)
            throws IOException {
        Path destination = Files.createDirectory(temporaryDirectory.resolve("report.json"));
        Path marker = Files.writeString(destination.resolve("keep.txt"), "previous report data");

        assertThrows(IOException.class, () -> ReportFileWriter.writeUtf8(destination, "new data"));

        assertTrue(Files.isDirectory(destination));
        assertEquals("previous report data", Files.readString(marker));
        try (var siblings = Files.list(temporaryDirectory)) {
            assertEquals(1, siblings.count(), "temporary report files must be removed after failure");
        }
    }
}
