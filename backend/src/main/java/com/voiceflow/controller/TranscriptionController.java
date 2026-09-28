package com.voiceflow.controller;

import com.voiceflow.dto.ApiResponse;
import com.voiceflow.dto.CleanupOptions;
import com.voiceflow.dto.CleanupRequest;
import com.voiceflow.dto.CleanupResponse;
import com.voiceflow.dto.TranscriptionResponse;
import com.voiceflow.service.TextCleanupService;
import com.voiceflow.service.TranscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transcriptions")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;
    private final TextCleanupService cleanupService;

    public TranscriptionController(TranscriptionService transcriptionService, TextCleanupService cleanupService) {
        this.transcriptionService = transcriptionService;
        this.cleanupService = cleanupService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TranscriptionResponse> transcribe(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "false") boolean cleanup,
            @RequestParam(defaultValue = "false") boolean save) {
        return ApiResponse.ok(transcriptionService.transcribe(audio, language, cleanup, save));
    }

    @PostMapping("/cleanup")
    public ApiResponse<CleanupResponse> cleanup(@Valid @RequestBody CleanupRequest request) {
        CleanupOptions options = request.options() != null ? request.options() : CleanupOptions.defaults(false);
        TextCleanupService.Result result = cleanupService.clean(request.text(), options);
        return ApiResponse.ok(new CleanupResponse(result.text(), result.operations()));
    }
}
