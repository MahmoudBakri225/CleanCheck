package com.mycompany.cleancheck.quality;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** Counts source lines for density-aware quality scoring. */
public final class SourceSize {

    private SourceSize() {
    }

    public static int totalLines(List<Path> javaFiles) {
        int total = 0;
        if (javaFiles == null) {
            return 0;
        }
        for (Path file : javaFiles) {
            try (Stream<String> lines = Files.lines(file)) {
                total = (int) Math.min(Integer.MAX_VALUE, (long) total + lines.count());
            } catch (IOException ignored) {
                // skip unreadable file
            }
        }
        return total;
    }
}
