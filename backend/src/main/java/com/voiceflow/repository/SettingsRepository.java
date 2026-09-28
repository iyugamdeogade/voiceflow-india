package com.voiceflow.repository;

import com.voiceflow.model.UserSettings;
import java.util.Optional;

public interface SettingsRepository {
    Optional<UserSettings> find();

    void save(UserSettings settings);
}
