package com.voiceflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.voiceflow.model.TranscriptionRecord;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;

/** Runs the real JDBC code against an in-memory H2 database (MySQL compatibility mode). */
class JdbcHistoryRepositoryTest {

    private DriverManagerDataSource dataSource;
    private JdbcHistoryRepository repo;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema-h2.sql")).execute(dataSource);
        repo = new JdbcHistoryRepository(new JdbcTemplate(Objects.requireNonNull(dataSource)));
    }

    @AfterEach
    void tearDown() {
        new JdbcTemplate(Objects.requireNonNull(dataSource)).execute("DROP ALL OBJECTS");
    }

    @Test
    void insertsAndReadsBackDevanagariText() {
        TranscriptionRecord saved = repo.insert("hi", "नमस्ते दुनिया", "नमस्ते, दुनिया।");
        assertThat(saved.id()).isPositive();
        TranscriptionRecord loaded = repo.findById(saved.id()).orElseThrow();
        assertThat(loaded.originalText()).isEqualTo("नमस्ते दुनिया");
        assertThat(loaded.editedText()).isEqualTo("नमस्ते, दुनिया।");
        assertThat(loaded.language()).isEqualTo("hi");
        assertThat(loaded.createdAt()).isNotNull();
    }

    @Test
    void updateChangesOnlyEditedText() {
        long id = repo.insert("en", "original", "original").id();
        assertThat(repo.updateEditedText(id, "changed")).isTrue();
        TranscriptionRecord r = repo.findById(id).orElseThrow();
        assertThat(r.originalText()).isEqualTo("original");
        assertThat(r.editedText()).isEqualTo("changed");
        assertThat(repo.updateEditedText(9999, "x")).isFalse();
    }

    @Test
    void listsNewestFirstWithPaging() {
        long a = repo.insert("en", "a", "a").id();
        long b = repo.insert("en", "b", "b").id();
        long c = repo.insert("en", "c", "c").id();
        assertThat(repo.count()).isEqualTo(3);
        List<TranscriptionRecord> firstPage = repo.findAll(2, 0);
        assertThat(firstPage).extracting(record -> record.id()).containsExactly(c, b);
        assertThat(repo.findAll(2, 2)).extracting(record -> record.id()).containsExactly(a);
    }

    @Test
    void deletesOneAndAll() {
        long a = repo.insert("en", "a", "a").id();
        repo.insert("mr", "b", "b");
        assertThat(repo.deleteById(a)).isTrue();
        assertThat(repo.deleteById(a)).isFalse();
        assertThat(repo.findById(a)).isEmpty();
        assertThat(repo.deleteAll()).isEqualTo(1);
        assertThat(repo.count()).isZero();
    }
}
