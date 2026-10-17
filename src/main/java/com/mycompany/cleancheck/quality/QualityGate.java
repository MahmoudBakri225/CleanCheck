package com.mycompany.cleancheck.quality;

public class QualityGate {

    private final int minimumScore;
    private final int currentScore;
    private final int criticalIssues;

    public QualityGate(int minimumScore, int currentScore, int criticalIssues) {
        if (minimumScore < 0 || minimumScore > 100) {
            throw new IllegalArgumentException("Minimum quality score must be between 0 and 100.");
        }
        if (currentScore < 0 || currentScore > 100) {
            throw new IllegalArgumentException("Current quality score must be between 0 and 100.");
        }
        if (criticalIssues < 0) {
            throw new IllegalArgumentException("Critical issue count cannot be negative.");
        }
        this.minimumScore = minimumScore;
        this.currentScore = currentScore;
        this.criticalIssues = criticalIssues;
    }

    public boolean isPassed() {
        return criticalIssues == 0 && currentScore >= minimumScore;
    }

    public String getStatus() {
        return isPassed() ? "PASSED" : "FAILED";
    }

    public String getReason() {
        if (criticalIssues > 0) {
            return "Critical issues found (" + criticalIssues + ").";
        }
        if (currentScore < minimumScore) {
            return "Quality score " + currentScore + " is below required minimum " + minimumScore + ".";
        }
        return "Quality requirements satisfied.";
    }

    public int getMinimumScore() {
        return minimumScore;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getCriticalIssues() {
        return criticalIssues;
    }
}
