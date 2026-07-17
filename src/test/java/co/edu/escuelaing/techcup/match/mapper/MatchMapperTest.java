package co.edu.escuelaing.techcup.match.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchMapperTest {

    private Match buildMatch() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setCompetenciaMatchId(UUID.randomUUID());
        match.setTournamentId(UUID.randomUUID());
        match.setPhase(MatchPhase.GRUPOS);
        match.setHomeTeamId(UUID.randomUUID());
        match.setAwayTeamId(UUID.randomUUID());
        match.setHomeTeamName("Home FC");
        match.setAwayTeamName("Away FC");
        match.setRefereeId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        match.setHomeScore(1);
        match.setAwayScore(0);
        match.setAddedMinutesFirstHalf(2);
        match.setAddedMinutesSecondHalf(0);
        match.setStartedAt(Instant.now());
        return match;
    }

    @Test
    void toResponse_mapsAllFields() {
        Match match = buildMatch();

        MatchResponse response = MatchMapper.toResponse(match, 35, EventType.GOAL);

        assertThat(response.id()).isEqualTo(match.getId());
        assertThat(response.competenciaMatchId()).isEqualTo(match.getCompetenciaMatchId());
        assertThat(response.tournamentId()).isEqualTo(match.getTournamentId());
        assertThat(response.phase()).isEqualTo(MatchPhase.GRUPOS);
        assertThat(response.homeTeamId()).isEqualTo(match.getHomeTeamId());
        assertThat(response.awayTeamId()).isEqualTo(match.getAwayTeamId());
        assertThat(response.homeTeamName()).isEqualTo("Home FC");
        assertThat(response.awayTeamName()).isEqualTo("Away FC");
        assertThat(response.refereeId()).isEqualTo(match.getRefereeId());
        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(response.currentPeriod()).isEqualTo(MatchPeriod.FIRST_HALF);
        assertThat(response.homeScore()).isEqualTo(1);
        assertThat(response.awayScore()).isZero();
        assertThat(response.addedMinutesFirstHalf()).isEqualTo(2);
        assertThat(response.addedMinutesSecondHalf()).isZero();
        assertThat(response.currentMinute()).isEqualTo(35);
        assertThat(response.startedAt()).isEqualTo(match.getStartedAt());
        assertThat(response.endedAt()).isNull();
        assertThat(response.eventType()).isEqualTo(EventType.GOAL);
    }

    @Test
    void toResponse_withEndedMatch_includesEndedAt() {
        Match match = buildMatch();
        match.setStatus(MatchStatus.FINISHED);
        match.setEndedAt(Instant.now());

        MatchResponse response = MatchMapper.toResponse(match, 90, EventType.MATCH_FINISHED);

        assertThat(response.endedAt()).isEqualTo(match.getEndedAt());
        assertThat(response.status()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void toSummary_inProgressMatch_isManageable() {
        Match match = buildMatch();

        MatchSummaryResponse summary = MatchMapper.toSummary(match);

        assertThat(summary.manageable()).isTrue();
        assertThat(summary.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(summary.homeScore()).isEqualTo(1);
    }

    @Test
    void toSummary_pausedMatch_isManageable() {
        Match match = buildMatch();
        match.setStatus(MatchStatus.PAUSED);

        MatchSummaryResponse summary = MatchMapper.toSummary(match);

        assertThat(summary.manageable()).isTrue();
    }

    @Test
    void toSummary_finishedMatch_isNotManageable() {
        Match match = buildMatch();
        match.setStatus(MatchStatus.FINISHED);

        MatchSummaryResponse summary = MatchMapper.toSummary(match);

        assertThat(summary.manageable()).isFalse();
    }

}
