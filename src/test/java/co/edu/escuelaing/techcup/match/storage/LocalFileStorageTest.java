package co.edu.escuelaing.techcup.match.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.escuelaing.techcup.match.config.StorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
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
        assertThat(stored.getParent().getFileName()).hasToString(matchId.toString());
        assertThat(stored.getFileName()).hasToString("planilla.pdf");
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
        assertThat(stored).hasParentRaw(matchDir);
    }

    @Test
    void store_literalDotDotFileName_isSanitizedAndStaysInMatchDirectory() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "..", "text/plain", "x".getBytes());

        Path stored = Path.of(result);
        Path matchDir = tempDir.resolve(matchId.toString()).toAbsolutePath().normalize();
        assertThat(stored.startsWith(matchDir)).isTrue();
        assertThat(stored.getFileName()).hasToString("archivo");
    }

    @Test
    void store_fileNameWithDisallowedCharacters_replacesThemWithUnderscore() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "archivo con espacios!@#.pdf", "application/pdf", "x".getBytes());

        Path stored = Path.of(result);
        assertThat(stored.getFileName()).hasToString("archivo_con_espacios___.pdf");
    }

    @Test
    void store_emptyFileName_fallsBackToDefaultName() {
        UUID matchId = UUID.randomUUID();
        String result = storage.store(matchId, "", "text/plain", "x".getBytes());

        Path stored = Path.of(result);
        assertThat(stored.getFileName()).hasToString("archivo");
    }

    @Test
    void store_whenMatchDirectoryPathIsBlockedByAFile_wrapsIOExceptionAsUnchecked() throws IOException {
        UUID matchId = UUID.randomUUID();
        // Un archivo regular ya ocupa el path donde debería crearse el directorio del
        // partido: Files.createDirectories no puede crear un directorio ahí y lanza IOException.
        Files.write(tempDir.resolve(matchId.toString()), "ocupado".getBytes());

        assertThrows(UncheckedIOException.class,
                () -> storage.store(matchId, "planilla.pdf", "application/pdf", "x".getBytes()));
    }
}
