package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.request.AddInjuryTimeRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.service.MatchService;
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
@RequestMapping("/api/partidos")
@PreAuthorize("@refereeGuard.isReferee()")
public class MatchController {

    private final MatchService matchService;
    private final CurrentRefereeProvider currentRefereeProvider;

    public MatchController(MatchService matchService, CurrentRefereeProvider currentRefereeProvider) {
        this.matchService = matchService;
        this.currentRefereeProvider = currentRefereeProvider;
    }

    @GetMapping
    public List<MatchSummaryResponse> listAssignedMatches() {
        return matchService.listAssignedMatches(currentRefereeProvider.getCurrentRefereeId());
    }

    @GetMapping("/{matchId}")
    public MatchResponse getMatch(@PathVariable UUID matchId) {
        return matchService.getMatch(matchId, currentRefereeProvider.getCurrentRefereeId());
    }

    @PostMapping("/{competenciaMatchId}/iniciar")
    public ResponseEntity<MatchResponse> startMatch(@PathVariable UUID competenciaMatchId) {
        MatchResponse response = matchService.startMatch(competenciaMatchId, currentRefereeProvider.getCurrentRefereeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{matchId}/pausar")
    public MatchResponse pauseMatch(@PathVariable UUID matchId) {
        return matchService.pauseMatch(matchId, currentRefereeProvider.getCurrentRefereeId());
    }

    @PostMapping("/{matchId}/reanudar")
    public MatchResponse resumeMatch(@PathVariable UUID matchId) {
        return matchService.resumeMatch(matchId, currentRefereeProvider.getCurrentRefereeId());
    }

    @PostMapping("/{matchId}/siguiente-tiempo")
    public MatchResponse startNextPeriod(@PathVariable UUID matchId) {
        return matchService.startNextPeriod(matchId, currentRefereeProvider.getCurrentRefereeId());
    }

    @PostMapping("/{matchId}/tiempo-adicional")
    public MatchResponse addInjuryTime(@PathVariable UUID matchId, @Valid @RequestBody AddInjuryTimeRequest request) {
        return matchService.addInjuryTime(matchId, currentRefereeProvider.getCurrentRefereeId(), request.minutes());
    }

    @PostMapping("/{matchId}/finalizar")
    public MatchResponse finishMatch(@PathVariable UUID matchId) {
        return matchService.finishMatch(matchId, currentRefereeProvider.getCurrentRefereeId());
    }
}
