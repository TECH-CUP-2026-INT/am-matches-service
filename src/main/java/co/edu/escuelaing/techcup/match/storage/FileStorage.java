package co.edu.escuelaing.techcup.match.storage;

import java.util.UUID;

public interface FileStorage {

    String store(UUID matchId, String fileName, String contentType, byte[] content);
}
