package com.mycompany.cleancheck.analysis;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Problem;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Thin wrapper around JavaParser. Retries after fixing common type typos. */
public final class JavaAst {

    private static final Pattern COMMON_TYPE_TYPO = Pattern.compile("\\b(string|Int|Char)\\b");
    /** One source snapshot per analysis thread; removed at the end of each file analysis. */
    private static final ThreadLocal<ParseCache> ACTIVE_SOURCE = new ThreadLocal<>();

    private JavaAst() {
    }

    /** Enables reuse when a rule engine runs many AST rules over the exact same source snapshot. */
    public static void beginFile(String source) {
        ACTIVE_SOURCE.set(new ParseCache(source));
    }

    /** Releases the cached AST and source text after the file's rules have completed. */
    public static void endFile() {
        ACTIVE_SOURCE.remove();
    }

    private static JavaParser newParser() {
        // Keep parser state local to each call: analysis may run concurrently in the GUI and web UI.
        // Match the project's JDK 26 target so current language constructs are not reported as syntax errors.
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_26);
        return new JavaParser(configuration);
    }

    public static Optional<CompilationUnit> parse(String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        ParseCache cache = ACTIVE_SOURCE.get();
        if (cache != null && cache.source == source) {
            return cache.parsed();
        }
        return parseUncached(source);
    }

    private static Optional<CompilationUnit> parseUncached(String source) {
        Optional<CompilationUnit> parsed = parseStrict(source);
        if (parsed.isPresent()) {
            return parsed;
        }
        return parseStrict(normalizeCommonTypos(source));
    }

    private static Optional<CompilationUnit> parseStrict(String source) {
        try {
            ParseResult<CompilationUnit> result = newParser().parse(source);
            return result.isSuccessful() ? result.getResult() : Optional.empty();
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Explains why a file cannot be parsed, or returns empty when it parses fine (or is blank).
     * Rules stay silent on a file that does not parse, so the engine reports this instead of
     * letting a broken file look clean.
     */
    public static Optional<String> parseError(String source) {
        ParseCache cache = ACTIVE_SOURCE.get();
        if (cache != null && cache.source == source) {
            return cache.parseError();
        }
        return parseErrorUncached(source);
    }

    private static Optional<String> parseErrorUncached(String source) {
        if (source == null || source.isBlank() || parse(source).isPresent()) {
            return Optional.empty();
        }
        try {
            ParseResult<CompilationUnit> result = newParser().parse(source);
            if (!result.getProblems().isEmpty()) {
                Problem problem = result.getProblems().get(0);
                int line = problem.getLocation()
                        .flatMap(l -> l.getBegin().getRange())
                        .map(r -> r.begin.line)
                        .orElse(1);
                return Optional.of(line + "|" + firstLine(problem.getMessage()));
            }
        } catch (RuntimeException ignored) {
            // fall through to the generic message
        }
        return Optional.of("1|The file could not be parsed.");
    }

    private static final class ParseCache {
        private final String source;
        private boolean parseComputed;
        private Optional<CompilationUnit> parsed;
        private boolean errorComputed;
        private Optional<String> error;

        private ParseCache(String source) {
            this.source = source;
        }

        private Optional<CompilationUnit> parsed() {
            if (!parseComputed) {
                parsed = parseUncached(source);
                parseComputed = true;
            }
            return parsed;
        }

        private Optional<String> parseError() {
            if (!errorComputed) {
                error = parseErrorUncached(source);
                errorComputed = true;
            }
            return error;
        }
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "Syntax error.";
        }
        int newline = message.indexOf('\n');
        return (newline < 0 ? message : message.substring(0, newline)).trim();
    }

    public static int line(com.github.javaparser.ast.Node node) {
        return node.getBegin().map(p -> p.line).orElse(1);
    }

    private static String normalizeCommonTypos(String source) {
        String code = JavaSourceMask.maskNonCode(source);
        Matcher matcher = COMMON_TYPE_TYPO.matcher(code);
        StringBuilder normalized = new StringBuilder(source);
        while (matcher.find()) {
            String replacement = switch (matcher.group()) {
                case "string" -> "String";
                case "Int" -> "int";
                case "Char" -> "char";
                default -> matcher.group();
            };
            normalized.replace(matcher.start(), matcher.end(), replacement);
        }
        return normalized.toString();
    }
}
