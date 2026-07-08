package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterObservationRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchObservationResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import co.edu.escuelaing.techcup.match.mapper.MatchObservationMapper;
import co.edu.escuelaing.techcup.match.repository.MatchObservationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las observaciones libres del árbitro no están en la lista de eventos que deben
 * reportarse a Estadísticas ni a Auditoría (solo inicio, gol, tarjeta, sustitución y fin),
 * así que este servicio solo persiste el registro.
 */
@Service
public class MatchObservationServiceImpl implements MatchObservationService {

    private final MatchObservationRepository observationRepository;
    private final MatchAccessService matchAccessService;

    public MatchObservationServiceImpl(MatchObservationRepository observationRepository,
                                        MatchAccessService matchAccessService) {
        this.observationRepository = observationRepository;
        this.matchAccessService = matchAccessService;
    }

    @Override
    @Transactional
    public MatchObservationResponse registerObservation(UUID matchId, UUID refereeId, RegisterObservationRequest request) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);

        MatchObservation observation = new MatchObservation();
        observation.setMatch(match);
        observation.setRefereeId(refereeId);
        observation.setText(request.text());
        observation.setMinute(request.minute());
        observation = observationRepository.save(observation);

        return MatchObservationMapper.toResponse(observation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchObservationResponse> listObservations(UUID matchId, UUID refereeId) {
        matchAccessService.requireOwnedMatch(matchId, refereeId);
        return observationRepository.findByMatchIdOrderByCreatedAtAsc(matchId).stream()
                .map(MatchObservationMapper::toResponse)
                .toList();
    }
}
