package co.edu.escuelaing.techcup.match.controller;

import co.edu.escuelaing.techcup.match.dto.response.MatchEventResponse;
import co.edu.escuelaing.techcup.match.service.MatchEventQueryService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit log local (solo lectura) de eventos de partido, accesible por Admin y Organizador.
 * Distinto del rol árbitro (ver {@link MatchController} y compañía): estos roles solo consultan
 * lo que ya ocurrió, no operan el cronómetro ni registran eventos.
 */
@RestController
@RequestMapping("/api/partidos/eventos")
@PreAuthorize("@adminOrOrganizadorGuard.isAdminOrOrganizador()")
public class MatchEventController {

    private final MatchEventQueryService matchEventQueryService;

    public MatchEventController(MatchEventQueryService matchEventQueryService) {
        this.matchEventQueryService = matchEventQueryService;
    }

    @GetMapping
    public List<MatchEventResponse> listEvents(@RequestParam(required = false) UUID matchId) {
        return matchEventQueryService.listEvents(matchId);
    }
}
