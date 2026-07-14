package co.edu.escuelaing.techcup.match.storage;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.escuelaing.techcup.match.config.StorageProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(new StorageProperties(tempDir.toString()));
    }

    @AfterEach
    void cleanUp() throws IOException {
        // TempDir is cleaned automatically by JUnit.
    }

    @Test
    void store_happyPath_createsFileInsideMatchDirectory() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "planilla.pdf", "application/pdf", "contenido".getBytes());

        Path stored = Path.of(result);
        assertThat(Files.exists(stored)).isTrue();
        assertThat(stored.getParent().getFileName().toString()).isEqualTo(matchId.toString());
        assertThat(stored.getFileName().toString()).isEqualTo("planilla.pdf");
    }

    @Test
    void store_fileNameWithPathTraversal_isSanitizedAndStaysInMatchDirectory() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "../../etc/passwd", "text/plain", "x".getBytes());

        Path stored = Path.of(result);
        Path matchDir = tempDir.resolve(matchId.toString()).toAbsolutePath().normalize();
        // Path separators are stripped, so the traversal collapses into a single sanitized
        // file name that stays contained within the match's own storage directory.
        assertThat(stored.startsWith(matchDir)).isTrue();
        assertThat(stored.getParent()).isEqualTo(matchDir);
    }

    @Test
    void store_literalDotDotFileName_isSanitizedAndStaysInMatchDirectory() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "..", "text/plain", "x".getBytes());

        Path stored = Path.of(result);
        Path matchDir = tempDir.resolve(matchId.toString()).toAbsolutePath().normalize();
        assertThat(stored.startsWith(matchDir)).isTrue();
        assertThat(stored.getFileName().toString()).isEqualTo("archivo");
    }

    @Test
    void store_fileNameWithDisallowedCharacters_replacesThemWithUnderscore() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "archivo con espacios!@#.pdf", "application/pdf", "x".getBytes());

        Path stored = Path.of(result);
        assertThat(stored.getFileName().toString()).isEqualTo("archivo_con_espacios___.pdf");
    }

    @Test
    void store_emptyFileName_fallsBackToDefaultName() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "", "text/plain", "x".getBytes());

        Path stored = Path.of(result);
        assertThat(stored.getFileName().toString()).isEqualTo("archivo");
    }
}
