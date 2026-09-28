package com.voiceflow.service;

import com.voiceflow.dto.HistoryItemResponse;
import com.voiceflow.dto.HistoryPageResponse;
import com.voiceflow.exception.ApiException;
import com.voiceflow.model.Language;
import com.voiceflow.model.TranscriptionRecord;
import com.voiceflow.repository.HistoryRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {

    static final int MAX_PAGE_SIZE = 100;

    private final HistoryRepository repository;

    public HistoryService(HistoryRepository repository) {
        this.repository = repository;
    }

    public HistoryItemResponse save(String language, String originalText, String editedText) {
        Language lang = Language.fromCode(language);
        return HistoryItemResponse.from(repository.insert(lang.code(), originalText, editedText));
    }

    public HistoryPageResponse list(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                    "page must be 0 or more and size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        List<HistoryItemResponse> items = repository.findAll(size, page * size).stream()
                .map(HistoryItemResponse::from).toList();
        return new HistoryPageResponse(items, page, size, repository.count());
    }

    public HistoryItemResponse get(long id) {
        return HistoryItemResponse.from(find(id));
    }

    public HistoryItemResponse updateEditedText(long id, String editedText) {
        if (!repository.updateEditedText(id, editedText)) throw notFound();
        return HistoryItemResponse.from(find(id));
    }

    public void delete(long id) {
        if (!repository.deleteById(id)) throw notFound();
    }

    public int deleteAll() {
        return repository.deleteAll();
    }

    /** Used by the health check. Never throws. */
    public boolean isAvailable() {
        try {
            repository.count();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private TranscriptionRecord find(long id) {
        return repository.findById(id).orElseThrow(HistoryService::notFound);
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "HISTORY_NOT_FOUND", "That saved transcription does not exist.");
    }
}
