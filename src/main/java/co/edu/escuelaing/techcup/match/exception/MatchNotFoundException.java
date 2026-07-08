package co.edu.escuelaing.techcup.match.exception;

import java.util.UUID;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(UUID matchId) {
        super("No existe un partido con id " + matchId);
    }
}
