package co.edu.escuelaing.techcup.match.integration.torneos;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementación síncrona vía REST, best-effort: si Torneos no responde o el partido no
 * está registrado allá (404 → RestClientResponseException), no debe bloquear ni tumbar
 * la publicación de estadísticas — el evento sale igual con tournamentId null (ver
 * MatchFinishedStatPublisher).
 */
@Component
public class RestTournamentClient implements TournamentClient {

    private static final Logger log = LoggerFactory.getLogger(RestTournamentClient.class);

    private final RestClient restClient;

    public RestTournamentClient(RestClient.Builder restClientBuilder, IntegrationServicesProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.torneos().baseUrl())
                .build();
    }

    @Override
    public Optional<UUID> findTournamentIdForMatch(UUID matchId) {
        try {
            MatchTournamentResponse response = restClient.get()
                    .uri("/tournaments/matches/{matchId}/tournament", matchId)
                    .retrieve()
                    .body(MatchTournamentResponse.class);
            return response == null || response.tournamentId() == null
                    ? Optional.empty()
                    : Optional.of(UUID.fromString(response.tournamentId()));
        } catch (RestClientException | IllegalArgumentException ex) {
            log.warn("No fue posible obtener el tournamentId del partido '{}' desde el Servicio de Torneos: {}",
                    matchId, ex.getMessage());
            return Optional.empty();
        }
    }

    private record MatchTournamentResponse(String matchId, String tournamentId) {
    }
}
