package com.voiceflow.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.voiceflow.config.AppProperties;
import com.voiceflow.exception.ApiException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Uses a fake HTTP server; no real speech API is contacted. */
class OpenAiCompatibleProviderTest {

    private static final String URL = "https://speech.example.test/v1/audio/transcriptions";
    private static final String SECRET = "sk-super-secret-key";
    private final AudioPayload audio = new AudioPayload(new byte[] {1, 2, 3}, "recording.webm", "audio/webm");

    private MockRestServiceServer server;

    private OpenAiCompatibleProvider provider(String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new OpenAiCompatibleProvider(
                new AppProperties.Speech(URL, "whisper-1", apiKey, Duration.ofSeconds(5)), builder);
    }

    @Test
    void sendsMultipartRequestWithBearerKeyAndReturnsText() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + SECRET))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andRespond(withSuccess("{\"text\":\" नमस्ते \"}", MediaType.APPLICATION_JSON));

        TranscriptionResult result = provider.transcribe(audio, "hi");

        assertThat(result.text()).isEqualTo("नमस्ते");
        assertThat(result.language()).isEqualTo("hi");
        server.verify();
    }

    @Test
    void missingApiKeyGivesSetupErrorWithoutCallingProvider() {
        var provider = provider("");
        assertThat(provider.isConfigured()).isFalse();
        assertThatThrownBy(() -> provider.transcribe(audio, "en"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SPEECH_API_KEY_MISSING");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
        server.verify(); // no request was expected or made
    }

    @Test
    void authFailureIsMappedAndDoesNotLeakTheKey() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"bad key " + SECRET + "\"}"));
        assertThatThrownBy(() -> provider.transcribe(audio, "en"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SPEECH_API_AUTH_FAILED");
                    assertThat(e.getMessage()).doesNotContain(SECRET).doesNotContain(URL);
                });
    }

    @Test
    void rateLimitIsMapped() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> provider.transcribe(audio, "en"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("SPEECH_API_RATE_LIMIT");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
    }

    @Test
    void serverErrorIsMappedToProviderError() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> provider.transcribe(audio, "en"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("TRANSCRIPTION_PROVIDER_ERROR");
                    assertThat(e.getMessage()).doesNotContain(SECRET);
                });
    }

    @Test
    void malformedJsonBodyIsHandled() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"unexpected\":true}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> provider.transcribe(audio, "en"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getCode()).isEqualTo("TRANSCRIPTION_PROVIDER_ERROR"));
    }

    @Test
    void notJsonBodyIsHandled() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL)).andRespond(withSuccess("<html>oops</html>", MediaType.TEXT_HTML));
        assertThatThrownBy(() -> provider.transcribe(audio, "en")).isInstanceOf(ApiException.class);
    }

    @Test
    void timeoutIsMapped() {
        var provider = provider(SECRET);
        server.expect(requestTo(URL)).andRespond(request -> {
            throw new SocketTimeoutException("Read timed out");
        });
        assertThatThrownBy(() -> provider.transcribe(audio, "en"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("TRANSCRIPTION_TIMEOUT");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
                });
    }
}
