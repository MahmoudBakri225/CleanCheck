package com.mycompany.cleancheck.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scans a project folder for Java source files and reads their content.
 * Used by the CleanCheck application to discover all .java files
 * and load them for analysis.
 */
public class C_JavaFileScanner {

    /**
     * Recursively scans the given folder and returns a list of all .java files.
     *
     * @param folder the root folder of the project to scan
     * @return a list of paths to all Java source files found
     * @throws IOException if the folder does not exist or cannot be accessed
     */
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

    /**
     * Reads the entire content of a Java source file as a single string.
     *
     * @param file the path of the file to read
     * @return the full source code of the file
     * @throws IOException if the file cannot be read
     */
    public String readFile(Path file) throws IOException {
        return Files.readString(file);
    }
}