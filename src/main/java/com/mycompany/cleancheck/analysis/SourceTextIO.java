package com.mycompany.cleancheck.analysis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared source decoding and encoding so scanning and fixes preserve the same charset. */
public final class SourceTextIO {

    public record Decoded(String text, Charset charset) {
    }

    private SourceTextIO() {
    }

    public static Decoded read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            return new Decoded(text, StandardCharsets.UTF_8);
        } catch (CharacterCodingException invalidUtf8) {
            return new Decoded(new String(bytes, StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
        }
    }

    public static void write(Path path, String text, Charset charset) throws IOException {
        ByteBuffer encoded = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        Files.write(path, bytes);
    }
}
