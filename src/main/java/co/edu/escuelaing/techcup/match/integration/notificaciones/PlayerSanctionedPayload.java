package co.edu.escuelaing.techcup.match.integration.notificaciones;

import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import java.time.Instant;
import java.util.UUID;

public record PlayerSanctionedPayload(
        UUID matchId,
        UUID teamId,
        UUID playerId,
        CardType triggeringCardType,
        int yellowCardsInMatch,
        Instant occurredAt
) {
}
