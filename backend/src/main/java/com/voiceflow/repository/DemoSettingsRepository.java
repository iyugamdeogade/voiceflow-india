package com.voiceflow.repository;

import com.voiceflow.exception.ApiException;
import com.voiceflow.model.UserSettings;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
@Profile("demo")
public class DemoSettingsRepository implements SettingsRepository {

    private static ApiException disabled() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "HISTORY_DISABLED",
                "Server-side settings are turned off in demo mode; the browser keeps them locally instead.");
    }

    @Override public Optional<UserSettings> find() { throw disabled(); }
    @Override public void save(UserSettings settings) { throw disabled(); }
}
