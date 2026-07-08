package co.edu.escuelaing.techcup.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterSubstitutionRequest(

        @NotNull(message = "El equipo es obligatorio")
        UUID teamId,

        @NotNull(message = "El jugador que sale es obligatorio")
        UUID playerOutId,

        @NotNull(message = "El jugador que entra es obligatorio")
        UUID playerInId,

        @Min(value = 0, message = "El minuto no puede ser negativo")
        Integer minute
) {
}
