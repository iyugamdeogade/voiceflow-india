package com.voiceflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CleanupRequest(
        @NotBlank(message = "must not be empty") @Size(max = 200_000, message = "is too long") String text,
        CleanupOptions options) {}
