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
 * ni rol de arquero. Confirmado con Estadísticas que está bien mandarlos así mientras
 * esos datos no existan.
 *
 * <p>{@code tournamentId} se resuelve consultando a Torneos
 * (GET /tournaments/matches/{matchId}/tournament — ver TournamentClient) justo antes de
 * publicar. Si Torneos no responde o no tiene registrado el partido, va null en vez de
 * bloquear la publicación (best-effort, mismo criterio que el resto de integraciones
 * salientes de este servicio).
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
