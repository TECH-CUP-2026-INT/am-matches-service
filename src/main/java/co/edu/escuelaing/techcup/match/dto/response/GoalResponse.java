package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import java.time.Instant;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID matchId,
        UUID teamId,
        UUID playerId,
        int minute,
        MatchPeriod period,
        int homeScore,
        int awayScore,
        EventType eventType,
        Instant createdAt
) {
}
