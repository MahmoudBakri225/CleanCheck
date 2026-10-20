package com.mycompany.cleancheck.quality;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;

import java.util.List;

/**
 * Rough technical-debt estimate derived from finding severity and volume.
 * This is an educational heuristic, not a precise industrial metric.
 *
 * <p>Assumptions (minutes of remediation effort):
 * <ul>
 *   <li>CRITICAL → 30 min</li>
 *   <li>WARNING → 10 min</li>
 *   <li>INFO → 3 min</li>
 * </ul>
 */
public final class TechnicalDebtEstimate {

    private final int criticalCount;
    private final int warningCount;
    private final int infoCount;
    private final int estimatedMinutes;

    public TechnicalDebtEstimate(List<Warning> warnings) {
        int c = 0, w = 0, i = 0;
        if (warnings != null) {
            for (Warning warning : warnings) {
                if (warning == null || warning.getSeverity() == null) {
                    continue;
                }
                switch (warning.getSeverity()) {
                    case CRITICAL -> c++;
                    case WARNING -> w++;
                    case INFO -> i++;
                }
            }
        }
        this.criticalCount = c;
        this.warningCount = w;
        this.infoCount = i;
        this.estimatedMinutes = c * 30 + w * 10 + i * 3;
    }

    public int criticalCount() { return criticalCount; }
    public int warningCount() { return warningCount; }
    public int infoCount() { return infoCount; }
    public int estimatedMinutes() { return estimatedMinutes; }

    /** Human-readable summary, e.g. "≈ 2h 15m estimated remediation". */
    public String summary() {
        if (estimatedMinutes <= 0) {
            return "No estimated technical debt from current findings.";
        }
        int hours = estimatedMinutes / 60;
        int minutes = estimatedMinutes % 60;
        if (hours == 0) {
            return "≈ " + minutes + "m estimated remediation effort";
        }
        if (minutes == 0) {
            return "≈ " + hours + "h estimated remediation effort";
        }
        return "≈ " + hours + "h " + minutes + "m estimated remediation effort";
    }
}
