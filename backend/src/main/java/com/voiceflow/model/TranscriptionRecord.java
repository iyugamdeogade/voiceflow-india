package com.voiceflow.model;

import java.time.Instant;

public record TranscriptionRecord(
        long id, String language, String originalText, String editedText, Instant createdAt, Instant updatedAt) {}
