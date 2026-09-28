package com.voiceflow.exception;

import com.voiceflow.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception into the same JSON shape:
 * { "success": false, "error": { "code": "...", "message": "..." } }
 * Stack traces and internal messages are never sent to the browser.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabase(DataAccessException ex) {
        log.error("Database error: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.error(
                "DATABASE_UNAVAILABLE",
                "The database could not be reached. Check that MySQL is running and the DB_* settings are correct."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
        if (ex instanceof MaxUploadSizeExceededException) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error(
                    "AUDIO_TOO_LARGE", "The recording is larger than the allowed upload size."));
        }
        if (ex instanceof MultipartException) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "BAD_REQUEST", "The upload could not be read. Send the audio as multipart/form-data."));
        }
        StackTraceElement top = ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : null;
        log.error("Unexpected error {} at {}", ex.getClass().getSimpleName(), top);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                "INTERNAL_ERROR", "Something went wrong on the server. Please try again."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("The request was invalid.");
        return body(status, "VALIDATION_ERROR", message);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            MissingServletRequestPartException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(status, "MISSING_PARAMETER", "Missing required part: " + ex.getRequestPartName());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return body(status, "MISSING_PARAMETER", "Missing required parameter: " + ex.getParameterName());
    }

    /** Fallback for all other standard Spring MVC errors (404, 405, 415, unreadable JSON, ...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object ignoredBody, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        int code = status.value();
        if (code == 404) return body(status, "NOT_FOUND", "That address does not exist.");
        if (code == 405) return body(status, "METHOD_NOT_ALLOWED", "That method is not allowed here.");
        if (code == 415) return body(status, "UNSUPPORTED_MEDIA_TYPE", "That content type is not supported.");
        if (code >= 400 && code < 500) return body(status, "BAD_REQUEST", "The request was invalid.");
        return body(status, "INTERNAL_ERROR", "Something went wrong on the server. Please try again.");
    }

    private ResponseEntity<Object> body(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}
