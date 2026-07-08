package co.edu.escuelaing.techcup.match.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RegisterObservationRequest(

        @NotBlank(message = "La observación no puede estar vacía")
        String text,

        @Min(value = 0, message = "El minuto no puede ser negativo")
        Integer minute
) {
}
