package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterObservationRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchObservationResponse;
import java.util.List;
import java.util.UUID;

public interface MatchObservationService {

    MatchObservationResponse registerObservation(UUID matchId, UUID refereeId, RegisterObservationRequest request);

    List<MatchObservationResponse> listObservations(UUID matchId, UUID refereeId);
}
