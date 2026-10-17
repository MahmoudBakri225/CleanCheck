package com.mycompany.cleancheck.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes a complete UTF-8 report beside its destination, then replaces the destination in one move. */
final class ReportFileWriter {

    private ReportFileWriter() {
    }

    static void writeUtf8(Path destination, String content) throws IOException {
        if (destination == null || destination.getFileName() == null) {
            throw new IOException("A report file path is required.");
        }
        Path absolute = destination.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("The report destination has no parent folder: " + destination);
        }

        Files.createDirectories(parent);
        String prefix = "." + absolute.getFileName() + ".";
        Path temporary = Files.createTempFile(parent, prefix, ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absolute,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
