package com.voiceflow.model;

public record UserSettings(
        String defaultLanguage,
        boolean fixWhitespace,
        boolean fixPunctuation,
        boolean capitalize,
        boolean removeFillers,
        boolean autoCleanup) {

    public static UserSettings defaults() {
        return new UserSettings("en", true, true, true, false, false);
    }
}
