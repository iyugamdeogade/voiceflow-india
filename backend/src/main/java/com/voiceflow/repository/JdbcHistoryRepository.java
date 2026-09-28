package com.voiceflow.repository;

import com.voiceflow.model.TranscriptionRecord;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Plain JDBC (JdbcTemplate) access to transcription_history. All values are bound as parameters. */
@Repository
@Profile("!demo")
public class JdbcHistoryRepository implements HistoryRepository {

    private static final String COLUMNS = "id, language, original_text, edited_text, created_at, updated_at";

    private static final RowMapper<TranscriptionRecord> MAPPER = (rs, rowNum) -> new TranscriptionRecord(
            rs.getLong("id"),
            rs.getString("language"),
            rs.getString("original_text"),
            rs.getString("edited_text"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());

    private final JdbcTemplate jdbc;

    public JdbcHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public TranscriptionRecord insert(String language, String originalText, String editedText) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO transcription_history (user_id, language, original_text, edited_text) VALUES (?, ?, ?, ?)",
                    new String[] {"id"});
            ps.setLong(1, DEFAULT_USER_ID);
            ps.setString(2, language);
            ps.setString(3, originalText);
            ps.setString(4, editedText);
            return ps;
        }, keys);
        Number generatedId = keys.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("The database did not return an id for the inserted transcript");
        }
        long id = generatedId.longValue();
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted row not found"));
    }

    @Override
    public Optional<TranscriptionRecord> findById(long id) {
        List<TranscriptionRecord> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM transcription_history WHERE id = ? AND user_id = ?",
                MAPPER, id, DEFAULT_USER_ID);
        return rows.stream().findFirst();
    }

    @Override
    public List<TranscriptionRecord> findAll(int limit, int offset) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM transcription_history WHERE user_id = ? "
                        + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                MAPPER, DEFAULT_USER_ID, limit, offset);
    }

    @Override
    public long count() {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transcription_history WHERE user_id = ?", Long.class, DEFAULT_USER_ID);
        return n == null ? 0 : n;
    }

    @Override
    public boolean updateEditedText(long id, String editedText) {
        return jdbc.update(
                "UPDATE transcription_history SET edited_text = ? WHERE id = ? AND user_id = ?",
                editedText, id, DEFAULT_USER_ID) > 0;
    }

    @Override
    public boolean deleteById(long id) {
        return jdbc.update("DELETE FROM transcription_history WHERE id = ? AND user_id = ?", id, DEFAULT_USER_ID) > 0;
    }

    @Override
    public int deleteAll() {
        return jdbc.update("DELETE FROM transcription_history WHERE user_id = ?", DEFAULT_USER_ID);
    }
}
