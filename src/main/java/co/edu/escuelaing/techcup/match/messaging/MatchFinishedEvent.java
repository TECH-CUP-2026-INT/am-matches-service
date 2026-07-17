package co.edu.escuelaing.techcup.match.messaging;

import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import java.time.Instant;
import java.util.UUID;

/**
 * Contrato de integración Matches -&gt; Tournament (RESULTADO). Routing key
 * {@code techcup.match.finished}. {@code matchId} es el id que Tournament asignó
 * originalmente (persistido acá como {@code Match.competenciaMatchId}), no el id interno
 * de este servicio. {@code ganadorId}/{@code eliminadoId} van null cuando la fase de
 * grupos termina en empate.
 *
 * <p>{@code ausenteId} va null en un partido normal; si viene poblado, el partido terminó
 * por walkover y ese es el equipo que no se presentó. En un walkover gana el equipo
 * presente en ambas fases ({@code ganadorId} lo identifica; {@code eliminadoId} solo se
 * llena en ELIMINATORIA, porque en GRUPOS no se elimina a nadie). El campo hace falta
 * porque el marcador de un walkover queda 0-0: sin él, Tournament no puede separar un
 * 0-0 por incomparecencia de un 0-0 jugado, que en GRUPOS reparte un punto por lado.
 */
public record MatchFinishedEvent(
        UUID matchId,
        UUID tournamentId,
        MatchPhase fase,
        int golesA,
        int golesB,
        UUID ganadorId,
        UUID eliminadoId,
        UUID ausenteId,
        Instant finishedAt
) {
}
