package co.edu.escuelaing.techcup.match.dto.response;

import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID competenciaMatchId,
        UUID homeTeamId,
        UUID awayTeamId,
        String homeTeamName,
        String awayTeamName,
        UUID refereeId,
        MatchStatus status,
        MatchPeriod currentPeriod,
        int homeScore,
        int awayScore,
        int addedMinutesFirstHalf,
        int addedMinutesSecondHalf,
        int currentMinute,
        Instant startedAt,
        Instant endedAt,
        EventType eventType
) {
}
