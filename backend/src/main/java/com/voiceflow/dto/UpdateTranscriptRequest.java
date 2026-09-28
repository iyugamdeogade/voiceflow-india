package com.voiceflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTranscriptRequest(@NotBlank @Size(max = 200_000) String editedText) {}
