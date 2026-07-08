package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.MatchSheetUploadCommand;
import co.edu.escuelaing.techcup.match.dto.response.MatchSheetResponse;
import java.util.UUID;

public interface MatchSheetService {

    MatchSheetResponse uploadSheet(UUID matchId, UUID refereeId, MatchSheetUploadCommand command);

    MatchSheetResponse getSheet(UUID matchId, UUID refereeId);
}
