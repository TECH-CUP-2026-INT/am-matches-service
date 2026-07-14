package co.edu.escuelaing.techcup.match.storage;

import co.edu.escuelaing.techcup.match.config.StorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorage implements FileStorage {

    private static final Pattern DISALLOWED_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final Pattern ONLY_DOTS = Pattern.compile("^\\.+$");
    private static final String FALLBACK_FILE_NAME = "archivo";

    private final Path baseDir;

    public LocalFileStorage(StorageProperties storageProperties) {
        this.baseDir = Path.of(storageProperties.matchSheetsDir()).toAbsolutePath().normalize();
    }

    @Override
    public String store(UUID matchId, String fileName, String contentType, byte[] content) {
        Path matchDir = baseDir.resolve(matchId.toString()).normalize();
        Path target = matchDir.resolve(sanitize(fileName)).normalize();

        // Defensa en profundidad: aunque sanitize() ya elimina separadores de ruta,
        // se verifica que la ruta resuelta siga contenida en el directorio del partido.
        if (!target.startsWith(matchDir)) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + fileName);
        }

        try {
            Files.createDirectories(matchDir);
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la planilla del partido " + matchId, e);
        }

        return target.toString();
    }

    private String sanitize(String fileName) {
        String cleaned = DISALLOWED_CHARS.matcher(fileName).replaceAll("_");
        if (cleaned.isBlank() || ONLY_DOTS.matcher(cleaned).matches()) {
            return FALLBACK_FILE_NAME;
        }
        return cleaned;
    }
}
