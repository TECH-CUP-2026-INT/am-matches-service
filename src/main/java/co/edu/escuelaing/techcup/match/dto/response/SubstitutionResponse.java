package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import java.time.Instant;
import java.util.UUID;

public record SubstitutionResponse(
        UUID id,
        UUID matchId,
        UUID teamId,
        UUID playerOutId,
        UUID playerInId,
        int minute,
        MatchPeriod period,
        EventType eventType,
        Instant createdAt
) {
}
