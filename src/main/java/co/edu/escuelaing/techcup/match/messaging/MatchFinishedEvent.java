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
 */
public record MatchFinishedEvent(
        UUID matchId,
        UUID tournamentId,
        MatchPhase fase,
        int golesA,
        int golesB,
        UUID ganadorId,
        UUID eliminadoId,
        Instant finishedAt
) {
}
