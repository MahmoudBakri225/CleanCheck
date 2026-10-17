package com.mycompany.cleancheck.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IssueOutputParser {

    private static final Pattern HEADER = Pattern.compile(
            "^!\\s*severity\\s*:\\s*(.*?)"
                    + "\\s*\\|\\s*fileName\\s*:\\s*(.*?)"
                    + "\\s*\\|\\s*Line\\s*:\\s*(.*?)"
                    + "\\s*\\|\\s*ruleName\\s*:\\s*(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    private IssueOutputParser() {
    }

    public static List<IssueRow> parse(String output) {
        List<IssueRow> rows = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return rows;
        }

        Set<String> keys = new HashSet<>();
        String severity = "";
        String file = "";
        String line = "";
        String rule = "";
        String message = "";
        String suggestion = "";

        for (String raw : output.split("\\R")) {
            String text = raw.trim();
            Matcher m = HEADER.matcher(text);
            if (m.matches()) {
                add(rows, keys, file, line, rule, severity, message, suggestion);
                severity = m.group(1).trim();
                file = m.group(2).trim();
                line = m.group(3).trim();
                rule = m.group(4).trim();
                message = "";
                suggestion = "";
                continue;
            }
            if (text.startsWith("Message") && text.contains(":")) {
                message = text.substring(text.indexOf(':') + 1).trim();
            } else if (text.startsWith("Suggestion") && text.contains(":")) {
                suggestion = text.substring(text.indexOf(':') + 1).trim();
            }
        }
        add(rows, keys, file, line, rule, severity, message, suggestion);
        return rows;
    }

    private static void add(
            List<IssueRow> rows,
            Set<String> keys,
            String file,
            String line,
            String rule,
            String severity,
            String message,
            String suggestion
    ) {
        if (severity == null || severity.isEmpty()) {
            return;
        }
        String key = file + "|" + line + "|" + rule + "|" + severity + "|" + message;
        if (keys.add(key)) {
            rows.add(new IssueRow(file, line, rule, severity, message, suggestion));
        }
    }
}
