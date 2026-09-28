package com.voiceflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.voiceflow.config.AppProperties;
import com.voiceflow.exception.ApiException;
import com.voiceflow.provider.AudioPayload;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AudioValidatorTest {

    private final AudioValidator validator = new AudioValidator(new AppProperties(
            new AppProperties.Speech("http://x", "m", "k", java.time.Duration.ofSeconds(5)),
            new AppProperties.Audio(1, 1500),
            new AppProperties.Cleanup(false),
            new AppProperties.Cors("http://localhost:5500")));

    static byte[] webm(int size) {
        byte[] b = new byte[size];
        b[0] = 0x1A; b[1] = 0x45; b[2] = (byte) 0xDF; b[3] = (byte) 0xA3;
        return b;
    }

    private static byte[] wav(int size) {
        byte[] b = new byte[size];
        System.arraycopy("RIFF".getBytes(), 0, b, 0, 4);
        System.arraycopy("WAVE".getBytes(), 0, b, 8, 4);
        return b;
    }

    @Test
    void acceptsWebmWithCodecParameterInContentType() {
        var file = new MockMultipartFile("audio", "a.webm", "audio/webm;codecs=opus", webm(5000));
        AudioPayload p = validator.validate(file);
        assertThat(p.filename()).isEqualTo("recording.webm");
        assertThat(p.contentType()).isEqualTo("audio/webm");
        assertThat(p.data()).hasSize(5000);
    }

    @Test
    void acceptsWav() {
        var p = validator.validate(new MockMultipartFile("audio", "a.wav", "audio/wav", wav(4000)));
        assertThat(p.filename()).isEqualTo("recording.wav");
    }

    @Test
    void rejectsEmptyFile() {
        var file = new MockMultipartFile("audio", "a.webm", "audio/webm", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AUDIO_EMPTY"));
    }

    @Test
    void rejectsTooShortFile() {
        var file = new MockMultipartFile("audio", "a.webm", "audio/webm", webm(200));
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AUDIO_TOO_SHORT"));
    }

    @Test
    void rejectsFileOverSizeLimit() {
        var file = new MockMultipartFile("audio", "a.webm", "audio/webm", webm(1024 * 1024 + 1));
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AUDIO_TOO_LARGE"));
    }

    @Test
    void rejectsDisallowedContentType() {
        var file = new MockMultipartFile("audio", "a.txt", "text/plain", webm(3000));
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AUDIO_UNSUPPORTED_FORMAT"));
    }

    @Test
    void rejectsFileWhoseBytesAreNotAudioEvenIfTypeSaysAudio() {
        byte[] fake = new byte[3000];
        Arrays.fill(fake, (byte) 'A');
        var file = new MockMultipartFile("audio", "a.webm", "audio/webm", fake);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("AUDIO_UNSUPPORTED_FORMAT"));
    }

    @Test
    void rejectsMissingContentType() {
        var file = new MockMultipartFile("audio", "a.webm", null, webm(3000));
        assertThatThrownBy(() -> validator.validate(file)).isInstanceOf(ApiException.class);
    }
}
