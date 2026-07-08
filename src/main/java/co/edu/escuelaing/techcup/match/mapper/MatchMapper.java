package co.edu.escuelaing.techcup.match.mapper;

import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.integration.competencia.ScheduledMatchInfo;

public final class MatchMapper {

    private MatchMapper() {
    }

    public static MatchResponse toResponse(Match match, int currentMinute, EventType eventType) {
        return new MatchResponse(
                match.getId(),
                match.getCompetenciaMatchId(),
                match.getHomeTeamId(),
                match.getAwayTeamId(),
                match.getHomeTeamName(),
                match.getAwayTeamName(),
                match.getRefereeId(),
                match.getStatus(),
                match.getCurrentPeriod(),
                match.getHomeScore(),
                match.getAwayScore(),
                match.getAddedMinutesFirstHalf(),
                match.getAddedMinutesSecondHalf(),
                currentMinute,
                match.getStartedAt(),
                match.getEndedAt(),
                eventType
        );
    }

    public static MatchSummaryResponse toSummary(Match match) {
        boolean manageable = match.getStatus() == MatchStatus.IN_PROGRESS || match.getStatus() == MatchStatus.PAUSED;
        return new MatchSummaryResponse(
                match.getId(),
                match.getCompetenciaMatchId(),
                match.getHomeTeamName(),
                match.getAwayTeamName(),
                match.getStatus(),
                manageable,
                match.getHomeScore(),
                match.getAwayScore()
        );
    }

    public static MatchSummaryResponse toUnstartedSummary(ScheduledMatchInfo scheduled, boolean manageable) {
        return new MatchSummaryResponse(
                null,
                scheduled.competenciaMatchId(),
                scheduled.homeTeamName(),
                scheduled.awayTeamName(),
                MatchStatus.SCHEDULED,
                manageable,
                0,
                0
        );
    }
}
