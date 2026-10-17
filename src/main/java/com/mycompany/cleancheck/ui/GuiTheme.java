package com.mycompany.cleancheck.ui;

/** Theme aligned to the Figma mockups (light blue SaaS look). */
public final class GuiTheme {

    public static final String BG = "-cc-bg";
    public static final String CARD = "-cc-card";
    public static final String SIDEBAR_BG = "-cc-sidebar";
    public static final String TEXT = "-cc-text";
    public static final String MUTED = "-cc-muted";
    public static final String BORDER = "-cc-border";
    public static final String PRIMARY = "-cc-primary";
    public static final String PRIMARY_SOFT = "-cc-primary-soft";
    public static final String TINT_CRITICAL = "-cc-tint-critical";
    public static final String TINT_CRITICAL_BORDER = "-cc-tint-critical-border";
    public static final String TINT_WARNING = "-cc-tint-warning";
    public static final String TINT_WARNING_BORDER = "-cc-tint-warning-border";
    public static final String TINT_INFO = "-cc-tint-info";
    public static final String TINT_INFO_BORDER = "-cc-tint-info-border";

    public static final String HEADER_BG = "#0F172A";
    public static final String CRITICAL = "#EF4444";
    public static final String WARNING = "#F59E0B";
    public static final String INFO = "#3B82F6";
    public static final String SUCCESS = "#22C55E";
    public static final String ACCENT = "#2563EB";

    private static final String LIGHT_VARIABLES
            = "-cc-bg: #F4F7FE;"
            + "-cc-card: #FFFFFF;"
            + "-cc-sidebar: #FFFFFF;"
            + "-cc-text: #0F172A;"
            + "-cc-muted: #64748B;"
            + "-cc-border: #E8EEF7;"
            + "-cc-primary: #2563EB;"
            + "-cc-primary-soft: #E8F0FE;"
            + "-cc-tint-critical: #FEF2F2;"
            + "-cc-tint-critical-border: #FECACA;"
            + "-cc-tint-warning: #FFFBEB;"
            + "-cc-tint-warning-border: #FDE68A;"
            + "-cc-tint-info: #EFF6FF;"
            + "-cc-tint-info-border: #BFDBFE;";

    private static final String DARK_VARIABLES
            = "-cc-bg: #0B1220;"
            + "-cc-card: #151C31;"
            + "-cc-sidebar: #10182B;"
            + "-cc-text: #E8EEF9;"
            + "-cc-muted: #94A3B8;"
            + "-cc-border: #243049;"
            + "-cc-primary: #60A5FA;"
            + "-cc-primary-soft: #1E3A5F;"
            + "-cc-tint-critical: #3F1D25;"
            + "-cc-tint-critical-border: #7F1D1D;"
            + "-cc-tint-warning: #3B2A12;"
            + "-cc-tint-warning-border: #92400E;"
            + "-cc-tint-info: #172554;"
            + "-cc-tint-info-border: #1E3A8A;";

    private GuiTheme() {
    }

    public static String rootStyle(boolean dark) {
        return (dark ? DARK_VARIABLES : LIGHT_VARIABLES)
                + "-fx-background-color: " + BG + ";"
                + "-fx-font-family: 'Segoe UI', 'Inter', 'System';";
    }

    public static String cardStyle() {
        return "-fx-background-color: " + CARD + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.07), 24, 0, 0, 8);";
    }

    public static String primaryButton() {
        return "-fx-background-color: #2563EB;"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 13px;"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 11 20 11 20;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.35), 12, 0, 0, 4);";
    }

    public static String successButton() {
        return "-fx-background-color: #16A34A;"
                + "-fx-text-fill: white;"
                + "-fx-font-weight: bold;"
                + "-fx-font-size: 13px;"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 11 20 11 20;"
                + "-fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.30), 10, 0, 0, 3);";
    }

    public static String navSelected() {
        // theme variables, so the selected item is readable in both light and dark mode
        return "-fx-background-color: " + PRIMARY_SOFT + ";"
                + "-fx-text-fill: " + PRIMARY + ";"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 12 14 12 14;"
                + "-fx-font-size: 13px;"
                + "-fx-cursor: hand;"
                + "-fx-border-color: transparent transparent transparent " + PRIMARY + ";"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 0 0 0 3;";
    }

    public static String navNormal() {
        return "-fx-background-color: transparent;"
                + "-fx-text-fill: " + MUTED + ";"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 12 14 12 14;"
                + "-fx-font-size: 13px;"
                + "-fx-cursor: hand;";
    }

    public static String tableStyle() {
        return "-fx-background-color: transparent; -fx-border-color: transparent;";
    }

    public static String sidebarStyle() {
        return "-fx-background-color: " + SIDEBAR_BG + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 1 0 0;";
    }

    public static String headerStyle() {
        return "-fx-background-color: #0F172A;";
    }

    public static String statusStripStyle() {
        return "-fx-background-color: " + CARD + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 0 1 0;";
    }
}
