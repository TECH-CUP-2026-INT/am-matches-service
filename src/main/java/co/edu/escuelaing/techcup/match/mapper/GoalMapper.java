package co.edu.escuelaing.techcup.match.mapper;

import co.edu.escuelaing.techcup.match.dto.response.GoalResponse;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;

public final class GoalMapper {

    private GoalMapper() {
    }

    public static GoalResponse toResponse(Goal goal, int homeScore, int awayScore) {
        return new GoalResponse(
                goal.getId(),
                goal.getMatch().getId(),
                goal.getTeamId(),
                goal.getPlayerId(),
                goal.getMinute(),
                goal.getPeriod(),
                homeScore,
                awayScore,
                EventType.GOAL,
                goal.getCreatedAt()
        );
    }
}
