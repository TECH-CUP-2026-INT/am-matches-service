package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import java.time.Instant;
import java.util.UUID;

/**
 * colorHint es solo una sugerencia visual; eventType/cardType son la fuente de verdad
 * para que la UI nunca dependa únicamente del color (accesibilidad daltonismo).
 */
public record CardResponse(
        UUID id,
        UUID matchId,
        UUID teamId,
        UUID playerId,
        CardType cardType,
        String colorHint,
        int minute,
        MatchPeriod period,
        EventType eventType,
        boolean playerSanctioned,
        Instant createdAt
) {
}
