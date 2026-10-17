package com.mycompany.cleancheck.application;

import com.mycompany.cleancheck.analysis.Warning;
import com.mycompany.cleancheck.report.HtmlReportGenerator;
import com.mycompany.cleancheck.report.JsonReportExporter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal local web UI bound to 127.0.0.1 only (not reachable from other machines).
 * Open http://localhost:8787/
 * Serves JSON analysis and a simple HTML dashboard without changing the desktop app.
 */
public final class WebServer {

    private WebServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = 8787;
        Path defaultProject = Path.of("sample-projects/problematic-project").toAbsolutePath().normalize();
        if (args.length > 0) {
            defaultProject = Path.of(args[0]).toAbsolutePath().normalize();
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        RunningServer running = start(defaultProject, port);
        Runtime.getRuntime().addShutdownHook(new Thread(running::close, "cleancheck-web-shutdown"));
        int actualPort = running.server().getAddress().getPort();
        System.out.println("CleanCheck Web UI: http://127.0.0.1:" + actualPort + "/");
        System.out.println("Project: " + defaultProject);
        System.out.println("API:     http://127.0.0.1:" + actualPort + "/api/analyze");
        System.out.println("Report:  http://127.0.0.1:" + actualPort + "/report");
    }

    static RunningServer start(Path project, int port) throws IOException {
        Path normalizedProject = project.toAbsolutePath().normalize();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        AtomicInteger workerNumber = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4, 4, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(32),
                task -> {
                    Thread worker = new Thread(task, "cleancheck-web-" + workerNumber.incrementAndGet());
                    worker.setDaemon(true);
                    return worker;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        server.setExecutor(executor);
        server.createContext("/", exchange -> route(exchange, normalizedProject));
        try {
            server.start();
            return new RunningServer(server, executor);
        } catch (RuntimeException failure) {
            executor.shutdownNow();
            server.stop(0);
            throw failure;
        }
    }

    record RunningServer(HttpServer server, ThreadPoolExecutor executor) implements AutoCloseable {
        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    private static void route(HttpExchange exchange, Path project) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.getResponseHeaders().set("Allow", "GET");
            respond(exchange, 405, "text/plain; charset=utf-8", "Method not allowed.");
            return;
        }
        switch (exchange.getRequestURI().getPath()) {
            case "/" -> handleHome(exchange, project);
            case "/api/analyze" -> handleAnalyze(exchange, project);
            case "/report" -> handleReport(exchange, project);
            default -> respond(exchange, 404, "text/plain; charset=utf-8", "Not found.");
        }
    }

    private static void handleHome(HttpExchange exchange, Path project) throws IOException {
        String html = """
                <!DOCTYPE html>
                <html lang="en"><head>
                <meta charset="utf-8"/>
                <title>CleanCheck Web</title>
                <style>
                body{font-family:Segoe UI,system-ui,sans-serif;margin:0;background:#F4F7FE;color:#0F172A}
                header{background:#0F172A;color:#fff;padding:20px 28px}
                main{max-width:960px;margin:24px auto;padding:0 16px}
                .card{background:#fff;border-radius:16px;padding:20px;margin-bottom:16px;
                box-shadow:0 8px 24px rgba(15,23,42,.08)}
                a.btn{display:inline-block;background:#2563EB;color:#fff;text-decoration:none;
                padding:12px 18px;border-radius:12px;font-weight:600;margin-right:8px}
                code{background:#E2E8F0;padding:2px 6px;border-radius:6px}
                </style></head><body>
                <header><h1>CleanCheck Web UI</h1>
                <p>Local read-only view of the same analyzer used by the desktop app.</p></header>
                <main>
                <div class="card">
                <h2>Project</h2>
                <p><code>%s</code></p>
                <p>
                <a class="btn" href="/api/analyze">JSON analysis</a>
                <a class="btn" href="/report">HTML report</a>
                </p>
                </div>
                <div class="card">
                <h2>How to run</h2>
                <p><code>mvn -q exec:java -Dexec.mainClass=com.mycompany.cleancheck.application.WebServer -Dexec.args="sample-projects/problematic-project 8787"</code></p>
                </div>
                </main></body></html>
                """.formatted(escapeHtml(project.toString()));
        respond(exchange, 200, "text/html; charset=utf-8", html);
    }

    private static void handleAnalyze(HttpExchange exchange, Path project) throws IOException {
        try {
            AnalysisResult result = new ProjectAnalyzer().analyze(project);
            String json = JsonReportExporter.toJson(result);
            respond(exchange, 200, "application/json; charset=utf-8", json);
        } catch (Exception ex) {
            respond(exchange, 500, "application/json; charset=utf-8",
                    "{\"error\":" + quote(ex.getMessage()) + "}");
        }
    }

    private static void handleReport(HttpExchange exchange, Path project) throws IOException {
        Path temporaryReport = null;
        try {
            AnalysisResult result = new ProjectAnalyzer().analyze(project);
            temporaryReport = Files.createTempFile("cleancheck-web-report-", ".html");
            String path = new HtmlReportGenerator().generateStandalone(result, temporaryReport);
            String html = Files.readString(Path.of(path));
            respond(exchange, 200, "text/html; charset=utf-8", html);
        } catch (Exception ex) {
            respond(exchange, 500, "text/plain; charset=utf-8", "Report failed: " + ex.getMessage());
        } finally {
            if (temporaryReport != null) {
                Files.deleteIfExists(temporaryReport);
            }
        }
    }

    private static void respond(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String quote(String s) {
        return JsonReportExporter.quote(s);
    }

    static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
