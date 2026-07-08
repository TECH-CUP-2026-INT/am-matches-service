package co.edu.escuelaing.techcup.match.dto.request;

/**
 * Aísla al service layer del tipo MultipartFile de Spring Web: el controller
 * traduce el archivo recibido a este comando antes de invocar al service.
 */
public record MatchSheetUploadCommand(
        String fileName,
        String contentType,
        byte[] content
) {
}
