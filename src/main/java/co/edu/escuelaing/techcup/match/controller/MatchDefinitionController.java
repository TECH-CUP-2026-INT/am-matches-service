package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.request.MatchDefinitionRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint servicio-a-servicio: recibe la definición del partido creada por Tournament
 * (matchId, tournamentId, fase, equipos, fecha, hora, árbitro y cancha) y la persiste en
 * SCHEDULED. Protegido por api key interna ({@link co.edu.escuelaing.techcup.match.security.ServiceGuard}),
 * nunca por el rol árbitro: distinto de {@link MatchController}, que expone el arbitraje en
 * vivo sobre un partido que ya fue recibido por acá.
 */
@RestController
@RequestMapping("/api/partidos")
public class MatchDefinitionController {

    private final MatchService matchService;

    public MatchDefinitionController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @PreAuthorize("@serviceGuard.isService()")
    public ResponseEntity<MatchResponse> receiveDefinition(@Valid @RequestBody MatchDefinitionRequest request) {
        MatchResponse response = matchService.receiveMatchDefinition(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
