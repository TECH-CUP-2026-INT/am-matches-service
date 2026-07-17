package co.edu.escuelaing.techcup.match.dto.request;

import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Contrato de integración Tournament -&gt; Matches (DEFINICIÓN): payload recibido en
 * {@code POST /api/partidos}. Nombres de campo fijados por ese contrato, no por la
 * convención interna de este servicio.
 */
public record MatchDefinitionRequest(

        @NotNull(message = "El id del partido es obligatorio")
        UUID matchId,

        @NotNull(message = "El id del torneo es obligatorio")
        UUID tournamentId,

        @NotNull(message = "La fase es obligatoria")
        MatchPhase fase,

        @NotNull(message = "El equipo A es obligatorio")
        UUID equipoAId,

        @NotNull(message = "El equipo B es obligatorio")
        UUID equipoBId,

        @NotBlank(message = "El nombre del equipo A es obligatorio")
        String equipoANombre,

        @NotBlank(message = "El nombre del equipo B es obligatorio")
        String equipoBNombre,

        @NotNull(message = "La fecha del partido es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La hora del partido es obligatoria")
        LocalTime hora,

        @NotNull(message = "El árbitro asignado es obligatorio")
        UUID arbitroId,

        UUID canchaId
) {
}
