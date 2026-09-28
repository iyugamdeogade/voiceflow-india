package com.voiceflow.provider;

/** Audio bytes plus the file name (with a correct extension) sent to the speech provider. */
public record AudioPayload(byte[] data, String filename, String contentType) {}
