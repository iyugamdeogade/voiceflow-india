package com.voiceflow.model;

import com.voiceflow.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Languages offered in the UI. The code is the ISO-639-1 value sent to the speech provider. */
public enum Language {
    EN("en"), HI("hi"), MR("mr");

    private final String code;

    Language(String code) { this.code = code; }

    public String code() { return code; }

    public static Language fromCode(String value) {
        if (value == null || value.isBlank() || "auto".equalsIgnoreCase(value.trim())) {
            return EN;
        }
        if (value != null) {
            for (Language l : values()) {
                if (l.code.equalsIgnoreCase(value.trim())) return l;
            }
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LANGUAGE",
                "Language must be one of: en (English), hi (Hindi), mr (Marathi).");
    }
}
