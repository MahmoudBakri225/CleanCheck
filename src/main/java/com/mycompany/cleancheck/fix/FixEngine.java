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
 * Safe automatic fixes:
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

    private static final Pattern INVALID_BUILT_IN_TYPE = Pattern.compile(
            "\\b(Int|Char|string)(?:\\s*\\[\\s*\\])*\\s+(?=[A-Za-z_$])");

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
            // Keep a user-selected fix scoped to the reported line. Bulk replacement is available
            // only through applyFixes(), where the caller explicitly requested a whole-file pass.
            after = fixTypeLine(before, lineNumber);
        } else if (ruleName.equalsIgnoreCase("Encapsulation")) {
            after = fixEncapsulationLine(before, lineNumber, javaFile);
        } else if (ruleName.equalsIgnoreCase("System Out")) {
            after = fixSystemOutLine(before, lineNumber);
        } else if (ruleName.equalsIgnoreCase("Naming Convention")) {
            after = fixNamingLine(before, lineNumber, javaFile);
        } else if (ruleName.equalsIgnoreCase("Missing Package")) {
            after = fixMissingPackage(before, javaFile);
        } else if (ruleName.equalsIgnoreCase("Magic Number")) {
            after = fixMagicNumber(before, lineNumber);
        } else if (ruleName.equalsIgnoreCase("Unused Import")) {
            after = fixUnusedImport(before, lineNumber);
        } else if (ruleName.equalsIgnoreCase("String Comparison")) {
            after = fixStringComparison(before, lineNumber);
        } else if (ruleName.equalsIgnoreCase("Unused Variable")) {
            after = fixUnusedVariable(before, lineNumber);
        } else if (ruleName.equalsIgnoreCase("Information Leak")) {
            after = fixPrintStackTrace(before, lineNumber, javaFile);
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
        if (hasSideEffectfulConsoleArgument(lines[index])) {
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

    private static boolean hasSideEffectfulConsoleArgument(String line) {
        int open = line.indexOf('(');
        int close = line.lastIndexOf(')');
        if (open < 0 || close <= open) {
            return true;
        }
        String arguments = line.substring(open + 1, close).trim();
        return arguments.matches("(?s).*(\\+\\+|--|\\bnew\\s+|(?<![=!<>])=(?!=)|[A-Za-z_$][\\w$.]*\\s*\\().*");
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

        // Mask the complete source at once. A line-by-line mask loses block-comment and
        // text-block state, which could otherwise rename words inside multi-line comments or
        // text blocks even though they are not Java identifiers in executable code.
        String maskedSource = JavaSourceMask.maskNonCode(source);
        if (newWord.matcher(maskedSource).find()
                || typeOrMethodUse.matcher(maskedSource).find()
                || foreignQualifier.matcher(maskedSource).find()) {
            return source;
        }
        if (accessedViaDotInSiblings(javaFile, oldName)) {
            return source;
        }

        List<int[]> replacements = new ArrayList<>();
        Matcher occurrences = oldWord.matcher(maskedSource);
        while (occurrences.find()) {
            replacements.add(new int[]{occurrences.start(), occurrences.end()});
        }
        if (replacements.isEmpty()) {
            return source;
        }
        StringBuilder renamed = new StringBuilder(source);
        for (int i = replacements.size() - 1; i >= 0; i--) {
            int[] range = replacements.get(i);
            renamed.replace(range[0], range[1], newName);
        }
        return renamed.toString();
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
        if (javaFile.getFileName() != null && javaFile.getFileName().toString().equals("module-info.java")) {
            return source;
        }
        // Ignore occurrences in comments and string literals; only a real package declaration
        // should suppress this fix.
        String maskedSource = JavaSourceMask.maskNonCode(source);
        if (Pattern.compile("(?m)^\\s*package\\s+[A-Za-z_$][\\w$]*(?:\\s*\\.\\s*[A-Za-z_$][\\w$]*)*\\s*;")
                .matcher(maskedSource).find()) {
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
        if (idx < 0) {
            idx = s.indexOf("/src/test/java/");
        }
        if (idx >= 0) {
            String sourceRoot = s.startsWith("/src/main/java/", idx)
                    ? "/src/main/java/" : "/src/test/java/";
            String rel = s.substring(idx + sourceRoot.length());
            int slash = rel.lastIndexOf('/');
            if (slash > 0) {
                return rel.substring(0, slash).replace('/', '.');
            }
            // A file directly in the source root belongs to Java's unnamed package.
            return "";
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
     * True at each index that is real code (not comment / string / text-block content).
     * When {@code maskStrings} is true, string and text-block interiors are treated as non-code;
     * when false, only comments are masked.
     */
    private static boolean[] codeMask(String source, boolean maskStrings) {
        String masked = maskStrings
                ? JavaSourceMask.maskNonCode(source)
                : JavaSourceMask.maskComments(source);
        boolean[] code = new boolean[source.length()];
        for (int i = 0; i < source.length(); i++) {
            char original = source.charAt(i);
            if (original == '\n' || original == '\r') {
                code[i] = false;
                continue;
            }
            // maskNonCode / maskComments replace non-code characters with spaces.
            code[i] = masked.charAt(i) == original;
        }
        return code;
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
        String joined = join(source, lines);

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
        List<int[]> replacements = new ArrayList<>();
        List<String> values = new ArrayList<>();
        while (matcher.find()) {
            int afterToken = matcher.end();
            while (afterToken < masked.length() && Character.isWhitespace(masked.charAt(afterToken))) {
                afterToken++;
            }
            if (afterToken >= masked.length() || !Character.isJavaIdentifierStart(masked.charAt(afterToken))) {
                continue;
            }
            if (targetLine > 0 && lineNumberAt(masked, matcher.start(1)) != targetLine) {
                continue;
            }
            String replacement = switch (matcher.group(1)) {
                case "Int" -> "int";
                case "Char" -> "char";
                case "string" -> "String";
                default -> matcher.group(1);
            };
            replacements.add(new int[]{matcher.start(1), matcher.end(1)});
            values.add(replacement);
        }
        // Apply from right to left: String is one character longer than string, so forward
        // replacements would otherwise shift offsets for later matches in the original source.
        StringBuilder fixed = new StringBuilder(source);
        for (int i = replacements.size() - 1; i >= 0; i--) {
            int[] range = replacements.get(i);
            fixed.replace(range[0], range[1], values.get(i));
        }
        return fixed.toString();
    }

    private static int lineNumberAt(String source, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            char c = source.charAt(i);
            if (c == '\r') {
                line++;
                if (i + 1 < offset && source.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String join(String source, String[] lines) {
        StringBuilder result = new StringBuilder(source.length() + lines.length);
        int cursor = 0;
        for (int i = 0; i < lines.length; i++) {
            result.append(lines[i]);
            if (i + 1 < lines.length) {
                while (cursor < source.length()
                        && source.charAt(cursor) != '\r' && source.charAt(cursor) != '\n') {
                    cursor++;
                }
                if (cursor < source.length() && source.charAt(cursor) == '\r') {
                    result.append('\r');
                    cursor++;
                }
                if (cursor < source.length() && source.charAt(cursor) == '\n') {
                    result.append('\n');
                    cursor++;
                }
            }
        }
        return result.toString();
    }
}
