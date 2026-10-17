package com.mycompany.cleancheck.risk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mycompany.cleancheck.analysis.Severity;
import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.application.ConsoleOutput;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskHotspotServiceTest {

    @Test
    void hotspotReportsUseTheActiveRunOutputStream() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8);
        Warning warning = new Warning("src/App.java", 7, "SQL Injection", Severity.CRITICAL,
                "Unsafe SQL", "Use parameters");

        ConsoleOutput.withStreams(stream, stream,
                () -> {
                    new RiskHotspotService().printRiskHotspots(List.of(warning));
                    return null;
                });

        String report = output.toString(StandardCharsets.UTF_8);
        assertTrue(report.contains("FILE HOTSPOTS"));
        assertTrue(report.contains("src/App.java"));
        assertTrue(report.contains("RULE HOTSPOTS"));
    }
}
