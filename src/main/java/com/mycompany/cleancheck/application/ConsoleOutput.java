package com.mycompany.cleancheck.application;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

/** Provides per-run console streams without replacing process-wide System streams. */
public final class ConsoleOutput {

    private record Streams(PrintStream out, PrintStream err) {
    }

    private static final ThreadLocal<Streams> ACTIVE = new ThreadLocal<>();

    private ConsoleOutput() {
    }

    public static PrintStream out() {
        Streams streams = ACTIVE.get();
        return streams == null ? System.out : streams.out();
    }

    public static PrintStream err() {
        Streams streams = ACTIVE.get();
        return streams == null ? System.err : streams.err();
    }

    public static <T> T withStreams(PrintStream out, PrintStream err, Supplier<T> action) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(err, "err");
        Objects.requireNonNull(action, "action");

        Streams previous = ACTIVE.get();
        ACTIVE.set(new Streams(out, err));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ACTIVE.remove();
            } else {
                ACTIVE.set(previous);
            }
        }
    }
}
