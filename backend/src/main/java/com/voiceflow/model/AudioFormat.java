package com.voiceflow.model;

/** Audio containers we accept, detected from the file's first bytes. */
public enum AudioFormat {
    WEBM("webm", "audio/webm"),
    OGG("ogg", "audio/ogg"),
    WAV("wav", "audio/wav"),
    MP4("mp4", "audio/mp4"),
    MP3("mp3", "audio/mpeg"),
    FLAC("flac", "audio/flac");

    private final String extension;
    private final String contentType;

    AudioFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() { return extension; }

    public String contentType() { return contentType; }
}
