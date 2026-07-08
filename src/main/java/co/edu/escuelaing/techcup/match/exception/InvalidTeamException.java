package co.edu.escuelaing.techcup.match.exception;

import java.util.UUID;

public class InvalidTeamException extends RuntimeException {

    public InvalidTeamException(UUID teamId, UUID matchId) {
        super("El equipo " + teamId + " no participa en el partido " + matchId);
    }
}
