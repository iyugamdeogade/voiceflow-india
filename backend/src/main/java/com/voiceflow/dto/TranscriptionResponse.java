package com.voiceflow.dto;

import java.util.List;

/**
 * @param id                  history id, only present if the result was saved
 * @param originalTranscript  exactly what the speech provider returned
 * @param cleanedTranscript   only present if cleanup was requested
 * @param language            the language the user selected (sent to the provider)
 * @param status              COMPLETED or NO_SPEECH_DETECTED
 */
public record TranscriptionResponse(
        Long id,
        String originalTranscript,
        String cleanedTranscript,
        String language,
        String status,
        List<String> cleanupOperations) {}
