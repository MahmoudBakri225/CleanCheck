package com.mycompany.cleancheck.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class JavaAstNormalizationTest {

    @Test
    void reusesOneParsedTreeForAllRulesInAFileAnalysis() {
        String source = "class Example { void run() {} }";
        JavaAst.beginFile(source);
        try {
            var first = JavaAst.parse(source).orElseThrow();
            var second = JavaAst.parse(source).orElseThrow();

            assertSame(first, second);
        } finally {
            JavaAst.endFile();
        }
        assertNotSame(JavaAst.parse(source).orElseThrow(), JavaAst.parse(source).orElseThrow());
    }

    @Test
    void parserFallbackLeavesLiteralAndCommentTextUntouched() {
        String source = """
                class Example {
                    void run() {
                        string label = "Int";
                    }
                    // Int in this comment is ordinary text.
                }
                """;

        var parsed = JavaAst.parse(source);

        assertTrue(parsed.isPresent());
        assertEquals("Int", parsed.get().findFirst(StringLiteralExpr.class).orElseThrow().asString());
        assertTrue(parsed.get().getAllComments().stream()
                .anyMatch(comment -> comment.getContent().contains("Int in this comment")));
    }

    @Test
    void parsesJava25CompactSourceFiles() {
        String source = "void main() { IO.println(\"Hello\"); }";

        assertTrue(JavaAst.parse(source).isPresent());
    }

    @Test
    void parserFallbackPreservesTextBlockContents() {
        String source = "class Example {\n"
                + "    void run() {\n"
                + "        string text = \"\"\"\n"
                + "            Int is text, not a type.\n"
                + "            \"\"\";\n"
                + "    }\n"
                + "}\n";

        var parsed = JavaAst.parse(source);

        assertTrue(parsed.isPresent());
        assertTrue(parsed.get().findFirst(TextBlockLiteralExpr.class).orElseThrow().asString().contains("Int is text"));
    }

    @Test
    void parserFallbackPreservesTextAfterEscapedTripleQuotes() {
        String source = "class Example {\n"
                + "    void run() {\n"
                + "        string text = \"\"\"\n"
                + "            Escaped delimiter: \\\"\"\" stays inside the text block.\n"
                + "            Int is still text here.\n"
                + "            \"\"\";\n"
                + "    }\n"
                + "}\n";

        var parsed = JavaAst.parse(source);

        assertTrue(parsed.isPresent());
        assertTrue(parsed.get().findFirst(TextBlockLiteralExpr.class).orElseThrow()
                .asString().contains("Int is still text here."));
    }

    @Test
    void parsesIndependentSourcesConcurrentlyWithoutSharingParserState() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int index = 0; index < 120; index++) {
                int sourceId = index;
                tasks.add(() -> {
                    String className = "Concurrent" + sourceId;
                    String source = "class " + className + " { int value() { return " + sourceId + "; } }";
                    return JavaAst.parse(source)
                            .map(unit -> unit.getType(0).getNameAsString())
                            .map(name -> name.equals(className))
                            .orElse(false);
                });
            }

            List<Future<Boolean>> results = workers.invokeAll(tasks);
            for (Future<Boolean> result : results) {
                assertTrue(result.get());
            }
        } finally {
            workers.shutdownNow();
        }
    }
}


