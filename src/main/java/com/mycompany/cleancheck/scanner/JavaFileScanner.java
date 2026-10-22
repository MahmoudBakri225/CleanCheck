package com.mycompany.cleancheck.scanner;

import com.mycompany.cleancheck.analysis.SourceTextIO;

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
    private static final Set<String> ALWAYS_EXCLUDED = Set.of(".cleancheck-backups", ".git", ".idea", ".svn", "node_modules");

    /** Build output folders: skipped, except inside a source tree (a package may be called "build" or "out"). */
    private static final Set<String> BUILD_OUTPUT = Set.of("target", "build", "out");

    /** Hard cap on directory depth relative to the project root (protects huge trees / monorepos). */
    private static final int MAX_RELATIVE_DEPTH = 40;

    /** Hard cap on discovered .java files per scan (avoids unbounded memory use). */
    private static final int MAX_JAVA_FILES = 25_000;

    public List<Path> scan(Path folder) throws IOException {
        if (Files.isSymbolicLink(folder)) {
            throw new IOException("Project root must not be a symbolic link: " + folder.toAbsolutePath().normalize());
        }
        if (!Files.exists(folder)) {
            throw new IOException("Folder does not exist: " + folder);
        }

        Path root = folder.toAbsolutePath().normalize();
        List<Path> javaFiles = new ArrayList<>();
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                Path absolute = directory.toAbsolutePath().normalize();
                if (!absolute.equals(root)) {
                    int depth = root.relativize(absolute).getNameCount();
                    if (depth > MAX_RELATIVE_DEPTH) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (isExcluded(folder, directory)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (javaFiles.size() >= MAX_JAVA_FILES) {
                    return FileVisitResult.TERMINATE;
                }
                if (!Files.isSymbolicLink(file) && attributes.isRegularFile()
                        && !isExcluded(folder, file) && file.toString().endsWith(".java")) {
                    javaFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        if (javaFiles.size() >= MAX_JAVA_FILES) {
            throw new IOException("Project exceeds the maximum of " + MAX_JAVA_FILES
                    + " Java source files. Narrow the folder or use exclude globs in cleancheck.properties.");
        }
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
        return SourceTextIO.read(file).text();
    }
}
