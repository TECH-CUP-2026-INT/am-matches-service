package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import java.time.Instant;
import java.util.UUID;

public record MatchObservationResponse(
        UUID id,
        UUID matchId,
        UUID refereeId,
        String text,
        Integer minute,
        EventType eventType,
        Instant createdAt
) {
}
