package com.mycompany.cleancheck.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebServerTest {

    @Test
    void servesOnlyExactGetRoutesAndSetsSafetyHeaders(@TempDir Path temporaryDirectory) throws Exception {
        Path project = Files.createDirectory(temporaryDirectory.resolve("project"));
        Files.writeString(project.resolve("App.java"), "class App { void run() { System.out.println(\"ok\"); } }\n");

        try (WebServer.RunningServer running = WebServer.start(project, 0);
                HttpClient client = HttpClient.newHttpClient()) {
            URI base = URI.create("http://127.0.0.1:" + running.server().getAddress().getPort());

            HttpResponse<String> analysis = client.send(
                    HttpRequest.newBuilder(base.resolve("/api/analyze")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, analysis.statusCode());
            assertTrue(analysis.headers().firstValue("X-Content-Type-Options").orElse("").equals("nosniff"));
            assertTrue(analysis.headers().firstValue("Cache-Control").orElse("").equals("no-store"));

            HttpResponse<String> nested = client.send(
                    HttpRequest.newBuilder(base.resolve("/api/analyze/extra")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(404, nested.statusCode());

            HttpResponse<String> post = client.send(
                    HttpRequest.newBuilder(base.resolve("/api/analyze"))
                            .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(405, post.statusCode());
            assertEquals("GET", post.headers().firstValue("Allow").orElse(""));
        }
    }

    @Test
    void htmlEscaperEncodesAllMarkupDelimiters() {
        assertEquals("&lt;script&gt;&amp;&quot;&#39;", WebServer.escapeHtml("<script>&\"'"));
    }
}
