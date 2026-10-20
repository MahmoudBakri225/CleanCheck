package com.mycompany.cleancheck.fix;

import com.mycompany.cleancheck.analysis.JavaSourceMask;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;

/**
 * Safe automatic fixes (current implementation is primarily line/regex based).
 *
 * <p><b>Design note:</b> Regex-based edits are intentionally limited to simple,
 * single-line patterns. All supported automatic fixes prefer JavaParser AST rewrites via AstFixSupport,
 * with the original regex implementation kept as a safe fallback. Until then, every applied fix
 * is re-analyzed and can be rolled back if new findings or compile errors appear.
 *
 * Supported:
 * - Data Type typos
 * - Encapsulation (field visibility)
 * - System Out (remove console call line)
 * - Naming Convention (field / parameter first letter)
 * - Missing Package (insert package line from path)
 * - Magic Number (extract simple int literal on a line to a named constant)
 * - Unused Import (remove the import line)
 * - String Comparison (x == "lit" becomes "lit".equals(x))
 * - Unused Variable (single-line local declaration; a call/new initializer is kept as a statement)
 * - Information Leak (a lone "e.printStackTrace();" becomes a java.util.logging call)
 */
public class FixEngine {

    private static final Pattern INVALID_BUILT_IN_TYPE = Pattern.compile("\\b(Int|Char|string)\\b");

    private static final Pattern FIELD_LINE = Pattern.compile(
            "^(\\s*)(public|protected)\\s+((?:static\\s+|final\\s+)*)([\\w.<>,\\[\\]\\s]+?)\\s+([A-Za-z_][\\w]*)\\s*(=.*)?;\\s*$"
    );

    private static final Pattern NAMED_FIELD_LINE = Pattern.compile(
            "^(\\s*)((?:public|protected|private)\\s+)?((?:static\\s+|final\\s+)*)([\\w.<>,\\[\\]\\s]+?)\\s+([A-Z][\\w]*)\\s*(=.*)?;\\s*$"
    );

    private static final Pattern SYSTEM_OUT_LINE = Pattern.compile(
            "^\\s*System\\.(out|err)\\.[A-Za-z]+\\s*\\(.*\\)\\s*;\\s*$"
    );

    private static final Pattern PRINT_STACK_TRACE_LINE = Pattern.compile(
            "^(\\s*)([A-Za-z_]\\w*)\\.printStackTrace\\(\\)\\s*;\\s*$"
    );

    private static final Pattern MAGIC_INT_ON_LINE = Pattern.compile(
            "(?<![\\w.])(\\d{2,})(?![\\w.])"
    );

    /** A type declaration that starts a line (comments and statements never match). */
    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?m)^[ \\t]*(?:(?:public|protected|private|abstract|final|static|sealed|non-sealed|strictfp)\\s+)*"
            + "(class|interface|enum|record|@interface)\\s+\\w+[^{;]*\\{"
    );

    /** The head of a braceless if/else/for/while, whose next statement is its body. */
    private static final Pattern CONTROL_HEAD = Pattern.compile(
            "^(?:\\}\\s*)?(?:else\\s+)?(?:if|for|while)\\b.*\\)$"
    );

    public static boolean supportsAutomaticFix(String ruleName, String message) {
        if (ruleName == null) {
            return false;
        }
        String rule = ruleName.trim();
        if (rule.equalsIgnoreCase("Data Type")) {
            return message == null
                    || message.contains("does not exist")
                    || message.contains("not a valid Java type")
                    || message.contains("string")
                    || message.contains("Int")
                    || message.contains("Char")
                    || message.toLowerCase(Locale.ROOT).contains("floating");
        }
        return rule.equalsIgnoreCase("Encapsulation")
                || rule.equalsIgnoreCase("System Out")
                || rule.equalsIgnoreCase("Naming Convention")
                || rule.equalsIgnoreCase("Missing Package")
                || rule.equalsIgnoreCase("Magic Number")
                || rule.equalsIgnoreCase("Unused Import")
                || rule.equalsIgnoreCase("String Comparison")
                || rule.equalsIgnoreCase("Unused Variable")
                || (rule.equalsIgnoreCase("Information Leak")
                        && (message == null || message.contains("printStackTrace")));
    }

    public List<FixResult> applyFix(Path javaFile, String ruleName, int lineNumber)
            throws IOException {
        SourceFileIO.Source source = SourceFileIO.read(javaFile);
        String before = source.text();
        String after = before;

        if (ruleName == null) {
            return List.of(new FixResult(javaFile.getFileName().toString(), null, false));
        }

        if (ruleName.equalsIgnoreCase("Data Type")) {
            after = AstFixSupport.fixBuiltinTypeAtLine(before, lineNumber)
                    .orElseGet(() -> {
                        String a = fixTypeLine(before, lineNumber);
                        return before.equals(a) ? fixAllTypeErrors(before) : a;
                    });
        } else if (ruleName.equalsIgnoreCase("Encapsulation")) {
            after = AstFixSupport.makeFieldPrivateAtLine(before, lineNumber)
                    .orElseGet(() -> fixEncapsulationLine(before, lineNumber, javaFile));
        } else if (ruleName.equalsIgnoreCase("System Out")) {
            after = AstFixSupport.removeSystemOutAtLine(before, lineNumber)
                    .orElseGet(() -> fixSystemOutLine(before, lineNumber));
        } else if (ruleName.equalsIgnoreCase("Naming Convention")) {
            after = AstFixSupport.renameFieldFirstLetterAtLine(before, lineNumber)
                    .orElseGet(() -> fixNamingLine(before, lineNumber, javaFile));
        } else if (ruleName.equalsIgnoreCase("Missing Package")) {
            after = AstFixSupport.ensurePackage(before, javaFile).orElseGet(() -> fixMissingPackage(before, javaFile));
        } else if (ruleName.equalsIgnoreCase("Magic Number")) {
            after = AstFixSupport.extractMagicIntAtLine(before, lineNumber)
                    .orElseGet(() -> fixMagicNumber(before, lineNumber));
        } else if (ruleName.equalsIgnoreCase("Unused Import")) {
            after = AstFixSupport.removeImportAtLine(before, lineNumber).orElseGet(() -> fixUnusedImport(before, lineNumber));
        } else if (ruleName.equalsIgnoreCase("String Comparison")) {
            after = AstFixSupport.fixStringEqualityAtLine(before, lineNumber).orElseGet(() -> fixStringComparison(before, lineNumber));
        } else if (ruleName.equalsIgnoreCase("Unused Variable")) {
            after = AstFixSupport.removeLocalVariableAtLine(before, lineNumber)
                    .orElseGet(() -> fixUnusedVariable(before, lineNumber));
        } else if (ruleName.equalsIgnoreCase("Information Leak")) {
            after = AstFixSupport.replacePrintStackTraceAtLine(before, lineNumber).orElseGet(() -> fixPrintStackTrace(before, lineNumber, javaFile));
        }

        if (!before.equals(after)) {
            SourceFileIO.write(javaFile, after, source.charset());
        }
        return List.of(new FixResult(
                javaFile.getFileName().toString(),
                ruleName,
                !before.equals(after)
        ));
    }

    public List<FixResult> applyFix(Path javaFile, String ruleName) throws IOException {
        return applyFix(javaFile, ruleName, -1);
    }

    public List<FixResult> applyFixes(Path javaFile) throws IOException {
        SourceFileIO.Source source = SourceFileIO.read(javaFile);
        String before = source.text();
        String after = fixAllTypeErrors(before);
        if (before.equals(after)) {
            return List.of();
        }
        SourceFileIO.write(javaFile, after, source.charset());
        return List.of(new FixResult(javaFile.getFileName().toString(), "Data Type", true));
    }

    public List<FixResult> applyFixes(List<Path> javaFiles) throws IOException {
        List<FixResult> results = new ArrayList<>();
        for (Path javaFile : javaFiles) {
            results.addAll(applyFixes(javaFile));
        }
        return results;
    }

    /**
     * "e.printStackTrace();" becomes a logger call. Fully qualified names, so no import is needed; the logger
     * is named after the file, which is the public top-level class in a normal project.
     */
    private String fixPrintStackTrace(String source, int lineNumber, Path javaFile) {
        if (lineNumber < 1) {
            return source;
        }
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index >= lines.length) {
            return source;
        }
        Matcher matcher = PRINT_STACK_TRACE_LINE.matcher(lines[index]);
        if (!matcher.matches()) {
            return source;
        }
        String fileName = javaFile.getFileName().toString();
        String className = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
        if (!SourceVersion.isIdentifier(className)) {
            return source;
        }
        lines[index] = matcher.group(1) + "java.util.logging.Logger.getLogger(" + className
                + ".class.getName()).log(java.util.logging.Level.SEVERE, \"Unexpected error\", "
                + matcher.group(2) + ");";
        return join(source, lines);
    }

    private String fixTypeLine(String source, int lineNumber) {
        if (lineNumber < 1) {
            return source;
        }
        return replaceInvalidBuiltInTypes(source, lineNumber);
    }

    private String fixEncapsulationLine(String source, int lineNumber, Path javaFile) {
        if (lineNumber < 1) {
            return source;
        }
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index >= lines.length) {
            return source;
        }
        Matcher matcher = FIELD_LINE.matcher(lines[index]);
        if (!matcher.matches()) {
            return source;
        }
        String indent = matcher.group(1);
        String modifiers = matcher.group(3) == null ? "" : matcher.group(3);
        String type = matcher.group(4).trim();
        String name = matcher.group(5);
        String rest = matcher.group(6) == null ? "" : matcher.group(6);
        if (modifiers.contains("static") && modifiers.contains("final")) {
            return source;
        }
        // Making the field private breaks any class in the same folder that reads it as obj.field.
        if (accessedViaDotInSiblings(javaFile, name)) {
            return source;
        }
        lines[index] = indent + "private " + modifiers + type + " " + name + rest + ";";
        return join(source, lines);
    }

    private static final Pattern IMPORT_LINE = Pattern.compile("^\\s*import\\s+(static\\s+)?[\\w.]+\\s*;\\s*$");

    private static final Pattern VALUE_EQ_LITERAL = Pattern.compile(
            "([A-Za-z_][\\w.]*)\\s*(==|!=)\\s*(\"(?:[^\"\\\\]|\\\\.)*\")");

    private static final Pattern LITERAL_EQ_VALUE = Pattern.compile(
            "(\"(?:[^\"\\\\]|\\\\.)*\")\\s*(==|!=)\\s*([A-Za-z_][\\w.]*)(?![\\w.(])");

    private String fixUnusedImport(String source, int lineNumber) {
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (lineNumber < 1 || index >= lines.length || !IMPORT_LINE.matcher(lines[index]).matches()) {
            return source;
        }
        lines[index] = "";
        return join(source, lines);
    }

    private String fixStringComparison(String source, int lineNumber) {
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (lineNumber < 1 || index >= lines.length) {
            return source;
        }
        String line = lines[index];
        // Only a line with exactly one comparison, so the rewrite cannot change another expression.
        int comparisons = line.split("==|!=", -1).length - 1;
        if (comparisons != 1) {
            return source;
        }
        Matcher valueFirst = VALUE_EQ_LITERAL.matcher(line);
        if (valueFirst.find()) {
            String call = valueFirst.group(3) + ".equals(" + valueFirst.group(1) + ")";
            lines[index] = valueFirst.replaceFirst(Matcher.quoteReplacement(
                    valueFirst.group(2).equals("!=") ? "!" + call : call));
            return join(source, lines);
        }
        Matcher literalFirst = LITERAL_EQ_VALUE.matcher(line);
        if (literalFirst.find()) {
            String call = literalFirst.group(1) + ".equals(" + literalFirst.group(3) + ")";
            lines[index] = literalFirst.replaceFirst(Matcher.quoteReplacement(
                    literalFirst.group(2).equals("!=") ? "!" + call : call));
            return join(source, lines);
        }
        return source;
    }

    private static final Pattern LOCAL_DECLARATION = Pattern.compile(
            "^(\\s*)(?:final\\s+)?([\\w.<>,\\[\\]?\\s]+?)\\s+([A-Za-z_]\\w*)\\s*(?:=\\s*(.+?))?\\s*;\\s*$"
    );

    private static final Pattern PURE_INITIALIZER = Pattern.compile(
            "-?\\d[\\w.]*|\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)+'|true|false|null|[A-Za-z_]\\w*"
    );

    private static final Pattern STATEMENT_EXPRESSION = Pattern.compile(
            "(new\\s+)?[\\w.]+(<[^()]*>)?\\(\\)(\\.\\w+(<[^()]*>)?\\(\\))*"
    );

    private static final java.util.Set<String> NOT_A_TYPE = java.util.Set.of(
            "return", "throw", "else", "case", "yield", "assert", "break", "continue", "new");

    /**
     * Removes an unused local on a single line. A literal or plain-name initializer is dropped with
     * the variable; a method call or "new" initializer is kept as a statement so its side effects
     * (and exceptions) still happen. Anything else, any other mention of the name in the file, and
     * any braceless if/else/loop body is left untouched.
     */
    private String fixUnusedVariable(String source, int lineNumber) {
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (lineNumber < 1 || index >= lines.length) {
            return source;
        }
        Matcher declaration = LOCAL_DECLARATION.matcher(lines[index]);
        if (!declaration.matches()) {
            return source;
        }
        String indent = declaration.group(1);
        String type = declaration.group(2).trim();
        String name = declaration.group(3);
        String initializer = declaration.group(4);

        String bareType = type;
        String previousType;
        do {
            previousType = bareType;
            bareType = bareType.replaceAll("<[^<>]*>", "");
        } while (!bareType.equals(previousType));
        if (bareType.contains(",") || NOT_A_TYPE.contains(type.split("\\s+")[0])) {
            return source;
        }
        // "x = 5;" elsewhere would stop compiling once the declaration is gone.
        Pattern mention = Pattern.compile("(?<![\\w$])" + Pattern.quote(name) + "(?![\\w$])");
        for (int i = 0; i < lines.length; i++) {
            if (i != index && mention.matcher(lines[i]).find()) {
                return source;
            }
        }
        String initializerCode = initializer == null ? "" : initializer.replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"", "\"\"");
        if (mention.matcher(initializerCode).find()) {
            return source;
        }
        // A braceless if/else/loop body: deleting the line would hand it to the next statement.
        for (int i = index - 1; i >= 0; i--) {
            String previous = lines[i].trim();
            if (previous.isEmpty()) {
                continue;
            }
            if (CONTROL_HEAD.matcher(previous).matches()
                    || previous.equals("else")
                    || previous.endsWith(" else")
                    || previous.equals("do")
                    || previous.endsWith("->")) {
                return source;
            }
            break;
        }
        if (initializer == null || PURE_INITIALIZER.matcher(initializer.trim()).matches()) {
            lines[index] = "";
            return join(source, lines);
        }
        String init = initializer.trim();
        if (isStatementExpression(init)) {
            lines[index] = indent + init + ";";
            return join(source, lines);
        }
        return source;
    }

    /** True for "call(...)", "a.b(...).c(...)" and "new T(...)", the only expressions legal as a statement here. */
    private static boolean isStatementExpression(String expression) {
        StringBuilder outside = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '(') {
                if (depth == 0) {
                    outside.append("()");
                }
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            } else if (depth == 0) {
                outside.append(c);
            }
        }
        return depth == 0 && !inString && STATEMENT_EXPRESSION.matcher(outside.toString().trim()).matches();
    }

    private String fixSystemOutLine(String source, int lineNumber) {
        if (lineNumber < 1) {
            return source;
        }
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index >= lines.length) {
            return source;
        }
        if (!SYSTEM_OUT_LINE.matcher(lines[index]).matches()) {
            return source;
        }
        // "if (x)" + println on its own line: deleting the println would make the NEXT statement the body.
        for (int i = index - 1; i >= 0; i--) {
            String previous = lines[i].trim();
            if (previous.isEmpty()) {
                continue;
            }
            if (CONTROL_HEAD.matcher(previous).matches()
                    || previous.equals("else")
                    || previous.endsWith(" else")
                    || previous.equals("do")
                    || previous.endsWith("->")) {
                return source;
            }
            break;
        }
        lines[index] = "";
        return join(source, lines);
    }

    /**
     * Lowercase the first letter of a field and every use of it in the same file.
     * Skipped (file untouched) whenever the rename could change meaning: the new name already
     * exists, the old name doubles as a type or method name, it is read through another object,
     * or a class in the same folder accesses it as obj.Name.
     */
    private String fixNamingLine(String source, int lineNumber, Path javaFile) {
        if (lineNumber < 1) {
            return source;
        }
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index >= lines.length) {
            return source;
        }
        String oldName;
        Matcher matcher = NAMED_FIELD_LINE.matcher(lines[index]);
        if (matcher.matches()) {
            oldName = matcher.group(5);
        } else {
            Matcher simple = Pattern.compile(
                    "^(\\s*)([\\w.<>,\\[\\]\\s]+?)\\s+([A-Z][\\w]*)\\s*(=.*)?;\\s*$"
            ).matcher(lines[index]);
            if (!simple.matches()) {
                return source;
            }
            oldName = simple.group(3);
        }
        if (oldName.equals(oldName.toUpperCase(Locale.ROOT)) && oldName.length() > 1) {
            return source; // constant-looking
        }
        String newName = Character.toLowerCase(oldName.charAt(0)) + oldName.substring(1);

        // The line must really be a field declaration: its "type" cannot be a statement keyword.
        String declaredType = matcher.matches() ? matcher.group(4).trim() : "";
        if (declaredType.equals("return") || declaredType.equals("package") || declaredType.equals("import")
                || declaredType.equals(oldName)) {
            return source;
        }

        String wholeOld = "(?<![\\w$])" + Pattern.quote(oldName) + "(?![\\w$])";
        String wholeNew = "(?<![\\w$])" + Pattern.quote(newName) + "(?![\\w$])";
        Pattern oldWord = Pattern.compile(wholeOld);
        Pattern newWord = Pattern.compile(wholeNew);
        Pattern typeOrMethodUse = Pattern.compile(
                "(?:\\b(?:new|class|interface|enum|record)\\s+" + Pattern.quote(oldName) + "(?![\\w$]))"
                + "|(?:" + wholeOld + "\\s*\\()");
        Pattern foreignQualifier = Pattern.compile("(?<!this)\\.\\s*" + wholeOld);

        for (String line : lines) {
            boolean[] code = codeMask(line, false);
            if (hasCodeMatch(newWord, line, code)
                    || hasCodeMatch(typeOrMethodUse, line, code)
                    || hasCodeMatch(foreignQualifier, line, code)) {
                return source;
            }
        }
        if (accessedViaDotInSiblings(javaFile, oldName)) {
            return source;
        }

        boolean changed = false;
        for (int i = 0; i < lines.length; i++) {
            String renamed = replaceInCode(oldWord, lines[i], newName);
            changed |= !renamed.equals(lines[i]);
            lines[i] = renamed;
        }
        return changed ? join(source, lines) : source;
    }

    private static boolean hasCodeMatch(Pattern pattern, String line, boolean[] code) {
        Matcher m = pattern.matcher(line);
        while (m.find()) {
            if (code[m.start()]) {
                return true;
            }
        }
        return false;
    }

    private static String replaceInCode(Pattern pattern, String line, String replacement) {
        boolean[] code = codeMask(line, false);
        Matcher m = pattern.matcher(line);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            if (!code[m.start()]) {
                continue;
            }
            out.append(line, last, m.start()).append(replacement);
            last = m.end();
        }
        out.append(line.substring(last));
        return out.toString();
    }

    /**
     * Marks which characters of a single line are real code: not inside a string or char literal
     * and not inside a comment. With starLineIsComment, a line beginning with '*' (Javadoc body)
     * counts as comment. Rename passes false so that a wrapped "* 100" expression is never skipped.
     */
    private static boolean[] codeMask(String line, boolean starLineIsComment) {
        boolean[] code = new boolean[line.length()];
        if (starLineIsComment && line.trim().startsWith("*")) {
            return code;
        }
        char quote = 0;
        boolean block = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (block) {
                if (c == '*' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                    block = false;
                    i++;
                }
                continue;
            }
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }
            if (c == '/' && i + 1 < line.length()) {
                char next = line.charAt(i + 1);
                if (next == '/') {
                    break;
                }
                if (next == '*') {
                    block = true;
                    i++;
                    continue;
                }
            }
            code[i] = true;
        }
        return code;
    }

    /**
     * True when another .java file in the same folder reads or writes the field as obj.name.
     * Unreadable neighbours count as a hit, so the fix is skipped rather than risked.
     */
    private static boolean accessedViaDotInSiblings(Path javaFile, String fieldName) {
        if (javaFile == null) {
            return false;
        }
        Path self = javaFile.toAbsolutePath().normalize();
        Path dir = self.getParent();
        if (dir == null) {
            return false;
        }
        Pattern access = Pattern.compile("\\.\\s*" + Pattern.quote(fieldName) + "(?![\\w$])");
        try (DirectoryStream<Path> neighbours = Files.newDirectoryStream(dir, "*.java")) {
            for (Path other : neighbours) {
                if (other.toAbsolutePath().normalize().equals(self)) {
                    continue;
                }
                if (access.matcher(Files.readString(other)).find()) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return true;
        }
        return false;
    }

    private String fixMissingPackage(String source, Path javaFile) {
        if (source.contains("package ")) {
            return source;
        }
        String pkg = packageFromPath(javaFile);
        if (pkg.isBlank()) {
            return source;
        }
        String newline = source.contains("\r\n") ? "\r\n" : "\n";
        return "package " + pkg + ";" + newline + newline + source;
    }

    private String packageFromPath(Path javaFile) {
        Path abs = javaFile.toAbsolutePath().normalize();
        String s = abs.toString().replace('\\', '/');
        int idx = s.indexOf("/src/main/java/");
        if (idx >= 0) {
            String rel = s.substring(idx + "/src/main/java/".length());
            int slash = rel.lastIndexOf('/');
            if (slash > 0) {
                return rel.substring(0, slash).replace('/', '.');
            }
        }
        // fallback: parent folder name as single segment package
        Path parent = abs.getParent();
        if (parent != null && parent.getFileName() != null) {
            String name = parent.getFileName().toString().replaceAll("[^A-Za-z0-9_]", "");
            if (!name.isBlank()) {
                String candidate = name.toLowerCase(Locale.ROOT);
                // "2024", "default", "new" ... are not legal package names
                return SourceVersion.isName(candidate) ? candidate : "app";
            }
        }
        return "app";
    }

    /**
     * Replace the first multi-digit literal in real code on the line with a class constant.
     * Narrow on purpose: digits inside strings/comments are left alone, the file must contain
     * exactly one class or record (so the constant cannot land in the wrong type), and an
     * existing VALUE_n constant is reused instead of declared twice.
     */
    private String fixMagicNumber(String source, int lineNumber) {
        if (lineNumber < 1) {
            return source;
        }
        String[] lines = source.split("\\R", -1);
        int index = lineNumber - 1;
        if (index >= lines.length) {
            return source;
        }
        String line = lines[index];
        if (line.contains("static final")) {
            return source;
        }

        boolean[] code = codeMask(line, true);
        Matcher m = MAGIC_INT_ON_LINE.matcher(line);
        int start = -1;
        int end = -1;
        String number = null;
        while (m.find()) {
            if (code[m.start(1)]) {
                start = m.start(1);
                end = m.end(1);
                number = m.group(1);
                break;
            }
        }
        if (number == null) {
            return source;
        }

        Matcher types = TYPE_DECLARATION.matcher(source);
        if (!types.find()) {
            return source;
        }
        String kind = types.group(1);
        if (!(kind.equals("class") || kind.equals("record")) || types.find()) {
            return source; // interface/enum need other placement; several types make the target ambiguous
        }

        String constName = "VALUE_" + number;
        lines[index] = line.substring(0, start) + constName + line.substring(end);
        String newline = source.contains("\r\n") ? "\r\n" : "\n";
        String joined = String.join(newline, lines);

        boolean alreadyDeclared = Pattern.compile("\\b" + constName + "\\s*=").matcher(source).find();
        if (alreadyDeclared) {
            return joined;
        }
        Matcher declaration = TYPE_DECLARATION.matcher(joined);
        if (!declaration.find()) {
            return source;
        }
        String constantLine = "    private static final int " + constName + " = " + number + ";";
        int brace = declaration.end();
        return joined.substring(0, brace) + newline + constantLine + joined.substring(brace);
    }

    private String fixAllTypeErrors(String source) {
        return replaceInvalidBuiltInTypes(source, -1);
    }

    /** Fixes mistyped types in code only, preserving comments, strings, and text blocks. */
    private String replaceInvalidBuiltInTypes(String source, int targetLine) {
        String masked = JavaSourceMask.maskNonCode(source);
        Matcher matcher = INVALID_BUILT_IN_TYPE.matcher(masked);
        StringBuilder fixed = new StringBuilder(source);
        int line = 1;
        int cursor = 0;
        while (matcher.find()) {
            while (cursor < matcher.start()) {
                char current = masked.charAt(cursor++);
                if (current == '\r') {
                    line++;
                    if (cursor < matcher.start() && masked.charAt(cursor) == '\n') {
                        cursor++;
                    }
                } else if (current == '\n') {
                    line++;
                }
            }
            if (targetLine > 0 && line != targetLine) {
                continue;
            }
            String replacement = switch (matcher.group(1)) {
                case "Int" -> "int";
                case "Char" -> "char";
                case "string" -> "String";
                default -> matcher.group(1);
            };
            fixed.replace(matcher.start(), matcher.end(), replacement);
        }
        return fixed.toString();
    }

    private static String join(String source, String[] lines) {
        String newline = source.contains("\r\n") ? "\r\n" : "\n";
        return String.join(newline, lines);
    }
}
