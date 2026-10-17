package com.mycompany.cleancheck.ui;

/** UI-free helpers that turn a finding into the risk / confidence labels shown in the details panel. */
public final class IssueAdvisor {

    private IssueAdvisor() {
    }

    public static String riskLabel(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return "High";
        }
        if ("WARNING".equalsIgnoreCase(severity)) {
            return "Medium";
        }
        return "Low";
    }

    public static String confidenceLabel(String rule) {
        if (rule == null) {
            return "Medium";
        }
        if (rule.equalsIgnoreCase("Infinite Loop")
                || rule.equalsIgnoreCase("Empty If")
                || rule.equalsIgnoreCase("Empty Method")
                || rule.equalsIgnoreCase("Encapsulation")
                || rule.equalsIgnoreCase("Parse Error")
                || rule.equalsIgnoreCase("String Comparison")
                || rule.equalsIgnoreCase("Unused Import")) {
            return "High";
        }
        return "Medium";
    }

    /** Critical=5, Warning=3, Info=1 (same weights as the HTML report and the quality score). */
    public static int riskWeight(String severity) {
        if (severity == null) {
            return 0;
        }
        return switch (severity.toUpperCase(java.util.Locale.ROOT)) {
            case "CRITICAL" -> 5;
            case "WARNING" -> 3;
            case "INFO" -> 1;
            default -> 0;
        };
    }
}
