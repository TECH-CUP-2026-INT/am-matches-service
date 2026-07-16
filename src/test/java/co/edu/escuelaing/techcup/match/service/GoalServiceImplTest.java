package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.request.RegisterGoalRequest;
import co.edu.escuelaing.techcup.match.dto.response.GoalResponse;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.estadisticas.MatchEventPublisher;
import co.edu.escuelaing.techcup.match.repository.GoalRepository;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock
    private GoalRepository goalRepository;
    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchAccessService matchAccessService;
    @Mock
    private MatchEventPublisher eventPublisher;
    @Mock
    private AuditReporter auditReporter;

    private GoalServiceImpl goalService;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private final UUID homeTeamId = UUID.randomUUID();
    private final UUID awayTeamId = UUID.randomUUID();
    private Match match;

    @BeforeEach
    void setUp() {
        goalService = new GoalServiceImpl(goalRepository, matchRepository, matchAccessService, eventPublisher, auditReporter);

        match = new Match();
        match.setId(matchId);
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now());

        lenient().when(matchAccessService.requireActiveMatch(matchId, refereeId)).thenReturn(match);
        lenient().when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal goal = invocation.getArgument(0);
            goal.setId(UUID.randomUUID());
            goal.setCreatedAt(Instant.now());
            return goal;
        });
    }

    @Test
    void homeTeamGoal_incrementsHomeScoreOnly() {
        GoalResponse response = goalService.registerGoal(
                matchId, refereeId, new RegisterGoalRequest(homeTeamId, UUID.randomUUID(), 20));

        assertThat(response.homeScore()).isEqualTo(1);
        assertThat(response.awayScore()).isZero();
        assertThat(match.getHomeScore()).isEqualTo(1);
    }

    @Test
    void awayTeamGoal_incrementsAwayScoreOnly() {
        GoalResponse response = goalService.registerGoal(
                matchId, refereeId, new RegisterGoalRequest(awayTeamId, UUID.randomUUID(), 55));

        assertThat(response.awayScore()).isEqualTo(1);
        assertThat(response.homeScore()).isZero();
    }

    @Test
    void listGoals_computesRunningScorePerTeamInMinuteOrder() {
        Goal homeGoal = goalOf(homeTeamId, 10);
        Goal awayGoal = goalOf(awayTeamId, 30);
        Goal secondHomeGoal = goalOf(homeTeamId, 50);
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        when(goalRepository.findByMatchIdOrderByMinuteAsc(matchId))
                .thenReturn(List.of(homeGoal, awayGoal, secondHomeGoal));

        List<GoalResponse> responses = goalService.listGoals(matchId, refereeId);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).homeScore()).isEqualTo(1);
        assertThat(responses.get(0).awayScore()).isZero();
        assertThat(responses.get(1).awayScore()).isEqualTo(1);
        assertThat(responses.get(2).homeScore()).isEqualTo(2);
    }

    private Goal goalOf(UUID teamId, int minute) {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setMatchId(matchId);
        goal.setTeamId(teamId);
        goal.setPlayerId(UUID.randomUUID());
        goal.setMinute(minute);
        goal.setPeriod(MatchPeriod.FIRST_HALF);
        goal.setCreatedAt(Instant.now());
        return goal;
    }
}
