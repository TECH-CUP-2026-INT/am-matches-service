package co.edu.escuelaing.techcup.match.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataMongoTest
class GoalRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7");

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private MatchRepository matchRepository;

    private Match persistMatch() {
        Match match = new Match();
        match.setCompetenciaMatchId(UUID.randomUUID());
        match.setHomeTeamId(UUID.randomUUID());
        match.setAwayTeamId(UUID.randomUUID());
        match.setHomeTeamName("Home FC");
        match.setAwayTeamName("Away FC");
        match.setRefereeId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        return matchRepository.save(match);
    }

    private Goal buildGoal(Match match, int minute) {
        Goal goal = new Goal();
        goal.setMatchId(match.getId());
        goal.setTeamId(match.getHomeTeamId());
        goal.setPlayerId(UUID.randomUUID());
        goal.setMinute(minute);
        goal.setPeriod(MatchPeriod.FIRST_HALF);
        return goal;
    }

    @Test
    void findByMatchIdOrderByMinuteAsc_returnsGoalsSortedByMinute() {
        Match match = persistMatch();
        goalRepository.save(buildGoal(match, 40));
        goalRepository.save(buildGoal(match, 10));
        goalRepository.save(buildGoal(match, 25));

        List<Goal> goals = goalRepository.findByMatchIdOrderByMinuteAsc(match.getId());

        assertThat(goals).extracting(Goal::getMinute).containsExactly(10, 25, 40);
    }

    @Test
    void findByMatchIdOrderByMinuteAsc_noGoals_returnsEmptyList() {
        Match match = persistMatch();

        List<Goal> goals = goalRepository.findByMatchIdOrderByMinuteAsc(match.getId());

        assertThat(goals).isEmpty();
    }

    @Test
    void findByMatchIdOrderByMinuteAsc_doesNotIncludeGoalsFromOtherMatches() {
        Match match = persistMatch();
        Match otherMatch = persistMatch();
        goalRepository.save(buildGoal(match, 5));
        goalRepository.save(buildGoal(otherMatch, 15));

        List<Goal> goals = goalRepository.findByMatchIdOrderByMinuteAsc(match.getId());

        assertThat(goals).hasSize(1);
        assertThat(goals.get(0).getMinute()).isEqualTo(5);
    }
}
