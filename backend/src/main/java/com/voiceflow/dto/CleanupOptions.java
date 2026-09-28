package com.voiceflow.dto;

/** Which cleanup rules to apply. */
public record CleanupOptions(boolean fixWhitespace, boolean fixPunctuation, boolean capitalize, boolean removeFillers) {

    public static CleanupOptions defaults(boolean removeFillers) {
        return new CleanupOptions(true, true, true, removeFillers);
    }
}
