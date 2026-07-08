package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.request.RegisterGoalRequest;
import co.edu.escuelaing.techcup.match.dto.response.GoalResponse;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.service.GoalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partidos/{matchId}/goles")
@PreAuthorize("@refereeGuard.isReferee()")
public class GoalController {

    private final GoalService goalService;
    private final CurrentRefereeProvider currentRefereeProvider;

    public GoalController(GoalService goalService, CurrentRefereeProvider currentRefereeProvider) {
        this.goalService = goalService;
        this.currentRefereeProvider = currentRefereeProvider;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> registerGoal(@PathVariable UUID matchId,
                                                       @Valid @RequestBody RegisterGoalRequest request) {
        GoalResponse response = goalService.registerGoal(matchId, currentRefereeProvider.getCurrentRefereeId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<GoalResponse> listGoals(@PathVariable UUID matchId) {
        return goalService.listGoals(matchId, currentRefereeProvider.getCurrentRefereeId());
    }
}
