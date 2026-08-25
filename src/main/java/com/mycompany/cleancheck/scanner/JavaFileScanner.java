package com.mycompany.cleancheck.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class JavaFileScanner {

    public List<Path> scan(Path folder) throws IOException {
        if (!Files.exists(folder)) {
            throw new IOException("Folder does not exist: " + folder);
        }

        try (Stream<Path> files = Files.walk(folder)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }
    }

    public String readFile(Path file) throws IOException {
        return Files.readString(file);
    }
}
