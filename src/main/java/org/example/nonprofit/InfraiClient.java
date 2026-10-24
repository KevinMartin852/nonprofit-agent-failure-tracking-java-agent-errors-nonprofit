package org.example.nonprofit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ERROR = Pattern.compile("\\\"error\\\"\\s*:\\s*(\\{.*?\\}|null)");
    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiClient(InfraiConfig config) { this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build()); }
    InfraiClient(InfraiConfig config, HttpClient http) { this.config = config; this.http = http; }

    public void capture(String exception) {
        String body = "{\"exception\":" + quote(exception) + "}";
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<String> response = send("POST", "/v1/errors/capture", body);
            Envelope envelope = Envelope.parse(response.body());
            if (response.statusCode() == 429 && attempt < 3) {
                pause(response, attempt);
                continue;
            }
            if (!envelope.ok()) throw new InfraiRejected(envelope.error(), response.statusCode());
            if (response.statusCode() >= 500) throw new IllegalStateException("Infrai transport status " + response.statusCode());
            return;
        }
        throw new IllegalStateException("capture retry budget exhausted");
    }

    private HttpResponse<String> send(String method, String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(config.baseUrl().resolve(path)).timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey()).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Infrai request transport failed", error);
        }
    }

    private static void pause(HttpResponse<String> response, int attempt) {
        long millis = response.headers().firstValue("Retry-After").map(Long::parseLong).map(value -> value * 1000)
                .orElse(200L * (1L << attempt));
        try { Thread.sleep(Math.min(millis, 5000)); } catch (InterruptedException error) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("retry interrupted", error);
        }
    }

    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""; }

    record Envelope(boolean ok, String error) {
        static Envelope parse(String json) {
            Matcher ok = OK.matcher(json);
            if (!ok.find()) throw new IllegalStateException("Infrai response omitted ok");
            Matcher error = ERROR.matcher(json);
            return new Envelope(Boolean.parseBoolean(ok.group(1)), error.find() ? error.group(1) : "null");
        }
    }

    public static final class InfraiRejected extends RuntimeException {
        public InfraiRejected(String error, int status) { super("Infrai rejected request: " + error + " (HTTP " + status + ")"); }
    }
}
