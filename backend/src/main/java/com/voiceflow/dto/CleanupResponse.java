package com.voiceflow.dto;

import java.util.List;

public record CleanupResponse(String cleanedText, List<String> operations) {}
