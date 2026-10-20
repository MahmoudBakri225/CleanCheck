package com.mycompany.cleancheck.ui;

/**
 * Stable identifiers for the main application pages.
 * Keeps navigation strings in one place instead of scattering literals.
 */
public enum PageId {
    HOME("Home"),
    DASHBOARD("Dashboard"),
    ANALYZE("Analyze"),
    SCAN_RESULTS("Scan Results"),
    ISSUES("Issues"),
    METRICS("Metrics"),
    SECURITY("Security Review"),
    RULES("Rules"),
    HISTORY("Quality History"),
    REGRESSION("Regression"),
    CLASS_MODE("Class Mode"),
    FIX_VERIFICATION("Fix Verification"),
    ABOUT("About");

    private final String label;

    PageId(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Resolves a nav label to a page id.
     * Returns null when the label is unknown (caller should keep the original string).
     */
    public static PageId fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (PageId id : values()) {
            if (id.label.equalsIgnoreCase(label.trim())) {
                return id;
            }
        }
        return null;
    }
}
