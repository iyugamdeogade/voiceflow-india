package com.voiceflow.controller;

import com.voiceflow.dto.ApiResponse;
import com.voiceflow.dto.HistoryItemResponse;
import com.voiceflow.dto.HistoryPageResponse;
import com.voiceflow.dto.SaveTranscriptRequest;
import com.voiceflow.dto.UpdateTranscriptRequest;
import com.voiceflow.service.HistoryService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<HistoryPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<HistoryItemResponse> get(@PathVariable long id) {
        return ApiResponse.ok(service.get(id));
    }

    /** Saves a transcript (original from the provider + the text the user kept). */
    @PostMapping
    public ResponseEntity<ApiResponse<HistoryItemResponse>> save(@Valid @RequestBody SaveTranscriptRequest request) {
        HistoryItemResponse saved = service.save(request.language(), request.originalText(), request.editedText());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved));
    }

    /** Updates only the edited text; the original transcript is never changed. */
    @PutMapping("/{id}")
    public ApiResponse<HistoryItemResponse> update(
            @PathVariable long id, @Valid @RequestBody UpdateTranscriptRequest request) {
        return ApiResponse.ok(service.updateEditedText(id, request.editedText()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.ok(Map.of("deleted", 1));
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> deleteAll() {
        return ApiResponse.ok(Map.of("deleted", service.deleteAll()));
    }
}
