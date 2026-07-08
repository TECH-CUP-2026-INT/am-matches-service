package co.edu.escuelaing.techcup.match.integration.estadisticas;

import java.util.UUID;

public record SubstitutionEventPayload(
        UUID matchId,
        UUID teamId,
        UUID playerOutId,
        UUID playerInId,
        int minute
) {
}
