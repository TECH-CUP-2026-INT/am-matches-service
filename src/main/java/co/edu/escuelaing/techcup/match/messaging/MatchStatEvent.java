package co.edu.escuelaing.techcup.match.messaging;

import co.edu.escuelaing.techcup.match.entity.enums.MatchResult;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contrato confirmado con el Servicio de Estadísticas (docs/rabbitmq-integration.md de
 * ese repo, sección "Para astromerge (Competencia)"). Routing key:
 * {@code techcup.match.event.stat}.
 *
 * <p>{@code foulsCommitted}, {@code minutesPlayed}, {@code assists} y {@code goalkeeper}
 * van siempre en 0/false: este servicio no trackea faltas, minutos jugados, asistencias
 * ni rol de arquero. {@code tournamentId} va siempre null: no hay ninguna fuente de ese
 * dato hoy (el Servicio de Competencia del que este repo depende para datos de
 * programación — ver CompetenciaClient — ni siquiera está desplegado, y su contrato no
 * incluye tournamentId). Confirmado con Estadísticas que está bien mandarlo así mientras
 * esos datos no existan.
 */
public record MatchStatEvent(
        UUID playerId,
        UUID teamId,
        UUID matchId,
        UUID tournamentId,
        MatchResult result,
        int goals,
        int yellowCards,
        int redCards,
        int foulsCommitted,
        int minutesPlayed,
        int assists,
        boolean goalkeeper,
        LocalDateTime occurredAt
) {
}
