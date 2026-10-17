package com.mycompany.cleancheck;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies automatic or assisted fixes for CleanCheck issues.
 *
 * Automatic (code change) when safe:
 * - Encapsulation, Empty If, Empty Method, Data Type, simple Naming
 *
 * Assisted (TODO comment) when a full rewrite is unsafe:
 * - Too Many Parameters, Infinite Loop, unknown rules
 */
public class C_FixEngine {

    // =========================================================
    // APPLY FIX FOR ONE SELECTED ISSUE (rule only)
    // =========================================================

    public List<C_FixResult> applyFix(
            Path javaFile,
            String ruleName
    ) throws IOException {

        return applyFix(javaFile, ruleName, -1);
    }

    // =========================================================
    // APPLY FIX FOR ONE SELECTED ISSUE (rule + line)
    // =========================================================

    public List<C_FixResult> applyFix(
            Path javaFile,
            String ruleName,
            int lineNumber
    ) throws IOException {

        String source = Files.readString(javaFile);
        String fixedSource = source;
        List<C_FixResult> results = new ArrayList<>();

        if (ruleName == null || ruleName.isBlank()) {
            return results;
        }

        String rule = ruleName.trim();

        if (equalsRule(rule, "Encapsulation")) {
            fixedSource = fixEncapsulation(source, lineNumber);
        } else if (equalsRule(rule, "Empty If")) {
            fixedSource = fixEmptyIf(source, lineNumber);
        } else if (equalsRule(rule, "Empty Method")) {
            fixedSource = fixEmptyMethod(source, lineNumber);
        } else if (equalsRule(rule, "Data Type")) {
            fixedSource = fixDataType(source, lineNumber);
        } else if (equalsRule(rule, "Naming Convention")) {
            fixedSource = fixNamingConvention(source, lineNumber);
        } else if (equalsRule(rule, "Too Many Parameters")) {
            fixedSource = fixTooManyParameters(source, lineNumber);
        } else if (equalsRule(rule, "Infinite Loop")) {
            fixedSource = fixInfiniteLoop(source, lineNumber);
        } else {
            fixedSource = insertAssistedComment(
                    source,
                    lineNumber,
                    "TODO [CleanCheck]: Review this issue manually (" + rule + ")"
            );
        }

        boolean changed = !source.equals(fixedSource);

        if (changed) {
            Files.writeString(javaFile, fixedSource);
        }

        results.add(
                new C_FixResult(
                        javaFile.getFileName().toString(),
                        ruleName,
                        changed
                )
        );

        return results;
    }

    // =========================================================
    // APPLY ALL AVAILABLE FIXES TO ONE FILE
    // =========================================================

    public List<C_FixResult> applyFixes(Path javaFile) throws IOException {

        String source = Files.readString(javaFile);
        String fixedSource = source;
        List<C_FixResult> results = new ArrayList<>();

        String afterEnc = fixEncapsulation(fixedSource, -1);
        if (!afterEnc.equals(fixedSource)) {
            fixedSource = afterEnc;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Encapsulation",
                    true
            ));
        }

        String afterIf = fixEmptyIf(fixedSource, -1);
        if (!afterIf.equals(fixedSource)) {
            fixedSource = afterIf;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Empty If",
                    true
            ));
        }

        String afterMethod = fixEmptyMethod(fixedSource, -1);
        if (!afterMethod.equals(fixedSource)) {
            fixedSource = afterMethod;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Empty Method",
                    true
            ));
        }

        String afterData = fixDataType(fixedSource, -1);
        if (!afterData.equals(fixedSource)) {
            fixedSource = afterData;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Data Type",
                    true
            ));
        }

        String afterNaming = fixNamingConvention(fixedSource, -1);
        if (!afterNaming.equals(fixedSource)) {
            fixedSource = afterNaming;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Naming Convention",
                    true
            ));
        }

        String afterParams = fixTooManyParameters(fixedSource, -1);
        if (!afterParams.equals(fixedSource)) {
            fixedSource = afterParams;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Too Many Parameters",
                    true
            ));
        }

        String afterLoop = fixInfiniteLoop(fixedSource, -1);
        if (!afterLoop.equals(fixedSource)) {
            fixedSource = afterLoop;
            results.add(new C_FixResult(
                    javaFile.getFileName().toString(),
                    "Infinite Loop",
                    true
            ));
        }

        if (!source.equals(fixedSource)) {
            Files.writeString(javaFile, fixedSource);
        }

        return results;
    }

    // =========================================================
    // APPLY ALL AVAILABLE FIXES TO ALL FILES
    // =========================================================

    public List<C_FixResult> applyFixes(List<Path> javaFiles) throws IOException {

        List<C_FixResult> allResults = new ArrayList<>();

        for (Path javaFile : javaFiles) {
            allResults.addAll(applyFixes(javaFile));
        }

        return allResults;
    }

    // =========================================================
    // RULE FIXERS
    // =========================================================

    private String fixEncapsulation(String source, int lineNumber) {

        if (lineNumber > 0) {
            return replaceOnLine(
                    source,
                    lineNumber,
                    line -> line.replaceFirst(
                            "^(\\s*)(public|protected)\\s+",
                            "$1private "
                    )
            );
        }

        return source.replaceFirst(
                "(?m)^(\\s*)(public|protected)\\s+([\\w<>\\[\\]]+)\\s+(\\w+)\\s*;",
                "$1private $3 $4;"
        );
    }

    private String fixEmptyIf(String source, int lineNumber) {

        Pattern emptyIf = Pattern.compile(
                "if\\s*\\((.*?)\\)\\s*\\{\\s*\\}",
                Pattern.DOTALL
        );

        if (lineNumber > 0) {
            return replaceInWindow(
                    source,
                    lineNumber,
                    2,
                    6,
                    emptyIf,
                    matcher -> "if (" + matcher.group(1) + ") {\n"
                            + "            // TODO: Review empty if block\n"
                            + "        }"
            );
        }

        Matcher matcher = emptyIf.matcher(source);
        if (matcher.find()) {
            String replacement =
                    "if (" + matcher.group(1) + ") {\n"
                            + "            // TODO: Review empty if block\n"
                            + "        }";
            return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        return source;
    }

    private String fixEmptyMethod(String source, int lineNumber) {

        Pattern emptyMethod = Pattern.compile(
                "(public|private|protected)\\s+([\\w<>\\[\\]]+)\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{\\s*\\}",
                Pattern.DOTALL
        );

        if (lineNumber > 0) {
            return replaceInWindow(
                    source,
                    lineNumber,
                    0,
                    8,
                    emptyMethod,
                    matcher -> matcher.group(1) + " " + matcher.group(2) + " "
                            + matcher.group(3) + "(" + matcher.group(4) + ") {\n"
                            + "        // TODO: Review empty method\n"
                            + "    }"
            );
        }

        Matcher matcher = emptyMethod.matcher(source);
        if (matcher.find()) {
            String replacement =
                    matcher.group(1) + " " + matcher.group(2) + " "
                            + matcher.group(3) + "(" + matcher.group(4) + ") {\n"
                            + "        // TODO: Review empty method\n"
                            + "    }";
            return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
        }
        return source;
    }

    private String fixDataType(String source, int lineNumber) {

        if (lineNumber > 0) {
            return replaceOnLine(source, lineNumber, this::normalizeDataTypesOnLine);
        }

        String[] lines = source.split("\\R", -1);
        StringBuilder sb = new StringBuilder();
        String nl = detectNewline(source);

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append(nl);
            }
            sb.append(normalizeDataTypesOnLine(lines[i]));
        }
        return sb.toString();
    }

    private String normalizeDataTypesOnLine(String line) {

        String result = line;

        result = result.replaceAll("\\bInt\\b", "int");
        result = result.replaceAll("\\bChar\\b", "char");
        result = result.replaceAll("\\bstring\\b", "String");

        result = result.replaceAll("\\bBoolean\\b", "boolean");
        result = result.replaceAll("\\bDouble\\b", "double");
        result = result.replaceAll("\\bFloat\\b", "float");
        result = result.replaceAll("\\bLong\\b", "long");
        result = result.replaceAll("\\bShort\\b", "short");
        result = result.replaceAll("\\bByte\\b", "byte");

        result = result.replaceAll(
                "(?m)^(\\s*(?:public|private|protected|static|final)\\s+)*float\\s+",
                "$1double "
        );
        result = result.replaceAll("(?m)^(\\s*)float\\s+", "$1double ");

        return result;
    }

    private String fixNamingConvention(String source, int lineNumber) {

        if (lineNumber > 0) {
            return replaceOnLine(source, lineNumber, line -> {
                String updated = line;

                updated = updated.replaceAll(
                        "(?<=\\s)_([a-zA-Z][a-zA-Z0-9_]*)",
                        "$1"
                );

                updated = convertSnakeCaseIdentifiers(updated);

                Matcher classMatcher = Pattern.compile(
                        "\\bclass\\s+([a-z][A-Za-z0-9_]*)"
                ).matcher(updated);

                if (classMatcher.find()) {
                    String name = classMatcher.group(1);
                    String fixed =
                            Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    updated = updated.replaceFirst(
                            "\\bclass\\s+" + Pattern.quote(name),
                            "class " + fixed
                    );
                }

                return updated;
            });
        }

        return source.replaceAll("(?<=\\s)_([a-zA-Z][a-zA-Z0-9_]*)", "$1");
    }

    private String convertSnakeCaseIdentifiers(String line) {

        Matcher matcher = Pattern.compile(
                "\\b([a-z][a-z0-9]*(_[a-z0-9]+)+)\\b"
        ).matcher(line);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String snake = matcher.group(1);
            String camel = snakeToCamel(snake);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(camel));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String snakeToCamel(String snake) {

        String[] parts = snake.split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }

    private String fixTooManyParameters(String source, int lineNumber) {

        String comment =
                "// TODO [CleanCheck]: Too many parameters — consider a parameter object or redesign";

        if (lineNumber > 0) {
            return insertCommentAfterLine(source, lineNumber, comment);
        }

        Pattern method = Pattern.compile(
                "(?m)^([ \\t]*(?:public|private|protected|static|final|synchronized)\\s+)+"
                        + "[\\w<>\\[\\],\\s]+\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*\\{"
        );

        Matcher matcher = method.matcher(source);
        StringBuffer sb = new StringBuffer();
        boolean any = false;

        while (matcher.find()) {
            String params = matcher.group(3) == null ? "" : matcher.group(3).trim();
            int count = countParams(params);
            if (count > 4 && !matcher.group().contains("TODO [CleanCheck]")) {
                any = true;
                matcher.appendReplacement(
                        sb,
                        Matcher.quoteReplacement(matcher.group() + "\n        " + comment)
                );
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return any ? sb.toString() : source;
    }

    private String fixInfiniteLoop(String source, int lineNumber) {

        String comment =
                "// TODO [CleanCheck]: Possible infinite loop — add a clear exit condition";

        if (lineNumber > 0) {
            return insertCommentAfterLine(source, lineNumber, comment);
        }

        Pattern loop = Pattern.compile(
                "(?m)(while\\s*\\(\\s*true\\s*\\)\\s*\\{|for\\s*\\(\\s*;\\s*;\\s*\\)\\s*\\{)"
        );

        Matcher matcher = loop.matcher(source);
        if (matcher.find()) {
            return matcher.replaceFirst(
                    Matcher.quoteReplacement(matcher.group() + "\n            " + comment)
            );
        }
        return source;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private boolean equalsRule(String actual, String expected) {
        return actual.equalsIgnoreCase(expected);
    }

    private int countParams(String params) {
        if (params == null || params.isBlank()) {
            return 0;
        }
        return params.split(",").length;
    }

    private String detectNewline(String source) {
        if (source.contains("\r\n")) {
            return "\r\n";
        }
        return "\n";
    }

    private String replaceOnLine(
            String source,
            int lineNumber,
            LineTransformer transformer
    ) {
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index < 0 || index >= lines.length) {
            return source;
        }

        String updated = transformer.transform(lines[index]);
        if (updated.equals(lines[index])) {
            return source;
        }

        lines[index] = updated;
        return String.join(detectNewline(source), lines);
    }

    private String insertCommentAfterLine(
            String source,
            int lineNumber,
            String comment
    ) {
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index < 0 || index >= lines.length) {
            return source;
        }

        if (index + 1 < lines.length && lines[index + 1].contains("TODO [CleanCheck]")) {
            return source;
        }
        if (lines[index].contains("TODO [CleanCheck]")) {
            return source;
        }

        String indent = leadingWhitespace(lines[index]) + "    ";
        List<String> list = new ArrayList<>(List.of(lines));
        list.add(index + 1, indent + comment);
        return String.join(detectNewline(source), list);
    }

    private String insertAssistedComment(
            String source,
            int lineNumber,
            String comment
    ) {
        if (lineNumber <= 0) {
            return source;
        }
        return insertCommentAfterLine(source, lineNumber, "// " + comment);
    }

    private String leadingWhitespace(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(0, i);
    }

    private String replaceInWindow(
            String source,
            int lineNumber,
            int before,
            int after,
            Pattern pattern,
            java.util.function.Function<Matcher, String> replacementFn
    ) {
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index < 0 || index >= lines.length) {
            return source;
        }

        int from = Math.max(0, index - before);
        int to = Math.min(lines.length - 1, index + after);
        String nl = detectNewline(source);

        StringBuilder window = new StringBuilder();
        for (int i = from; i <= to; i++) {
            if (i > from) {
                window.append(nl);
            }
            window.append(lines[i]);
        }

        Matcher matcher = pattern.matcher(window.toString());
        if (!matcher.find()) {
            return source;
        }

        String replacement = replacementFn.apply(matcher);
        String newWindow = matcher.replaceFirst(Matcher.quoteReplacement(replacement));

        StringBuilder rebuilt = new StringBuilder();
        for (int i = 0; i < from; i++) {
            if (i > 0) {
                rebuilt.append(nl);
            }
            rebuilt.append(lines[i]);
        }
        if (from > 0) {
            rebuilt.append(nl);
        }
        rebuilt.append(newWindow);
        for (int i = to + 1; i < lines.length; i++) {
            rebuilt.append(nl).append(lines[i]);
        }
        return rebuilt.toString();
    }

    @FunctionalInterface
    private interface LineTransformer {
        String transform(String line);
    }
}