package com.mycompany.cleancheck.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IgnoreDirectivesTest {

    @Test
    void stringLiteralThatLooksLikeDirectiveDoesNotSuppressFindings() {
        String source = """
                class Example {
                    String text = "// cleancheck:ignore-file";
                    void run() { System.out.println(text); }
                }
                """;

        long systemOutFindings = new RuleEngine().analyze("Example.java", source).stream()
                .filter(warning -> warning.getRuleName().equals("System Out"))
                .count();

        assertEquals(1, systemOutFindings);
    }

    @Test
    void realLineCommentDirectiveStillSuppressesFinding() {
        String source = """
                class Example {
                    // cleancheck:ignore System Out
                    void run() { System.out.println("text"); }
                }
                """;

        long systemOutFindings = new RuleEngine().analyze("Example.java", source).stream()
                .filter(warning -> warning.getRuleName().equals("System Out"))
                .count();

        assertEquals(0, systemOutFindings);
    }
}
