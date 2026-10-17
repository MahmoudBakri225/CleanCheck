package com.mycompany.cleancheck.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mycompany.cleancheck.application.AnalysisResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportOutputGuardTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void everyReportExporterRefusesToReplaceAnAnalyzedJavaFile() throws Exception {
        Path source = temporaryDirectory.resolve("KeepMe.java");
        String original = "class KeepMe { }\n";
        Files.writeString(source, original);
        AnalysisResult analysis = new AnalysisResult(
                temporaryDirectory, List.of(source), List.of(), 1);

        assertThrows(IOException.class, () -> JsonReportExporter.write(analysis, source));
        assertThrows(IOException.class, () -> CsvReportExporter.write(analysis, source));
        assertThrows(IOException.class, () -> SarifReportExporter.write(analysis, source));
        assertThrows(IOException.class,
                () -> new HtmlReportGenerator().generateStandalone(analysis, source));

        assertEquals(original, Files.readString(source));
    }

    @Test
    void rejectsAReportDestinationThatAliasesTheSourceThroughAHardLink() throws Exception {
        Path source = temporaryDirectory.resolve("KeepMe.java");
        Path alias = temporaryDirectory.resolve("report.json");
        Files.writeString(source, "class KeepMe { }\n");
        Files.createLink(alias, source);
        AnalysisResult analysis = new AnalysisResult(
                temporaryDirectory, List.of(source), List.of(), 1);

        assertThrows(IOException.class, () -> JsonReportExporter.write(analysis, alias));

        assertEquals("class KeepMe { }\n", Files.readString(source));
    }
}
