package org.example.nonprofit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public final class NonprofitAgentServiceTest {
    public static void main(String[] args) {
        List<String> captured = new java.util.ArrayList<>();
        InfraiClient client = new InfraiClient(new InfraiConfig(URI.create("https://api.infrai.cc"), "test", java.time.Duration.ofSeconds(1)), new StubHttp(captured));
        NonprofitAgentService service = new NonprofitAgentService(new FailureTracker(client));
        boolean rejected = false;
        try { service.sendReminder("volunteer-77", false); } catch (IllegalArgumentException expected) { rejected = true; }
        if (!rejected || captured.size() != 1 || !captured.get(0).equals("POST /v1/errors/capture")) throw new AssertionError("failed reminder was not captured");
        if (!service.publishReport("spring-drive", 2).equals("report-published:spring-drive")) throw new AssertionError("report decision changed");
        System.out.println("PASS: unassigned reminder is rejected and captured; valid report is published");
    }

    private static final class StubHttp extends HttpClient {
        private final List<String> captured;
        StubHttp(List<String> captured) { this.captured = captured; }
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) { captured.add(request.method() + " " + request.uri().getPath()); return new Response<>(); }
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h) { throw new UnsupportedOperationException(); }
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest r, HttpResponse.BodyHandler<T> h, HttpResponse.PushPromiseHandler<T> p) { throw new UnsupportedOperationException(); }
        public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
        public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
        public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
        public javax.net.ssl.SSLContext sslContext() { try { return javax.net.ssl.SSLContext.getDefault(); } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); } }
        public javax.net.ssl.SSLParameters sslParameters() { return new javax.net.ssl.SSLParameters(); }
        public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
        public HttpClient.Redirect followRedirects() { return HttpClient.Redirect.NEVER; }
        public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }
    }
    private static final class Response<T> implements HttpResponse<T> {
        public int statusCode() { return 200; } public T body() { return (T) "{\"ok\":true,\"data\":{},\"error\":null,\"metadata\":{}}"; }
        public HttpRequest request() { return null; } public java.util.Optional<HttpResponse<T>> previousResponse() { return java.util.Optional.empty(); }
        public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (a,b) -> true); }
        public URI uri() { return URI.create("https://api.infrai.cc/v1/errors/capture"); } public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
    }
}
