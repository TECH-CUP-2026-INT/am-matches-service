package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterSubstitutionRequest;
import co.edu.escuelaing.techcup.match.dto.response.SubstitutionResponse;
import java.util.List;
import java.util.UUID;

public interface SubstitutionService {

    SubstitutionResponse registerSubstitution(UUID matchId, UUID refereeId, RegisterSubstitutionRequest request);

    List<SubstitutionResponse> listSubstitutions(UUID matchId, UUID refereeId);
}
