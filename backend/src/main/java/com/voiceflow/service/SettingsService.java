package com.voiceflow.service;

import com.voiceflow.dto.SettingsDto;
import com.voiceflow.model.UserSettings;
import com.voiceflow.repository.SettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private final SettingsRepository repository;

    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    public SettingsDto get() {
        return SettingsDto.from(repository.find().orElseGet(UserSettings::defaults));
    }

    public SettingsDto update(SettingsDto dto) {
        repository.save(dto.toModel());
        return dto;
    }
}
