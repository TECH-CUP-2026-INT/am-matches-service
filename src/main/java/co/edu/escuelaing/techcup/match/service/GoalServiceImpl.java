package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterGoalRequest;
import co.edu.escuelaing.techcup.match.dto.response.GoalResponse;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.auditoria.MatchAuditEvent;
import co.edu.escuelaing.techcup.match.integration.estadisticas.GoalEventPayload;
import co.edu.escuelaing.techcup.match.integration.estadisticas.MatchEventPublisher;
import co.edu.escuelaing.techcup.match.mapper.GoalMapper;
import co.edu.escuelaing.techcup.match.repository.GoalRepository;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MatchRepository matchRepository;
    private final MatchAccessService matchAccessService;
    private final MatchEventPublisher eventPublisher;
    private final AuditReporter auditReporter;

    public GoalServiceImpl(GoalRepository goalRepository,
                            MatchRepository matchRepository,
                            MatchAccessService matchAccessService,
                            MatchEventPublisher eventPublisher,
                            AuditReporter auditReporter) {
        this.goalRepository = goalRepository;
        this.matchRepository = matchRepository;
        this.matchAccessService = matchAccessService;
        this.eventPublisher = eventPublisher;
        this.auditReporter = auditReporter;
    }

    @Override
    public GoalResponse registerGoal(UUID matchId, UUID refereeId, RegisterGoalRequest request) {
        Match match = matchAccessService.requireActiveMatch(matchId, refereeId);
        matchAccessService.validateTeamBelongsToMatch(match, request.teamId());

        int minute = request.minute() != null ? request.minute() : MatchClock.currentMinute(match);

        Goal goal = new Goal();
        goal.setMatchId(match.getId());
        goal.setTeamId(request.teamId());
        goal.setPlayerId(request.playerId());
        goal.setMinute(minute);
        goal.setPeriod(match.getCurrentPeriod());
        goal.setCreatedAt(Instant.now());
        goal = goalRepository.save(goal);

        if (request.teamId().equals(match.getHomeTeamId())) {
            match.setHomeScore(match.getHomeScore() + 1);
        } else {
            match.setAwayScore(match.getAwayScore() + 1);
        }
        match.setUpdatedAt(Instant.now());
        matchRepository.save(match);

        eventPublisher.publishGoal(new GoalEventPayload(
                match.getId(), goal.getTeamId(), goal.getPlayerId(), minute, match.getHomeScore(), match.getAwayScore()));

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.GOAL, refereeId, Instant.now(),
                Map.of("teamId", goal.getTeamId(), "playerId", goal.getPlayerId(), "minute", minute)));

        return GoalMapper.toResponse(goal, match.getHomeScore(), match.getAwayScore());
    }

    @Override
    public List<GoalResponse> listGoals(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        List<Goal> goals = goalRepository.findByMatchIdOrderByMinuteAsc(matchId);

        int homeScore = 0;
        int awayScore = 0;
        List<GoalResponse> responses = new ArrayList<>(goals.size());
        for (Goal goal : goals) {
            if (goal.getTeamId().equals(match.getHomeTeamId())) {
                homeScore++;
            } else {
                awayScore++;
            }
            responses.add(GoalMapper.toResponse(goal, homeScore, awayScore));
        }
        return responses;
    }
}
