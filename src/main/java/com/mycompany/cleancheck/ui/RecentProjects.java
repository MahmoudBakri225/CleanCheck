package com.mycompany.cleancheck.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The projects the user really opened, newest first, kept in a small text file so they survive restarts.
 * Folders that no longer exist are skipped. All I/O is best effort: a read-only home folder must never
 * break the application.
 */
public final class RecentProjects {

    static final int MAX = 6;

    private final Path file;

    public RecentProjects(Path file) {
        this.file = file;
    }

    public static RecentProjects standard() {
        return new RecentProjects(Path.of(System.getProperty("user.home", "."), ".cleancheck", "recent-projects.txt"));
    }

    /** Existing project folders, newest first. */
    public List<Path> list() {
        List<Path> result = new ArrayList<>();
        for (Path path : readAll()) {
            if (Files.isDirectory(path)) {
                result.add(path);
            }
        }
        return result;
    }

    /** Moves the project to the front (no duplicates) and keeps at most {@link #MAX} entries. */
    public void add(Path project) {
        if (project == null) {
            return;
        }
        Path normalized = project.toAbsolutePath().normalize();
        List<Path> all = new ArrayList<>(readAll());
        all.removeIf(p -> p.equals(normalized));
        all.add(0, normalized);
        while (all.size() > MAX) {
            all.remove(all.size() - 1);
        }
        writeAll(all);
    }

    public void remove(Path project) {
        if (project == null) {
            return;
        }
        Path normalized = project.toAbsolutePath().normalize();
        List<Path> all = new ArrayList<>(readAll());
        if (all.removeIf(p -> p.equals(normalized))) {
            writeAll(all);
        }
    }

    private List<Path> readAll() {
        List<Path> paths = new ArrayList<>();
        try {
            if (!Files.isRegularFile(file)) {
                return paths;
            }
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    paths.add(Path.of(trimmed).toAbsolutePath().normalize());
                }
            }
        } catch (IOException | RuntimeException ex) {
            // Unreadable or malformed file: behave as if there were no history.
            return new ArrayList<>();
        }
        return paths;
    }

    private void writeAll(List<Path> paths) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> lines = new ArrayList<>();
            for (Path path : paths) {
                lines.add(path.toString());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException ex) {
            System.err.println("Could not save recent projects: " + ex.getMessage());
        }
    }
}
