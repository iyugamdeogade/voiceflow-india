package com.voiceflow.service;

import com.voiceflow.config.AppProperties;
import com.voiceflow.dto.CleanupOptions;
import com.voiceflow.dto.TranscriptionResponse;
import com.voiceflow.model.Language;
import com.voiceflow.provider.AudioPayload;
import com.voiceflow.provider.SpeechToTextProvider;
import com.voiceflow.provider.TranscriptionResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * The main use case: validate audio -> ask the speech provider -> optionally clean up -> optionally save.
 * Audio is never stored, and neither audio nor transcript text is logged.
 */
@Service
public class TranscriptionService {

    private final AudioValidator validator;
    private final SpeechToTextProvider provider;
    private final TextCleanupService cleanup;
    private final HistoryService history;
    private final boolean defaultRemoveFillers;

    public TranscriptionService(AudioValidator validator, SpeechToTextProvider provider,
                                TextCleanupService cleanup, HistoryService history, AppProperties props) {
        this.validator = validator;
        this.provider = provider;
        this.cleanup = cleanup;
        this.history = history;
        this.defaultRemoveFillers = props.cleanup().defaultRemoveFillers();
    }

    public TranscriptionResponse transcribe(MultipartFile file, String language, boolean applyCleanup, boolean save) {
        Language lang = Language.fromCode(language);
        AudioPayload audio = validator.validate(file);

        TranscriptionResult result = provider.transcribe(audio, lang.code());
        String original = result.text() == null ? "" : result.text();
        String detectedLanguage = result.language() == null || result.language().isBlank() ? lang.code() : result.language();

        if (original.isBlank()) {
            return new TranscriptionResponse(null, "", null, detectedLanguage, "NO_SPEECH_DETECTED", List.of());
        }

        String cleaned = null;
        List<String> operations = List.of();
        if (applyCleanup) {
            TextCleanupService.Result r = cleanup.clean(original, CleanupOptions.defaults(defaultRemoveFillers));
            cleaned = r.text();
            operations = r.operations();
        }

        Long id = null;
        if (save) {
            id = history.save(detectedLanguage, original, cleaned != null ? cleaned : original).id();
        }
        return new TranscriptionResponse(id, original, cleaned, detectedLanguage, "COMPLETED", operations);
    }
}
