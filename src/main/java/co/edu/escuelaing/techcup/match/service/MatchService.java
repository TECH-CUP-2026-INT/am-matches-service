package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface MatchService {

    List<MatchSummaryResponse> listAssignedMatches(UUID refereeId);

    MatchResponse getMatch(UUID matchId, UUID refereeId);

    MatchResponse startMatch(UUID competenciaMatchId, UUID refereeId);

    MatchResponse pauseMatch(UUID matchId, UUID refereeId);

    MatchResponse resumeMatch(UUID matchId, UUID refereeId);

    MatchResponse startNextPeriod(UUID matchId, UUID refereeId);

    MatchResponse addInjuryTime(UUID matchId, UUID refereeId, int minutes);

    MatchResponse finishMatch(UUID matchId, UUID refereeId);
}
