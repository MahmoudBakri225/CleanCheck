package com.mycompany.cleancheck.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Minimal Java tokenizer used to colour the code review. UI-free, line based, aware of block comments. */
public final class JavaSyntax {

    public enum Kind { PLAIN, KEYWORD, STRING, COMMENT, NUMBER, ANNOTATION }

    public record Token(String text, Kind kind) {
    }

    /** Carries "are we inside a /* ... *&#47; comment" from one line to the next. */
    public static final class State {
        private boolean inBlockComment;

        public boolean inBlockComment() {
            return inBlockComment;
        }

        public void setInBlockComment(boolean value) {
            this.inBlockComment = value;
        }
    }

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "var", "record", "true", "false", "null");

    private JavaSyntax() {
    }

    /** Splits one line into coloured tokens. Adjacent tokens of the same kind are merged. */
    public static List<Token> tokenize(String line, State state) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = line.length();
        if (state.inBlockComment) {
            int end = line.indexOf("*/");
            if (end < 0) {
                add(tokens, line, Kind.COMMENT);
                return tokens;
            }
            add(tokens, line.substring(0, end + 2), Kind.COMMENT);
            state.inBlockComment = false;
            i = end + 2;
        }
        while (i < n) {
            char c = line.charAt(i);
            if (line.startsWith("//", i)) {
                add(tokens, line.substring(i), Kind.COMMENT);
                break;
            }
            if (line.startsWith("/*", i)) {
                int end = line.indexOf("*/", i + 2);
                if (end < 0) {
                    add(tokens, line.substring(i), Kind.COMMENT);
                    state.inBlockComment = true;
                    break;
                }
                add(tokens, line.substring(i, end + 2), Kind.COMMENT);
                i = end + 2;
            } else if (c == '"' || c == '\'') {
                int end = closingQuote(line, i, c);
                add(tokens, line.substring(i, end), Kind.STRING);
                i = end;
            } else if (c == '@' && i + 1 < n && Character.isJavaIdentifierStart(line.charAt(i + 1))) {
                int end = identifierEnd(line, i + 1);
                add(tokens, line.substring(i, end), Kind.ANNOTATION);
                i = end;
            } else if (Character.isDigit(c)) {
                int end = i;
                while (end < n && (Character.isLetterOrDigit(line.charAt(end))
                        || line.charAt(end) == '.' || line.charAt(end) == '_')) {
                    end++;
                }
                add(tokens, line.substring(i, end), Kind.NUMBER);
                i = end;
            } else if (Character.isJavaIdentifierStart(c)) {
                int end = identifierEnd(line, i);
                String word = line.substring(i, end);
                add(tokens, word, KEYWORDS.contains(word) ? Kind.KEYWORD : Kind.PLAIN);
                i = end;
            } else {
                add(tokens, String.valueOf(c), Kind.PLAIN);
                i++;
            }
        }
        return tokens;
    }

    private static int identifierEnd(String line, int from) {
        int end = from;
        while (end < line.length() && Character.isJavaIdentifierPart(line.charAt(end))) {
            end++;
        }
        return end;
    }

    /** Index just after the closing quote, or the end of the line for an unterminated literal. */
    private static int closingQuote(String line, int open, char quote) {
        int i = open + 1;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i + 1;
            }
            i++;
        }
        return line.length();
    }

    private static void add(List<Token> tokens, String text, Kind kind) {
        if (text.isEmpty()) {
            return;
        }
        if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).kind() == kind && kind == Kind.PLAIN) {
            Token last = tokens.remove(tokens.size() - 1);
            tokens.add(new Token(last.text() + text, kind));
            return;
        }
        tokens.add(new Token(text, kind));
    }
}
