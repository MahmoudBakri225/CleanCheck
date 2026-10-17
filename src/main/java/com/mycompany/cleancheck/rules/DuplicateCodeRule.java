package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.mycompany.cleancheck.analysis.JavaAst;
import com.mycompany.cleancheck.analysis.JavaSourceMask;
import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Methods whose bodies are identical (ignoring comments and layout), within one file or across files.
 * Only bodies with at least {@code minStatements} statements count, so trivial getters never match.
 */
public class DuplicateCodeRule implements ProjectRule {

    private record Occurrence(String file, int line, String name) {
    }

    private final int minStatements;

    public DuplicateCodeRule() {
        this(5);
    }

    public DuplicateCodeRule(int minStatements) {
        this.minStatements = minStatements;
    }

    @Override
    public String getName() {
        return "Duplicate Code";
    }

    @Override
    public List<Warning> analyze(Map<String, String> sourcesByFile) {
        Map<String, List<Occurrence>> groups = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : sourcesByFile.entrySet()) {
            JavaAst.parse(entry.getValue()).ifPresent(cu -> {
                List<CallableDeclaration<?>> callables = new ArrayList<>();
                callables.addAll(cu.findAll(MethodDeclaration.class));
                callables.addAll(cu.findAll(ConstructorDeclaration.class));
                for (CallableDeclaration<?> callable : callables) {
                    BlockStmt body = callable instanceof MethodDeclaration m
                            ? m.getBody().orElse(null)
                            : ((ConstructorDeclaration) callable).getBody();
                    if (body == null || body.getStatements().size() < minStatements) {
                        continue;
                    }
                    String key = normalizeWhitespaceOutsideLiterals(JavaSourceMask.maskComments(body.toString()));
                    groups.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new Occurrence(entry.getKey(), JavaAst.line(callable), callable.getNameAsString()));
                }
            });
        }

        List<Warning> warnings = new ArrayList<>();
        for (List<Occurrence> group : groups.values()) {
            if (group.size() < 2) {
                continue;
            }
            Occurrence first = group.get(0);
            for (Occurrence copy : group.subList(1, group.size())) {
                warnings.add(new Warning(
                        copy.file(),
                        copy.line(),
                        getName(),
                        Severity.WARNING,
                        "'" + copy.name() + "' has the same body as '" + first.name() + "' in "
                                + first.file() + ":" + first.line() + ".",
                        "Extract the shared code into one method and call it from both places."
                ));
            }
        }
        return warnings;
    }

    /** Formatting is ignored, but whitespace inside strings, chars and text blocks is semantic. */
    private static String normalizeWhitespaceOutsideLiterals(String source) {
        StringBuilder normalized = new StringBuilder(source.length());
        boolean pendingSpace = false;
        boolean escaped = false;
        char quote = 0;
        boolean textBlock = false;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (textBlock) {
                normalized.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"' && i + 2 < source.length()
                        && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"') {
                    normalized.append("\"\"");
                    i += 2;
                    textBlock = false;
                }
                continue;
            }
            if (quote != 0) {
                normalized.append(current);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '"' && i + 2 < source.length()
                    && source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"') {
                appendPendingSpace(normalized, pendingSpace);
                pendingSpace = false;
                normalized.append("\"\"\"");
                i += 2;
                textBlock = true;
            } else if (current == '"' || current == '\'') {
                appendPendingSpace(normalized, pendingSpace);
                pendingSpace = false;
                normalized.append(current);
                quote = current;
            } else if (Character.isWhitespace(current)) {
                pendingSpace = true;
            } else {
                appendPendingSpace(normalized, pendingSpace);
                pendingSpace = false;
                normalized.append(current);
            }
        }
        return normalized.toString().trim();
    }

    private static void appendPendingSpace(StringBuilder target, boolean pendingSpace) {
        if (pendingSpace && target.length() > 0) {
            target.append(' ');
        }
    }
}
