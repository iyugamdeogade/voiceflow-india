package com.voiceflow.repository;

import com.voiceflow.exception.ApiException;
import com.voiceflow.model.TranscriptionRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

/** Used in "demo" mode (no database). Every operation explains that history is turned off. */
@Repository
@Profile("demo")
public class DemoHistoryRepository implements HistoryRepository {

    private static ApiException disabled() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "HISTORY_DISABLED",
                "History is turned off because the backend runs in demo mode (no database). See the README to enable MySQL.");
    }

    @Override public TranscriptionRecord insert(String l, String o, String e) { throw disabled(); }
    @Override public Optional<TranscriptionRecord> findById(long id) { throw disabled(); }
    @Override public List<TranscriptionRecord> findAll(int limit, int offset) { throw disabled(); }
    @Override public long count() { throw disabled(); }
    @Override public boolean updateEditedText(long id, String t) { throw disabled(); }
    @Override public boolean deleteById(long id) { throw disabled(); }
    @Override public int deleteAll() { throw disabled(); }
}
