package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.response.MatchEventResponse;
import java.util.List;
import java.util.UUID;

/**
 * Audit log local (solo lectura) de eventos ya ocurridos en este servicio: goles, tarjetas,
 * sustituciones, observaciones y ciclo de vida del partido (inicio/fin), agregados a partir
 * de las colecciones propias del dominio (partido, gol, tarjeta, etc.). Distinto de
 * {@code integration.auditoria}, que persiste un registro de auditoría dedicado por evento.
 */
public interface MatchEventQueryService {

    /**
     * Lista los eventos de partido ordenados de más reciente a más antiguo.
     *
     * @param matchId si no es nulo, filtra los eventos a ese partido únicamente
     */
    List<MatchEventResponse> listEvents(UUID matchId);
}
