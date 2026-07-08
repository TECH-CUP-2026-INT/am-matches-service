package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidMatchStateException;
import co.edu.escuelaing.techcup.match.exception.InvalidTeamException;
import co.edu.escuelaing.techcup.match.exception.MatchAccessDeniedException;
import co.edu.escuelaing.techcup.match.exception.MatchNotFoundException;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reglas de acceso y estado compartidas por todos los servicios que registran
 * eventos sobre un partido (goles, tarjetas, sustituciones, observaciones, planilla).
 */
@Component
public class MatchAccessService {

    private final MatchRepository matchRepository;

    public MatchAccessService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public Match requireOwnedMatch(UUID matchId, UUID refereeId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        if (!match.getRefereeId().equals(refereeId)) {
            throw new MatchAccessDeniedException(matchId);
        }
        return match;
    }

    public Match requireActiveMatch(UUID matchId, UUID refereeId) {
        Match match = requireOwnedMatch(matchId, refereeId);
        if (match.getStatus() != MatchStatus.IN_PROGRESS && match.getStatus() != MatchStatus.PAUSED) {
            throw new InvalidMatchStateException(
                    "El partido no está en curso, no se pueden registrar eventos en este momento");
        }
        return match;
    }

    public void validateTeamBelongsToMatch(Match match, UUID teamId) {
        if (!teamId.equals(match.getHomeTeamId()) && !teamId.equals(match.getAwayTeamId())) {
            throw new InvalidTeamException(teamId, match.getId());
        }
    }
}
