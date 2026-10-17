package com.mycompany.cleancheck.rules;

import com.github.javaparser.ast.comments.LineComment;
import com.mycompany.cleancheck.analysis.JavaAst;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads "// cleancheck:ignore [Rule, Rule]" comments. A directive silences findings on its own line
 * and on the line right after it; "// cleancheck:ignore-file" silences the whole file.
 * Without rule names, every rule is silenced.
 */
final class IgnoreDirectives {

    private static final Pattern DIRECTIVE = Pattern.compile("^\\s*cleancheck:ignore(-file)?\\b([^\\r\\n]*)");

    private boolean wholeFile;
    private Set<String> wholeFileRules = Set.of();
    private final Map<Integer, Set<String>> byLine = new HashMap<>();

    static IgnoreDirectives parse(String source) {
        IgnoreDirectives result = new IgnoreDirectives();
        if (source == null || !source.contains("cleancheck:ignore")) {
            return result;
        }
        JavaAst.parse(source).ifPresent(unit -> {
            for (LineComment comment : unit.getAllComments().stream()
                    .filter(comment -> comment instanceof LineComment)
                    .map(comment -> (LineComment) comment)
                    .toList()) {
                Matcher m = DIRECTIVE.matcher(comment.getContent());
                if (!m.find()) {
                    continue;
                }
                Set<String> rules = new HashSet<>();
                for (String name : m.group(2).split(",")) {
                    String trimmed = name.trim().toLowerCase(Locale.ROOT);
                    if (!trimmed.isEmpty()) {
                        rules.add(trimmed);
                    }
                }
                if (m.group(1) != null) {
                    result.wholeFile = true;
                    result.wholeFileRules = rules;
                } else {
                    int line = comment.getBegin().map(position -> position.line).orElse(1);
                    result.byLine.put(line, rules);
                }
            }
        });
        return result;
    }

    boolean suppresses(int line, String ruleName) {
        String rule = ruleName.toLowerCase(Locale.ROOT);
        if (wholeFile && (wholeFileRules.isEmpty() || wholeFileRules.contains(rule))) {
            return true;
        }
        for (int candidate : new int[]{line, line - 1}) {
            Set<String> rules = byLine.get(candidate);
            if (rules != null && (rules.isEmpty() || rules.contains(rule))) {
                return true;
            }
        }
        return false;
    }
}
