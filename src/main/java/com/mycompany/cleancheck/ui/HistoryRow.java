package com.mycompany.cleancheck.ui;

public class HistoryRow {

    private final String scanId;
    private final String date;
    private final String score;
    private final String issues;
    private final String critical;
    private final String warnings;
    private final String info;

    public HistoryRow(
            int scanId,
            String date,
            int score,
            int issues,
            int critical,
            int warnings,
            int info
    ) {
        this.scanId = String.valueOf(scanId);
        this.date = date;
        this.score = score + " / 100";
        this.issues = String.valueOf(issues);
        this.critical = String.valueOf(critical);
        this.warnings = String.valueOf(warnings);
        this.info = String.valueOf(info);
    }

    public String getScanId() {
        return scanId;
    }

    public String getDate() {
        return date;
    }

    public String getScore() {
        return score;
    }

    public String getIssues() {
        return issues;
    }

    public String getCritical() {
        return critical;
    }

    public String getWarnings() {
        return warnings;
    }

    public String getInfo() {
        return info;
    }
}
