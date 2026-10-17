package com.mycompany.cleancheck;

public class C_QualityGate {

    private final int previousScore;
    private final int currentScore;
    private final int newCriticalIssues;

    public C_QualityGate(
            int previousScore,
            int currentScore,
            int newCriticalIssues
    ) {
        this.previousScore = previousScore;
        this.currentScore = currentScore;
        this.newCriticalIssues = newCriticalIssues;
    }

    public boolean isPassed() {

        if (currentScore < previousScore) {
            return false;
        }

        if (newCriticalIssues > 0) {
            return false;
        }

        return true;
    }

    public String getStatus() {

        if (isPassed()) {
            return "PASSED";
        }

        return "FAILED";
    }

    public String getReason() {

        if (currentScore < previousScore
                && newCriticalIssues > 0) {

            return "Quality score decreased and new critical issues were introduced.";
        }

        if (currentScore < previousScore) {

            return "Quality score decreased.";
        }

        if (newCriticalIssues > 0) {

            return "New critical issues were introduced.";
        }

        return "Quality requirements satisfied.";
    }

    public int getPreviousScore() {
        return previousScore;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getNewCriticalIssues() {
        return newCriticalIssues;
    }
}