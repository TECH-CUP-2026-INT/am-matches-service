package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterGoalRequest;
import co.edu.escuelaing.techcup.match.dto.response.GoalResponse;
import java.util.List;
import java.util.UUID;

public interface GoalService {

    GoalResponse registerGoal(UUID matchId, UUID refereeId, RegisterGoalRequest request);

    List<GoalResponse> listGoals(UUID matchId, UUID refereeId);
}
