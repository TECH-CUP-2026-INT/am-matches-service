package co.edu.escuelaing.techcup.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterGoalRequest(

        @NotNull(message = "El equipo anotador es obligatorio")
        UUID teamId,

        @NotNull(message = "El jugador anotador es obligatorio")
        UUID playerId,

        @Min(value = 0, message = "El minuto no puede ser negativo")
        Integer minute
) {
}
