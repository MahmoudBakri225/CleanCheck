package com.mycompany.cleancheck.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class DuplicateCodeRuleTest {

    private final DuplicateCodeRule rule = new DuplicateCodeRule(3);

    @Test
    void doesNotTreatDifferentUrlLiteralsAsDuplicateCode() {
        String source = """
                class Example {
                    void first() {
                        String url = "https://alpha.example/path";
                        int retries = 3;
                        System.out.println(url);
                    }
                    void second() {
                        String url = "https://beta.example/path";
                        int retries = 3;
                        System.out.println(url);
                    }
                }
                """;

        assertEquals(0, rule.analyze(Map.of("Example.java", source)).size());
    }

    @Test
    void ignoresCommentsButKeepsTextBlockContentInTheFingerprint() {
        String source = "class Example {\n"
                + "    void first() {\n"
                + "        String query = \"\"\"\nselect * from alpha;\n\"\"\";\n"
                + "        int retries = 3;\n"
                + "        System.out.println(query);\n"
                + "    }\n"
                + "    void second() {\n"
                + "        String query = \"\"\"\nselect * from beta;\n\"\"\";\n"
                + "        int retries = 3; // comment should not affect the fingerprint\n"
                + "        System.out.println(query);\n"
                + "    }\n"
                + "}\n";

        assertEquals(0, rule.analyze(Map.of("Example.java", source)).size());
    }

    @Test
    void preservesWhitespaceInsideStringLiteralsWhenBuildingFingerprints() {
        String source = """
                class Example {
                    void first() {
                        String value = "a  b";
                        int retries = 3;
                        System.out.println(value);
                    }
                    void second() {
                        String value = "a b";
                        int retries = 3;
                        System.out.println(value);
                    }
                }
                """;

        assertEquals(0, rule.analyze(Map.of("Example.java", source)).size());
    }

    @Test
    void preservesWhitespaceInsideTextBlocksWhenBuildingFingerprints() {
        String source = "class Example {\n"
                + "    void first() {\n"
                + "        String value = \"\"\"\nalpha  beta\n\"\"\";\n"
                + "        int retries = 3;\n"
                + "        System.out.println(value);\n"
                + "    }\n"
                + "    void second() {\n"
                + "        String value = \"\"\"\nbeta  alpha\n\"\"\";\n"
                + "        int retries = 3;\n"
                + "        System.out.println(value);\n"
                + "    }\n"
                + "}\n";

        assertEquals(0, rule.analyze(Map.of("Example.java", source)).size());
    }

    @Test
    void reportsIdenticalBodiesWhenOnlyCommentsDiffer() {
        String source = """
                class Example {
                    void first() {
                        int retries = 3; // first comment
                        retries++;
                        System.out.println(retries);
                    }
                    void second() {
                        int retries = 3; /* another comment */
                        retries++;
                        System.out.println(retries);
                    }
                }
                """;

        assertEquals(1, rule.analyze(Map.of("Example.java", source)).size());
    }
}
