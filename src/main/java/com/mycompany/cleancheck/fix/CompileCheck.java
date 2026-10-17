package com.mycompany.cleancheck.fix;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Compiles a project's own sources with javac (nothing is kept) and lists the compile errors.
 * Used to prove that an automatic fix did not break a class in another file or package.
 * Libraries are not on the classpath, so a project that imports them already has baseline errors;
 * callers therefore compare the error set before and after a change instead of expecting zero.
 */
public final class CompileCheck {

    private CompileCheck() {
    }

    /** True on a full JDK; a plain JRE has no compiler and the check is skipped. */
    public static boolean isAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    /**
     * @return distinct error messages ("File.java: message", no line numbers so a fix that shifts
     *         lines does not look like a new error), or empty when no compiler is available
     */
    public static Optional<Set<String>> errors(Path projectRoot, List<Path> javaFiles) throws IOException {
        return errorCounts(projectRoot, javaFiles).map(Map::keySet);
    }

    /** Counts identical diagnostics separately so a newly repeated compiler error is not hidden. */
    public static Optional<Map<String, Integer>> errorCounts(Path projectRoot, List<Path> javaFiles)
            throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null || javaFiles.isEmpty()) {
            return Optional.empty();
        }
        Path root = projectRoot.toAbsolutePath().normalize();
        Path output = Files.createTempDirectory("cleancheck-compile");
        try {
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
                Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(javaFiles);
                List<String> options = List.of("-proc:none", "-Xlint:none", "-Xmaxerrs", "1000", "-d", output.toString());
                compiler.getTask(null, fm, diagnostics, options, null, units).call();
            }
            Map<String, Integer> errors = new TreeMap<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() != Diagnostic.Kind.ERROR) {
                    continue;
                }
                String file = d.getSource() == null
                        ? "?"
                        : relative(root, d.getSource().toUri().getPath());
                String message = file + ": " + d.getMessage(null).lines().findFirst().orElse("");
                errors.merge(message, 1, Integer::sum);
            }
            return Optional.of(Map.copyOf(errors));
        } finally {
            try (Stream<Path> walk = Files.walk(output)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static String relative(Path root, String path) {
        try {
            return root.relativize(Path.of(path).toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (RuntimeException ex) {
            return path;
        }
    }
}
