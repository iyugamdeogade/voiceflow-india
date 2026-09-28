package com.voiceflow.controller;

import com.voiceflow.dto.ApiResponse;
import com.voiceflow.dto.HealthResponse;
import com.voiceflow.provider.SpeechToTextProvider;
import com.voiceflow.service.HistoryService;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final SpeechToTextProvider provider;
    private final HistoryService history;
    private final Environment environment;

    public HealthController(SpeechToTextProvider provider, HistoryService history, Environment environment) {
        this.provider = provider;
        this.history = history;
        this.environment = environment;
    }

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        boolean demo = environment.matchesProfiles("demo");
        return ApiResponse.ok(new HealthResponse(
                "UP",
                provider.isConfigured(),
                demo ? "demo" : "database",
                !demo && history.isAvailable()));
    }
}
