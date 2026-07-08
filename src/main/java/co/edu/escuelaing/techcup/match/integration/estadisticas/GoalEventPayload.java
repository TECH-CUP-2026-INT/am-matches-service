package co.edu.escuelaing.techcup.match.integration.estadisticas;

import java.util.UUID;

public record GoalEventPayload(
        UUID matchId,
        UUID teamId,
        UUID playerId,
        int minute,
        int homeScore,
        int awayScore
) {
}
