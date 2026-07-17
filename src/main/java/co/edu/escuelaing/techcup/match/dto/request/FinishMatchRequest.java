package co.edu.escuelaing.techcup.match.dto.request;

import jakarta.validation.constraints.Min;
import java.util.UUID;

/**
 * Cuerpo opcional de "finalizar partido". Solo hace falta completarlo en dos casos:
 * walkover (equipo ausente) o desempate por penales en eliminatoria.
 */
public record FinishMatchRequest(

        Boolean walkover,

        UUID equipoAusenteId,

        @Min(value = 0, message = "Los goles de penales no pueden ser negativos")
        Integer golesPenalesA,

        @Min(value = 0, message = "Los goles de penales no pueden ser negativos")
        Integer golesPenalesB
) {
}
