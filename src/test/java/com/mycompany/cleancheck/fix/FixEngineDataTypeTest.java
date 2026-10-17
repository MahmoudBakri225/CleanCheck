package com.mycompany.cleancheck.fix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FixEngineDataTypeTest {

    @Test
    void fixesCodeWithoutChangingMultilineCommentsOrTextBlocks(@TempDir Path temporaryDirectory) throws IOException {
        String source = "class Example {\n"
                + "    /*\n"
                + "       string in this comment must stay lowercase\n"
                + "    */\n"
                + "    String description = \"\"\"\n"
                + "            string inside a text block\n"
                + "            \"\"\";\n"
                + "    string value = \"string literal\";\n"
                + "}\n";
        String expected = source.replace("    string value", "    String value");
        Path file = Files.writeString(temporaryDirectory.resolve("Example.java"), source);

        new FixEngine().applyFix(file, "Data Type", 8);

        assertEquals(expected, Files.readString(file));
    }

    @Test
    void fixAllChangesOnlyTypeTokensInCode(@TempDir Path temporaryDirectory) throws IOException {
        String source = "class Example { /* string comment */ String text = \"string literal\"; string value; }\n";
        String expected = "class Example { /* string comment */ String text = \"string literal\"; String value; }\n";
        Path file = Files.writeString(temporaryDirectory.resolve("Example.java"), source);

        new FixEngine().applyFixes(file);

        assertEquals(expected, Files.readString(file));
    }

    @Test
    void targetedFixPreservesLegacyLatin1BytesOutsideTheChangedToken(@TempDir Path temporaryDirectory)
            throws IOException {
        byte[] original = "// café\nclass Example { string value; }\n"
                .getBytes(StandardCharsets.ISO_8859_1);
        byte[] expected = "// café\nclass Example { String value; }\n"
                .getBytes(StandardCharsets.ISO_8859_1);
        Path file = temporaryDirectory.resolve("Example.java");
        Files.write(file, original);

        new FixEngine().applyFix(file, "Data Type", 2);

        assertArrayEquals(expected, Files.readAllBytes(file));
    }

    @Test
    void fixAllPreservesLegacyLatin1Encoding(@TempDir Path temporaryDirectory) throws IOException {
        byte[] original = "// café\nclass Example { string value; }\n"
                .getBytes(StandardCharsets.ISO_8859_1);
        byte[] expected = "// café\nclass Example { String value; }\n"
                .getBytes(StandardCharsets.ISO_8859_1);
        Path file = temporaryDirectory.resolve("Example.java");
        Files.write(file, original);

        new FixEngine().applyFixes(file);

        assertArrayEquals(expected, Files.readAllBytes(file));
    }
}
