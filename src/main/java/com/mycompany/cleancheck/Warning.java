
package com.mycompany.cleancheck;

public class Warning {
    private final String fileName;
    private final int lineNumber;
    private final String ruleName;
    private final Severity severity;
    private final String message;
    private final String suggestion;

    public Warning(String fileName, int lineNumber, String ruleName,
                   Severity severity, String message, String suggestion) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.ruleName = ruleName;
        this.severity = severity;
        this.message = message;
        this.suggestion = suggestion;
    }

    public String getFileName() { return fileName; }
    public int getLineNumber() { return lineNumber; }
    public String getRuleName() { return ruleName; }
    public Severity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getSuggestion() { return suggestion; }

    @Override
    public String toString() {
        return "[" + severity + "] "
                + fileName + " : Line " + lineNumber
                + " | " + ruleName + " | " + message
                + "\nSuggestion: " + suggestion;
    }
}