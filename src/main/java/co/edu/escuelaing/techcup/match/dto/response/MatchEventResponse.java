package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.MatchEventType;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila del audit log local de eventos de partido (consultado vía {@code GET /api/partidos/eventos}),
 * accesible por los roles Admin y Organizador. Se construye a partir de datos ya persistidos en
 * este servicio (goles, tarjetas, sustituciones, observaciones y ciclo de vida del partido); no
 * agrega ninguna tabla nueva.
 */
public record MatchEventResponse(
        MatchEventType tipo,
        UUID matchId,
        UUID entidadId,
        UUID actorId,
        Instant timestamp,
        String detalle
) {
}
