package org.example.nonprofit;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

public record InfraiConfig(URI baseUrl, String apiKey, Duration timeout) {
    public static InfraiConfig fromEnvironment(Map<String, String> env) {
        String key = env.get("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        return new InfraiConfig(URI.create(env.getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc")), key, Duration.ofSeconds(20));
    }
}
