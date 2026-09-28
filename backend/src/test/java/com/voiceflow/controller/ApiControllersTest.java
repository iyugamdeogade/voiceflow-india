package com.voiceflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.voiceflow.dto.HistoryItemResponse;
import com.voiceflow.dto.HistoryPageResponse;
import com.voiceflow.dto.TranscriptionResponse;
import com.voiceflow.exception.ApiException;
import com.voiceflow.provider.SpeechToTextProvider;
import com.voiceflow.service.HistoryService;
import com.voiceflow.service.SettingsService;
import com.voiceflow.service.TextCleanupService;
import com.voiceflow.service.TranscriptionService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.voiceflow.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = {TranscriptionController.class, HistoryController.class, HealthController.class, SettingsController.class})
@Import({GlobalExceptionHandler.class, TextCleanupService.class})
class ApiControllersTest {

    @Autowired MockMvc mvc;
    @MockitoBean TranscriptionService transcriptionService;
    @MockitoBean HistoryService historyService;
    @MockitoBean SettingsService settingsService;
    @MockitoBean SpeechToTextProvider provider;

        @NonNull private final MockMultipartFile audio = Objects.requireNonNull(
            new MockMultipartFile("audio", "r.webm", "audio/webm", new byte[] {1, 2, 3}));

    @Test
    void validUploadReturnsTranscript() throws Exception {
        when(transcriptionService.transcribe(any(), eq("hi"), anyBoolean(), anyBoolean()))
                .thenReturn(new TranscriptionResponse(null, "नमस्ते", null, "hi", "COMPLETED", List.of()));
        mvc.perform(multipart("/api/transcriptions").file(audio).param("language", "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalTranscript").value("नमस्ते"))
                .andExpect(jsonPath("$.data.language").value("hi"));
    }

    @Test
    void missingAudioPartIsBadRequestWithJsonError() throws Exception {
        mvc.perform(multipart("/api/transcriptions").param("language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MISSING_PARAMETER"));
    }

    @Test
    void invalidLanguageErrorIsPassedThrough() throws Exception {
        when(transcriptionService.transcribe(any(), eq("fr"), anyBoolean(), anyBoolean()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE", "Language must be en, hi or mr."));
        mvc.perform(multipart("/api/transcriptions").file(audio).param("language", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_LANGUAGE"));
    }

    @Test
    void providerFailureDoesNotLeakSecretsOrStackTraces() throws Exception {
        when(transcriptionService.transcribe(any(), any(), anyBoolean(), anyBoolean()))
                .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "SPEECH_API_AUTH_FAILED",
                        "The speech provider rejected the API key. Check SPEECH_API_KEY."));
        mvc.perform(multipart("/api/transcriptions").file(audio))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("SPEECH_API_AUTH_FAILED"))
                .andExpect(content().string(Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sk-")))))
                .andExpect(content().string(Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception")))));
    }

    @Test
    void unexpectedExceptionBecomesGeneric500() throws Exception {
        when(transcriptionService.transcribe(any(), any(), anyBoolean(), anyBoolean()))
                .thenThrow(new IllegalStateException("db password is hunter2"));
        mvc.perform(multipart("/api/transcriptions").file(audio))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hunter2")))));
    }

    @Test
    void cleanupEndpointCleansText() throws Exception {
        mvc.perform(post("/api/transcriptions/cleanup").contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"text\":\"hello  world\",\"options\":{\"fixWhitespace\":true,\"fixPunctuation\":true,\"capitalize\":true,\"removeFillers\":false}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cleanedText").value("Hello world."))
                .andExpect(jsonPath("$.data.operations").isArray());
    }

    @Test
    void cleanupRejectsBlankText() throws Exception {
        mvc.perform(post("/api/transcriptions/cleanup").contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON)).content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void historyListReturnsPage() throws Exception {
        var item = new HistoryItemResponse(1, "en", "a", "b", Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
        when(historyService.list(0, 20)).thenReturn(new HistoryPageResponse(List.of(item), 0, 20, 1));
        mvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].editedText").value("b"))
                .andExpect(jsonPath("$.data.totalItems").value(1));
    }

    @Test
    void missingHistoryRecordIs404() throws Exception {
        when(historyService.get(99)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "HISTORY_NOT_FOUND", "missing"));
        mvc.perform(get("/api/history/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HISTORY_NOT_FOUND"));
    }

    @Test
    void nonNumericIdIsBadRequest() throws Exception {
        mvc.perform(get("/api/history/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void saveValidatesInput() throws Exception {
        mvc.perform(post("/api/history").contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content("{\"language\":\"xx\",\"originalText\":\"a\",\"editedText\":\"b\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteAllReturnsCount() throws Exception {
        when(historyService.deleteAll()).thenReturn(3);
        mvc.perform(delete("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(3));
    }

    @Test
    void databaseFailureIs503() throws Exception {
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("jdbc:mysql://secret-host password=abc"))
                .when(historyService).delete(1);
        mvc.perform(delete("/api/history/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("DATABASE_UNAVAILABLE"))
                .andExpect(content().string(Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-host")))));
    }

    @Test
    void healthDoesNotExposeConfiguration() throws Exception {
        when(provider.isConfigured()).thenReturn(true);
        when(historyService.isAvailable()).thenReturn(true);
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.speechProviderConfigured").value(true))
                .andExpect(content().string(Objects.requireNonNull(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("apiKey")))));
    }
}
