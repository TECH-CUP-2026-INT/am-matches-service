package co.edu.escuelaing.techcup.match.integration.competencia;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos del partido programado y su alineación, tal como los provee el Servicio de Competencia.
 */
public record ScheduledMatchInfo(
        UUID competenciaMatchId,
        UUID homeTeamId,
        UUID awayTeamId,
        String homeTeamName,
        String awayTeamName,
        Instant scheduledKickoff,
        boolean lineupConfirmed
) {
}
