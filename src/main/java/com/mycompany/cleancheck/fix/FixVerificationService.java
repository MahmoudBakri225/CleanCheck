package com.mycompany.cleancheck.fix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Creates a local rollback point before a source file is modified and restores it on demand.
 * The service deliberately stays local: source code is never uploaded or sent to an external service.
 */
public final class FixVerificationService {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private FixVerificationService() {
    }

    public static Path createBackup(Path projectRoot, Path sourceFile)
            throws IOException {

        if (projectRoot == null || sourceFile == null) {
            throw new IllegalArgumentException("Project root and source file are required.");
        }

        Path lexicalRoot = projectRoot.toAbsolutePath().normalize();
        Path lexicalFile = sourceFile.toAbsolutePath().normalize();
        if (!lexicalFile.startsWith(lexicalRoot)) {
            throw new IOException("Source file must be inside the selected project folder: " + sourceFile);
        }
        for (Path current = lexicalFile; current != null && !current.equals(lexicalRoot); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Automatic fixes do not follow symbolic links: " + current);
            }
        }

        Path normalizedRoot = lexicalRoot;
        Path normalizedFile = lexicalFile;

        Path backupParent = normalizedRoot.resolve(".cleancheck-backups");
        Files.createDirectories(backupParent);
        Path backupRoot = Files.createTempDirectory(
                backupParent,
                LocalDateTime.now().format(FORMAT) + "-");

        Path relative = normalizedRoot.relativize(normalizedFile);

        Path backup = backupRoot.resolve(relative);
        Files.createDirectories(backup.getParent());
        Files.copy(normalizedFile, backup);

        return backup;
    }

    public static void restoreBackup(Path backupFile, Path sourceFile)
            throws IOException {

        if (backupFile == null || sourceFile == null) {
            throw new IllegalArgumentException("Backup and source file are required.");
        }

        if (!Files.isRegularFile(backupFile)) {
            throw new IOException("Rollback backup does not exist: " + backupFile);
        }

        Files.copy(
                backupFile,
                sourceFile,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
    }

    /** Deletes one unique fix snapshot after the change has been rejected or proved unnecessary. */
    public static void discardBackup(Path projectRoot, Path backupFile) throws IOException {
        if (projectRoot == null || backupFile == null) {
            throw new IllegalArgumentException("Project root and backup file are required.");
        }
        Path backupParent = projectRoot.toAbsolutePath().normalize().resolve(".cleancheck-backups");
        Path backup = backupFile.toAbsolutePath().normalize();
        if (!backup.startsWith(backupParent) || backup.getNameCount() <= backupParent.getNameCount()) {
            throw new IOException("Backup is outside this project's rollback folder: " + backupFile);
        }
        Path snapshotRoot = backupParent.resolve(backup.getName(backupParent.getNameCount()));
        if (Files.isSymbolicLink(snapshotRoot)) {
            throw new IOException("Refusing to remove a symbolic-link rollback folder: " + snapshotRoot);
        }
        if (!Files.exists(snapshotRoot)) {
            return;
        }
        List<Path> entries;
        try (Stream<Path> walk = Files.walk(snapshotRoot)) {
            entries = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path entry : entries) {
            Files.deleteIfExists(entry);
        }
    }
}
