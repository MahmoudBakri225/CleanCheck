package com.mycompany.cleancheck.scanner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JavaFileScanner {

    /** Tooling folders that are skipped wherever they appear. */
    private static final Set<String> ALWAYS_EXCLUDED = Set.of(".cleancheck-backups", ".git");

    /** Build output folders: skipped, except inside a source tree (a package may be called "build" or "out"). */
    private static final Set<String> BUILD_OUTPUT = Set.of("target", "build", "out");

    public List<Path> scan(Path folder) throws IOException {
        if (Files.isSymbolicLink(folder)) {
            throw new IOException("Project root must not be a symbolic link: " + folder.toAbsolutePath().normalize());
        }
        if (!Files.exists(folder)) {
            throw new IOException("Folder does not exist: " + folder);
        }

        List<Path> javaFiles = new ArrayList<>();
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                return !directory.equals(folder) && isExcluded(folder, directory)
                        ? FileVisitResult.SKIP_SUBTREE
                        : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (!Files.isSymbolicLink(file) && attributes.isRegularFile()
                        && !isExcluded(folder, file) && file.toString().endsWith(".java")) {
                    javaFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        javaFiles.sort(Comparator.naturalOrder()); // Keep reports deterministic regardless of filesystem order.
        return List.copyOf(javaFiles);
    }

    private boolean isExcluded(Path folder, Path file) {
        Path root = folder.toAbsolutePath().normalize();
        Path relative = root.relativize(file.toAbsolutePath().normalize());
        boolean insideSources = false;
        for (Path part : relative) {
            String name = part.toString().toLowerCase(Locale.ROOT);
            if (ALWAYS_EXCLUDED.contains(name)) {
                return true;
            }
            if (name.equals("src")) {
                insideSources = true;
            } else if (!insideSources && BUILD_OUTPUT.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads a source file as UTF-8. A file in another encoding (for example a Windows code page)
     * is still analyzed, decoded as ISO-8859-1, instead of aborting the whole scan.
     */
    public String readFile(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ex) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }
}
