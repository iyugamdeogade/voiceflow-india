package com.voiceflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveTranscriptRequest(
        @NotBlank @Pattern(regexp = "en|hi|mr", message = "must be en, hi or mr") String language,
        @NotBlank @Size(max = 200_000) String originalText,
        @NotBlank @Size(max = 200_000) String editedText) {}
