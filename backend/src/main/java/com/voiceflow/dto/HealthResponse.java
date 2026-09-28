package com.voiceflow.dto;

/** Safe to expose: contains no keys, URLs or credentials. */
public record HealthResponse(String status, boolean speechProviderConfigured, String historyMode, boolean historyAvailable) {}
