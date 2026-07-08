package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterCardRequest;
import co.edu.escuelaing.techcup.match.dto.response.CardResponse;
import java.util.List;
import java.util.UUID;

public interface CardService {

    CardResponse registerCard(UUID matchId, UUID refereeId, RegisterCardRequest request);

    List<CardResponse> listCards(UUID matchId, UUID refereeId);
}
