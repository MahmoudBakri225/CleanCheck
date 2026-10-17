package com.mycompany.cleancheck.report;

import com.mycompany.cleancheck.analysis.Warning;

/** Shared formatting helpers for the HTML report (escaping, severity names and weights). */
final class ReportFormat {

    private ReportFormat() {
    }

    /*
     * =========================================================
     * SEVERITY WEIGHT
     * =========================================================
     */
    static int getSeverityWeight(
            Warning warning
    ) {

        String severity
                = getSeverityName(warning);

        if ("CRITICAL".equals(severity)) {
            return 5;
        }

        if ("WARNING".equals(severity)) {
            return 3;
        }

        return 1;
    }

    /*
     * =========================================================
     * SEVERITY NAME
     * =========================================================
     */
    static String getSeverityName(
            Warning warning
    ) {

        if (warning == null
                || warning.getSeverity() == null) {

            return "";
        }

        return warning
                .getSeverity()
                .toString()
                .toUpperCase();
    }


    /*
     * =========================================================
     * SEVERITY CSS CLASS
     * =========================================================
     */
    static String getSeverityClass(
            String severity
    ) {

        if ("CRITICAL".equals(severity)) {
            return "critical";
        }

        if ("WARNING".equals(severity)) {
            return "warning";
        }

        return "info";
    }

    /*
     * =========================================================
     * SIGNED VALUE
     * =========================================================
     */
    static String formatSignedValue(
            int value
    ) {

        if (value > 0) {
            return "+" + value;
        }

        return String.valueOf(value);
    }

    /*
     * =========================================================
     * HTML ESCAPE
     * =========================================================
     */
    static String escapeHtml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
