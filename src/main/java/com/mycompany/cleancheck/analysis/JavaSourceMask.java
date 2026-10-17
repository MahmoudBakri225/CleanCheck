package com.mycompany.cleancheck.analysis;

public final class JavaSourceMask {

    private JavaSourceMask() {
    }

    public static String maskNonCode(String source) {
        StringBuilder masked = new StringBuilder(source);
        int i = 0;
        while (i < source.length()) {
            char ch = source.charAt(i);
            if (ch == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                i = maskUntilLineEnd(source, masked, i);
            } else if (ch == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                int limit = end < 0 ? source.length() : end + 2;
                maskRange(source, masked, i, limit);
                i = limit;
            } else if (ch == '"' && source.startsWith("\"\"\"", i)) {
                int limit = skipTextBlock(source, i);
                maskRange(source, masked, i, limit);
                i = limit;
            } else if (ch == '"' || ch == '\'') {
                i = maskQuoted(source, masked, i, ch);
            } else {
                i++;
            }
        }
        return masked.toString();
    }

    /** Replaces comments with spaces while preserving string, character and text-block literals. */
    public static String maskComments(String source) {
        StringBuilder masked = new StringBuilder(source);
        int i = 0;
        while (i < source.length()) {
            char ch = source.charAt(i);
            if (ch == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                i = maskUntilLineEnd(source, masked, i);
            } else if (ch == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                int limit = end < 0 ? source.length() : end + 2;
                maskRange(source, masked, i, limit);
                i = limit;
            } else if (ch == '"' && source.startsWith("\"\"\"", i)) {
                i = skipTextBlock(source, i);
            } else if (ch == '"' || ch == '\'') {
                i = skipQuoted(source, i, ch);
            } else {
                i++;
            }
        }
        return masked.toString();
    }

    private static int maskUntilLineEnd(String source, StringBuilder masked, int start) {
        int end = start + 2;
        while (end < source.length() && source.charAt(end) != '\n' && source.charAt(end) != '\r') {
            end++;
        }
        maskRange(source, masked, start, end);
        return end;
    }

    private static int maskQuoted(String source, StringBuilder masked, int start, char quote) {
        int i = skipQuoted(source, start, quote);
        maskRange(source, masked, start, i);
        return i;
    }

    private static int skipQuoted(String source, int start, char quote) {
        int i = start + 1;
        while (i < source.length()) {
            char ch = source.charAt(i++);
            if (ch == '\\' && i < source.length()) {
                i++;
            } else if (ch == quote) {
                break;
            }
        }
        return i;
    }

    private static int skipTextBlock(String source, int start) {
        int i = start + 3;
        while (i < source.length()) {
            if (source.startsWith("\"\"\"", i)) {
                int backslashes = 0;
                for (int j = i - 1; j >= start && source.charAt(j) == '\\'; j--) {
                    backslashes++;
                }
                if (backslashes % 2 == 0) {
                    return i + 3;
                }
                i += 3;
            } else {
                i++;
            }
        }
        return source.length();
    }

    private static void maskRange(String source, StringBuilder masked, int start, int end) {
        for (int i = start; i < end; i++) {
            char ch = source.charAt(i);
            if (ch != '\n' && ch != '\r') {
                masked.setCharAt(i, ' ');
            }
        }
    }
}
