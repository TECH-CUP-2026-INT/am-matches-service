package co.edu.escuelaing.techcup.match.dto.request;

import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterCardRequest(

        @NotNull(message = "El equipo del jugador es obligatorio")
        UUID teamId,

        @NotNull(message = "El jugador sancionado es obligatorio")
        UUID playerId,

        @NotNull(message = "El tipo de tarjeta es obligatorio")
        CardType cardType,

        @Min(value = 0, message = "El minuto no puede ser negativo")
        Integer minute
) {
}
