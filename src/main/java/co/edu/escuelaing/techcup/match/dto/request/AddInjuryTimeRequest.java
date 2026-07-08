package co.edu.escuelaing.techcup.match.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddInjuryTimeRequest(

        @NotNull(message = "Los minutos de tiempo añadido son obligatorios")
        @Min(value = 1, message = "El tiempo añadido debe ser de al menos 1 minuto")
        @Max(value = 15, message = "El tiempo añadido no puede superar 15 minutos")
        Integer minutes
) {
}
