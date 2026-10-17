package com.mycompany.cleancheck.ui;

public class IssueRow {

    private final String file;
    private final String line;
    private final String rule;
    private final String severity;
    private final String message;
    private final String suggestion;
    private String status;

    public IssueRow(
            String file,
            String line,
            String rule,
            String severity,
            String message,
            String suggestion
    ) {
        this(file, line, rule, severity, message, suggestion, "Open");
    }

    public IssueRow(
            String file,
            String line,
            String rule,
            String severity,
            String message,
            String suggestion,
            String status
    ) {
        this.file = file;
        this.line = line;
        this.rule = rule;
        this.severity = severity;
        this.message = message;
        this.suggestion = suggestion;
        this.status = status == null || status.isBlank() ? "Open" : status;
    }

    public static IssueRow from(com.mycompany.cleancheck.analysis.Warning warning) {
        return new IssueRow(
                warning.getFileName(),
                String.valueOf(warning.getLineNumber()),
                warning.getRuleName(),
                warning.getSeverity().toString(),
                warning.getMessage(),
                warning.getSuggestion()
        );
    }

    public String getFile() {
        return file;
    }

    public String getLine() {
        return line;
    }

    public String getRule() {
        return rule;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public IssueRow asFixed() {
        return new IssueRow(file, line, rule, severity, message, suggestion, "Fixed");
    }
}
