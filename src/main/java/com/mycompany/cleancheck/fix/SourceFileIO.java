package com.mycompany.cleancheck.fix;

import com.mycompany.cleancheck.analysis.SourceTextIO;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

/** Reads and rewrites source using the same UTF-8 / ISO-8859-1 fallback as the project scanner. */
final class SourceFileIO {

    record Source(String text, Charset charset) {
    }

    private SourceFileIO() {
    }

    static Source read(Path path) throws IOException {
        SourceTextIO.Decoded decoded = SourceTextIO.read(path);
        return new Source(decoded.text(), decoded.charset());
    }

    static void write(Path path, String text, Charset charset) throws IOException {
        SourceTextIO.write(path, text, charset);
    }
}
