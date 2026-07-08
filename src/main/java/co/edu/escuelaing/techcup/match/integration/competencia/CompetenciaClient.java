package co.edu.escuelaing.techcup.match.integration.competencia;

import java.util.List;
import java.util.UUID;

/**
 * Puerto hacia el Servicio de Competencia. Es precondición obligatoria para "iniciar partido":
 * si no se puede confirmar el partido programado y su alineación, el partido no debe iniciar.
 */
public interface CompetenciaClient {

    ScheduledMatchInfo getScheduledMatch(UUID competenciaMatchId);

    List<ScheduledMatchInfo> getAssignedMatches(UUID refereeId);
}
