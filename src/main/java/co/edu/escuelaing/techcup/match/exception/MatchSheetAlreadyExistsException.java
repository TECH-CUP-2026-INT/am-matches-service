package co.edu.escuelaing.techcup.match.exception;

import java.util.UUID;

public class MatchSheetAlreadyExistsException extends RuntimeException {

    public MatchSheetAlreadyExistsException(UUID matchId) {
        super("Ya existe una planilla registrada para el partido " + matchId);
    }
}
