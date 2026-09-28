package com.voiceflow.repository;

import com.voiceflow.model.UserSettings;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("!demo")
public class JdbcSettingsRepository implements SettingsRepository {

    private final JdbcTemplate jdbc;

    public JdbcSettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UserSettings> find() {
        return jdbc.query(
                "SELECT default_language, fix_whitespace, fix_punctuation, capitalize, remove_fillers, auto_cleanup "
                        + "FROM user_settings WHERE user_id = ?",
                (rs, i) -> new UserSettings(
                        rs.getString("default_language"),
                        rs.getBoolean("fix_whitespace"),
                        rs.getBoolean("fix_punctuation"),
                        rs.getBoolean("capitalize"),
                        rs.getBoolean("remove_fillers"),
                        rs.getBoolean("auto_cleanup")),
                HistoryRepository.DEFAULT_USER_ID).stream().findFirst();
    }

    @Override
    @Transactional
    public void save(UserSettings s) {
        // MySQL upsert: insert the row, or update it if this user already has one.
        jdbc.update(
                "INSERT INTO user_settings (user_id, default_language, fix_whitespace, fix_punctuation, capitalize, remove_fillers, auto_cleanup) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE default_language = VALUES(default_language), "
                        + "fix_whitespace = VALUES(fix_whitespace), fix_punctuation = VALUES(fix_punctuation), "
                        + "capitalize = VALUES(capitalize), remove_fillers = VALUES(remove_fillers), "
                        + "auto_cleanup = VALUES(auto_cleanup)",
                HistoryRepository.DEFAULT_USER_ID, s.defaultLanguage(), s.fixWhitespace(), s.fixPunctuation(),
                s.capitalize(), s.removeFillers(), s.autoCleanup());
    }
}
