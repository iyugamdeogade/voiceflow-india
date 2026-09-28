package com.voiceflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

/** An error we expected and want to show to the user with a clear message. */
public class ApiException extends RuntimeException {

    @NonNull private final HttpStatus status;
    @NonNull private final String code;

    public ApiException(@NonNull HttpStatus status, @NonNull String code, @NonNull String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    @NonNull public HttpStatus getStatus() { return status; }

    @NonNull public String getCode() { return code; }
}
