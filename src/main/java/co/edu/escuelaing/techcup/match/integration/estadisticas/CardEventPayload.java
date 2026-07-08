package co.edu.escuelaing.techcup.match.integration.estadisticas;

import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import java.util.UUID;

public record CardEventPayload(
        UUID matchId,
        UUID teamId,
        UUID playerId,
        CardType cardType,
        int minute
) {
}
