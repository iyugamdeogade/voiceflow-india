package com.voiceflow.provider;

/**
 * Anything that can turn audio into text. The controller and services only know this interface,
 * so a different provider can be added without changing them.
 */
public interface SpeechToTextProvider {

    /** @param languageCode ISO-639-1 code such as "en", "hi", "mr" (may be null to let the provider decide) */
    TranscriptionResult transcribe(AudioPayload audio, String languageCode);

    /** True when the provider has everything it needs (for example an API key). */
    boolean isConfigured();
}
