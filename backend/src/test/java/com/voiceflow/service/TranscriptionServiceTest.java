package com.voiceflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voiceflow.config.AppProperties;
import com.voiceflow.dto.HistoryItemResponse;
import com.voiceflow.dto.TranscriptionResponse;
import com.voiceflow.exception.ApiException;
import com.voiceflow.provider.AudioPayload;
import com.voiceflow.provider.SpeechToTextProvider;
import com.voiceflow.provider.TranscriptionResult;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockMultipartFile;

/** The speech provider is mocked here. Real provider calls only happen when the app runs. */
class TranscriptionServiceTest {

    private final SpeechToTextProvider provider = mock(SpeechToTextProvider.class);
    private final HistoryService history = mock(HistoryService.class);
    private TranscriptionService service;

    @NonNull private final MockMultipartFile audio =
            new MockMultipartFile("audio", "r.webm", "audio/webm", AudioValidatorTest.webm(5000));

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Speech("http://x", "m", "k", java.util.Objects.requireNonNull(Duration.ofSeconds(5))),
                new AppProperties.Audio(1, 1500),
                new AppProperties.Cleanup(false),
                new AppProperties.Cors("http://localhost:5500"));
        service = new TranscriptionService(
                new AudioValidator(props), provider, new TextCleanupService(), history, props);
    }

    @Test
    void returnsProviderTextAndPassesLanguageCode() {
        when(provider.transcribe(any(AudioPayload.class), eq("hi"))).thenReturn(new TranscriptionResult("नमस्ते दुनिया", "hi"));
        TranscriptionResponse r = service.transcribe(audio, "hi", false, false);
        assertThat(r.originalTranscript()).isEqualTo("नमस्ते दुनिया");
        assertThat(r.cleanedTranscript()).isNull();
        assertThat(r.language()).isEqualTo("hi");
        assertThat(r.status()).isEqualTo("COMPLETED");
        assertThat(r.id()).isNull();
        verify(history, never()).save(any(), any(), any());
    }

    @Test
    void treatsBlankOrAutoLanguageAsAutoDetection() {
        when(provider.transcribe(any(AudioPayload.class), eq("en"))).thenReturn(new TranscriptionResult("hello there", "en"));

        TranscriptionResponse autoResult = service.transcribe(audio, "auto", false, false);
        TranscriptionResponse blankResult = service.transcribe(audio, " ", false, false);

        assertThat(autoResult.language()).isEqualTo("en");
        assertThat(blankResult.language()).isEqualTo("en");
        verify(provider, times(2)).transcribe(any(AudioPayload.class), eq("en"));
    }

    @Test
    void cleansUpWhenRequestedAndKeepsOriginal() {
        when(provider.transcribe(any(), eq("en"))).thenReturn(new TranscriptionResult("hello world", "en"));
        TranscriptionResponse r = service.transcribe(audio, "en", true, false);
        assertThat(r.originalTranscript()).isEqualTo("hello world");
        assertThat(r.cleanedTranscript()).isEqualTo("Hello world.");
        assertThat(r.cleanupOperations()).isNotEmpty();
    }

    @Test
    void savesToHistoryWhenRequested() {
        when(provider.transcribe(any(), eq("mr"))).thenReturn(new TranscriptionResult("आज छान दिवस आहे", "mr"));
        when(history.save(eq("mr"), any(), any())).thenReturn(
                new HistoryItemResponse(42, "mr", "आज छान दिवस आहे", "आज छान दिवस आहे", Instant.now(), Instant.now()));
        TranscriptionResponse r = service.transcribe(audio, "mr", false, true);
        assertThat(r.id()).isEqualTo(42L);
    }

    @Test
    void reportsNoSpeechWhenProviderReturnsBlankText() {
        when(provider.transcribe(any(), eq("en"))).thenReturn(new TranscriptionResult("   ", "en"));
        TranscriptionResponse r = service.transcribe(audio, "en", true, true);
        assertThat(r.status()).isEqualTo("NO_SPEECH_DETECTED");
        verify(history, never()).save(any(), any(), any());
    }

    @Test
    void rejectsUnknownLanguageBeforeCallingProvider() {
        assertThatThrownBy(() -> service.transcribe(audio, "fr", false, false))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_LANGUAGE"));
        verify(provider, never()).transcribe(any(), any());
    }

    @Test
    void rejectsInvalidAudioBeforeCallingProvider() {
        var bad = new MockMultipartFile("audio", "r.webm", "audio/webm", new byte[10]);
        assertThatThrownBy(() -> service.transcribe(bad, "en", false, false)).isInstanceOf(ApiException.class);
        verify(provider, never()).transcribe(any(), any());
    }

    @Test
    void propagatesProviderFailures() {
        when(provider.transcribe(any(), any())).thenThrow(
                new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SPEECH_API_RATE_LIMIT", "slow down"));
        assertThatThrownBy(() -> service.transcribe(audio, "en", false, false))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("SPEECH_API_RATE_LIMIT"));
    }
}
