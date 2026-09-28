package com.voiceflow.model;

import org.springframework.lang.NonNull;

/** Audio containers we accept, detected from the file's first bytes. */
public enum AudioFormat {
    WEBM("webm", "audio/webm"),
    OGG("ogg", "audio/ogg"),
    WAV("wav", "audio/wav"),
    MP4("mp4", "audio/mp4"),
    MP3("mp3", "audio/mpeg"),
    FLAC("flac", "audio/flac");

    @NonNull private final String extension;
    @NonNull private final String contentType;

    AudioFormat(@NonNull String extension, @NonNull String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    @NonNull public String extension() { return extension; }

    @NonNull public String contentType() { return contentType; }
}
