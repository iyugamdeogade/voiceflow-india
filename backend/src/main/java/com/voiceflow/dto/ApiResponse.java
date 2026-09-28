package com.voiceflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.Nullable;

/** Every response uses this envelope so the frontend can handle results in one way. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, @Nullable T data, @Nullable ErrorBody error) {

    public record ErrorBody(String code, String message) {}

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }
}
