package co.edu.escuelaing.techcup.match.dto.request;

import java.util.Arrays;
import java.util.Objects;

/**
 * Aísla al service layer del tipo MultipartFile de Spring Web: el controller
 * traduce el archivo recibido a este comando antes de invocar al service.
 */
public record MatchSheetUploadCommand(
        String fileName,
        String contentType,
        byte[] content
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MatchSheetUploadCommand other)) {
            return false;
        }
        return Objects.equals(fileName, other.fileName)
                && Objects.equals(contentType, other.contentType)
                && Arrays.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fileName, contentType);
        return 31 * result + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "MatchSheetUploadCommand[fileName=" + fileName
                + ", contentType=" + contentType
                + ", content=" + Arrays.toString(content) + "]";
    }
}
