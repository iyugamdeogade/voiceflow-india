package com.voiceflow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the "voiceflow.*" section of application.yml. */
@ConfigurationProperties(prefix = "voiceflow")
public record AppProperties(Speech speech, Audio audio, Cleanup cleanup, Cors cors) {

    public record Speech(String endpoint, String model, String apiKey, Duration timeout) {
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Audio(int maxMb, int minBytes) {
        public long maxBytes() {
            return (long) maxMb * 1024 * 1024;
        }
    }

    public record Cleanup(boolean defaultRemoveFillers) {}

    public record Cors(String allowedOrigins) {}
}
