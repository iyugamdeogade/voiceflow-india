package com.voiceflow.controller;

import com.voiceflow.dto.ApiResponse;
import com.voiceflow.dto.SettingsDto;
import com.voiceflow.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService service;

    public SettingsController(SettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<SettingsDto> get() {
        return ApiResponse.ok(service.get());
    }

    @PutMapping
    public ApiResponse<SettingsDto> update(@Valid @RequestBody SettingsDto dto) {
        return ApiResponse.ok(service.update(dto));
    }
}
