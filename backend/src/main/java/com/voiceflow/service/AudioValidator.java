package com.voiceflow.service;

import com.voiceflow.config.AppProperties;
import com.voiceflow.exception.ApiException;
import com.voiceflow.model.AudioFormat;
import com.voiceflow.provider.AudioPayload;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Checks an upload before it is sent anywhere: size, declared type, AND the real file
 * signature (first bytes), because the browser-provided type alone can be wrong or forged.
 * Uploaded bytes are held in memory only; nothing is written to disk.
 */
@Component
public class AudioValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio/webm", "audio/ogg", "audio/wav", "audio/x-wav", "audio/wave", "audio/mp4",
            "audio/x-m4a", "audio/m4a", "audio/mpeg", "audio/mp3", "audio/flac", "audio/x-flac");

    private final AppProperties.Audio limits;

    public AudioValidator(AppProperties props) {
        this.limits = props.audio();
    }

    public AudioPayload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw bad(HttpStatus.BAD_REQUEST, "AUDIO_EMPTY", "The recording is empty. Please record again.");
        }
        if (file.getSize() > limits.maxBytes()) {
            throw bad(HttpStatus.PAYLOAD_TOO_LARGE, "AUDIO_TOO_LARGE",
                    "The recording is larger than " + limits.maxMb() + " MB. Record a shorter clip.");
        }
        if (file.getSize() < limits.minBytes()) {
            throw bad(HttpStatus.BAD_REQUEST, "AUDIO_TOO_SHORT", "The recording is too short. Hold on a little longer and speak.");
        }
        if (!ALLOWED_TYPES.contains(baseType(file.getContentType()))) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AUDIO_UNSUPPORTED_FORMAT",
                    "This audio type is not supported. Use WebM, Ogg, WAV, MP3, M4A/MP4 or FLAC.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw bad(HttpStatus.BAD_REQUEST, "AUDIO_UNREADABLE", "The uploaded audio could not be read.");
        }
        AudioFormat format = detect(data);
        if (format == null) {
            throw bad(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AUDIO_UNSUPPORTED_FORMAT",
                    "The file does not look like a supported audio recording.");
        }
        return new AudioPayload(data, "recording." + format.extension(), format.contentType());
    }

    static String baseType(String contentType) {
        if (contentType == null) return "";
        int semi = contentType.indexOf(';');
        String base = semi >= 0 ? contentType.substring(0, semi) : contentType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    /** Recognises the audio container from the first bytes ("magic numbers"). */
    static AudioFormat detect(byte[] d) {
        if (d.length < 12) return null;
        if ((d[0] & 0xFF) == 0x1A && (d[1] & 0xFF) == 0x45 && (d[2] & 0xFF) == 0xDF && (d[3] & 0xFF) == 0xA3) return AudioFormat.WEBM;
        if (ascii(d, 0, "OggS")) return AudioFormat.OGG;
        if (ascii(d, 0, "RIFF") && ascii(d, 8, "WAVE")) return AudioFormat.WAV;
        if (ascii(d, 0, "fLaC")) return AudioFormat.FLAC;
        if (ascii(d, 4, "ftyp")) return AudioFormat.MP4;
        if (ascii(d, 0, "ID3") || ((d[0] & 0xFF) == 0xFF && (d[1] & 0xE0) == 0xE0)) return AudioFormat.MP3;
        return null;
    }

    private static boolean ascii(byte[] d, int offset, String s) {
        for (int i = 0; i < s.length(); i++) {
            if (d[offset + i] != (byte) s.charAt(i)) return false;
        }
        return true;
    }

    private static ApiException bad(HttpStatus status, String code, String message) {
        return new ApiException(status, code, message);
    }
}
