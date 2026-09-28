package com.voiceflow.dto;

import com.voiceflow.model.UserSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SettingsDto(
        @NotBlank @Pattern(regexp = "en|hi|mr", message = "must be en, hi or mr") String defaultLanguage,
        boolean fixWhitespace,
        boolean fixPunctuation,
        boolean capitalize,
        boolean removeFillers,
        boolean autoCleanup) {

    public static SettingsDto from(UserSettings s) {
        return new SettingsDto(s.defaultLanguage(), s.fixWhitespace(), s.fixPunctuation(),
                s.capitalize(), s.removeFillers(), s.autoCleanup());
    }

    public UserSettings toModel() {
        return new UserSettings(defaultLanguage, fixWhitespace, fixPunctuation, capitalize, removeFillers, autoCleanup);
    }
}
