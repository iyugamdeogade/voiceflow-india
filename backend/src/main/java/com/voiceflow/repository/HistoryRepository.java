package com.voiceflow.repository;

import com.voiceflow.model.TranscriptionRecord;
import java.util.List;
import java.util.Optional;

public interface HistoryRepository {

    /** The single local user for this MVP. Kept as a column so login can be added later. */
    long DEFAULT_USER_ID = 1L;

    TranscriptionRecord insert(String language, String originalText, String editedText);

    Optional<TranscriptionRecord> findById(long id);

    List<TranscriptionRecord> findAll(int limit, int offset);

    long count();

    boolean updateEditedText(long id, String editedText);

    boolean deleteById(long id);

    int deleteAll();
}
