package com.voiceflow.provider;

import org.springframework.lang.NonNull;

/** Audio bytes plus the file name (with a correct extension) sent to the speech provider. */
public record AudioPayload(@NonNull byte[] data, @NonNull String filename, @NonNull String contentType) {}
