package com.mycompany.cleancheck.fix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads and rewrites source using the same UTF-8 / ISO-8859-1 fallback as the project scanner. */
final class SourceFileIO {

    record Source(String text, Charset charset) {
    }

    private SourceFileIO() {
    }

    static Source read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return new Source(text, StandardCharsets.UTF_8);
        } catch (CharacterCodingException invalidUtf8) {
            return new Source(new String(bytes, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
        }
    }

    static void write(Path path, String text, Charset charset) throws IOException {
        ByteBuffer encoded = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        Files.write(path, bytes);
    }
}
