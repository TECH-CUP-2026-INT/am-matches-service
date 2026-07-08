package co.edu.escuelaing.techcup.match.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MatchSheetResponse(
        UUID id,
        UUID matchId,
        String fileUrl,
        UUID uploadedBy,
        Instant uploadedAt
) {
}
