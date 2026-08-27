package com.mycompany.cleancheck;

/**
 * Represents a warning detected during the CleanCheck code analysis.
 * It stores all information about the detected issue, including the file name,
 * line number, rule name, severity, message, and suggested solution.
 */
public class C_Warning {

    private final String fileName;
    private final int lineNumber;
    private final String ruleName;
    private final E_Severity severity;
    private final String message;
    private final String suggestion;

    /**
     * Creates a warning object and initializes all information related to the
     * detected code quality issue.
     *
     * @param fileName   the name of the source file where the issue was found
     * @param lineNumber the line number of the issue
     * @param ruleName   the name of the rule that detected the issue
     * @param severity   the severity level of the issue
     * @param message    a short description of the problem
     * @param suggestion a recommended way to fix the problem
     */
    public C_Warning(String fileName, int lineNumber, String ruleName,
            E_Severity severity, String message, String suggestion) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.ruleName = ruleName;
        this.severity = severity;
        this.message = message;
        this.suggestion = suggestion;
    }

    /**
     * Returns the name of the file where the issue was detected.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number where the issue was detected.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the name of the rule that detected the issue.
     *
     * @return the rule name
     */
    public String getRuleName() {
        return ruleName;
    }

    /**
     * Returns the severity level of the detected issue.
     *
     * @return the severity
     */
    public E_Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the message describing the detected issue.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the suggested solution for fixing the detected issue.
     *
     * @return the suggestion
     */
    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Returns a formatted text representation of the warning, including its
     * severity, file name, line number, rule, message, and suggestion.
     *
     * @return a multi-line string representation of this warning
     */
    @Override
    public String toString() {
        return "! severity : " + severity + " | fileName : " + fileName + " | Line : " + lineNumber
                + " | ruleName : " + ruleName
                + System.lineSeparator() + "Message    : " + message
                + System.lineSeparator() + "Suggestion : " + suggestion;
    }
}