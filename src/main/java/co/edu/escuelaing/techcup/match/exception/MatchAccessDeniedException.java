package co.edu.escuelaing.techcup.match.exception;

import java.util.UUID;

public class MatchAccessDeniedException extends RuntimeException {

    public MatchAccessDeniedException(UUID matchId) {
        super("El partido " + matchId + " no está asignado a este árbitro");
    }
}
