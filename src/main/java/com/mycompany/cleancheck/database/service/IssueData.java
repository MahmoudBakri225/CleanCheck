package com.mycompany.cleancheck.database.service;

/** Plain data holder for one stored issue. */
final class IssueData {

    String fileName;
    int lineNumber;
    String ruleName;
    String severity;
    String message;
    String suggestion;
    String fingerprint;
}
