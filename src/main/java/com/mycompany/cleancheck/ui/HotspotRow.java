package com.mycompany.cleancheck.ui;

public class HotspotRow {

    private final String name;
    private final String issues;
    private final String riskScore;
    private final int topWeight;

    public HotspotRow(String name, int issues, int riskScore) {
        this(name, issues, riskScore, 0);
    }

    /** @param topWeight risk weight of the worst finding in the group (critical 5, warning 3, info 1) */
    public HotspotRow(String name, int issues, int riskScore, int topWeight) {
        this.name = name;
        this.issues = String.valueOf(issues);
        this.riskScore = String.valueOf(riskScore);
        this.topWeight = topWeight;
    }

    public int getTopWeight() {
        return topWeight;
    }

    public String getName() {
        return name;
    }

    public String getIssues() {
        return issues;
    }

    public String getRiskScore() {
        return riskScore;
    }
}
