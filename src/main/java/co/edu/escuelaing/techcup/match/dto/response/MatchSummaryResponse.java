package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.util.UUID;

/**
 * Vista liviana para el listado "mis partidos asignados" del árbitro.
 * manageable indica si el botón "gestionar partido" debe habilitarse (solo si ya inició).
 */
public record MatchSummaryResponse(
        UUID id,
        UUID competenciaMatchId,
        String homeTeamName,
        String awayTeamName,
        MatchStatus status,
        boolean manageable,
        int homeScore,
        int awayScore
) {
}
