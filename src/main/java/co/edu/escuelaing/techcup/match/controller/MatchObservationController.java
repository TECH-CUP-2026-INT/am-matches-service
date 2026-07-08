package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.request.RegisterObservationRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchObservationResponse;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.service.MatchObservationService;
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
@RequestMapping("/api/partidos/{matchId}/observaciones")
@PreAuthorize("@refereeGuard.isReferee()")
public class MatchObservationController {

    private final MatchObservationService observationService;
    private final CurrentRefereeProvider currentRefereeProvider;

    public MatchObservationController(MatchObservationService observationService,
                                       CurrentRefereeProvider currentRefereeProvider) {
        this.observationService = observationService;
        this.currentRefereeProvider = currentRefereeProvider;
    }

    @PostMapping
    public ResponseEntity<MatchObservationResponse> registerObservation(@PathVariable UUID matchId,
                                                                          @Valid @RequestBody RegisterObservationRequest request) {
        MatchObservationResponse response = observationService.registerObservation(
                matchId, currentRefereeProvider.getCurrentRefereeId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<MatchObservationResponse> listObservations(@PathVariable UUID matchId) {
        return observationService.listObservations(matchId, currentRefereeProvider.getCurrentRefereeId());
    }
}
