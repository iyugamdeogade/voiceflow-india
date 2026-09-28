package com.voiceflow.config;

import java.time.Duration;
import org.springframework.lang.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the "voiceflow.*" section of application.yml. */
@ConfigurationProperties(prefix = "voiceflow")
public record AppProperties(@NonNull Speech speech, @NonNull Audio audio, @NonNull Cleanup cleanup, @NonNull Cors cors) {

    public record Speech(@NonNull String endpoint, @NonNull String model, @NonNull String apiKey, @NonNull Duration timeout) {
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

    public record Cors(@NonNull String allowedOrigins) {}
}
