package com.mycompany.cleancheck.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class ConsoleOutputTest {

    @Test
    void embeddedAnalysisUsesCallerStreamsWithoutChangingSystemOut() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        PrintStream originalOut = System.out;

        CleanCheck.Outcome outcome = CleanCheck.execute(
                new String[]{"missing-project-for-console-test", "--analyze-only"}, stream, stream);

        assertEquals(CleanCheck.EXIT_ERROR, outcome.exitCode());
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("ANALYSIS_STATUS : FAILED"));
        assertSame(originalOut, System.out);
        assertSame(originalOut, ConsoleOutput.out());
    }

    @Test
    void concurrentRunsKeepTheirOutputStreamsIsolated() throws InterruptedException {
        ByteArrayOutputStream firstOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream secondOutput = new ByteArrayOutputStream();
        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        Thread first = writer("first", firstOutput, bothReady, release);
        Thread second = writer("second", secondOutput, bothReady, release);
        first.start();
        second.start();
        bothReady.await();
        release.countDown();
        first.join();
        second.join();

        assertEquals("first" + System.lineSeparator(), firstOutput.toString(StandardCharsets.UTF_8));
        assertEquals("second" + System.lineSeparator(), secondOutput.toString(StandardCharsets.UTF_8));
    }

    private static Thread writer(String value, ByteArrayOutputStream output,
            CountDownLatch ready, CountDownLatch release) {
        return new Thread(() -> {
            try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
                ConsoleOutput.withStreams(stream, stream, () -> {
                    ready.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    }
                    ConsoleOutput.out().println(value);
                    return null;
                });
            }
        });
    }
}
