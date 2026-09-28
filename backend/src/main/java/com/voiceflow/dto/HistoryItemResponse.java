package com.voiceflow.dto;

import com.voiceflow.model.TranscriptionRecord;
import java.time.Instant;

public record HistoryItemResponse(
        long id, String language, String originalText, String editedText, Instant createdAt, Instant updatedAt) {

    public static HistoryItemResponse from(TranscriptionRecord r) {
        return new HistoryItemResponse(
                r.id(), r.language(), r.originalText(), r.editedText(), r.createdAt(), r.updatedAt());
    }
}
