package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.request.RegisterCardRequest;
import co.edu.escuelaing.techcup.match.dto.response.CardResponse;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.service.CardService;
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
@RequestMapping("/api/partidos/{matchId}/tarjetas")
@PreAuthorize("@refereeGuard.isReferee()")
public class CardController {

    private final CardService cardService;
    private final CurrentRefereeProvider currentRefereeProvider;

    public CardController(CardService cardService, CurrentRefereeProvider currentRefereeProvider) {
        this.cardService = cardService;
        this.currentRefereeProvider = currentRefereeProvider;
    }

    @PostMapping
    public ResponseEntity<CardResponse> registerCard(@PathVariable UUID matchId,
                                                       @Valid @RequestBody RegisterCardRequest request) {
        CardResponse response = cardService.registerCard(matchId, currentRefereeProvider.getCurrentRefereeId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<CardResponse> listCards(@PathVariable UUID matchId) {
        return cardService.listCards(matchId, currentRefereeProvider.getCurrentRefereeId());
    }
}
