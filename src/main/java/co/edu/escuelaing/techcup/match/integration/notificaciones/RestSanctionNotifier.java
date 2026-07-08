package co.edu.escuelaing.techcup.match.integration.notificaciones;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestSanctionNotifier implements SanctionNotifier {

    private static final Logger log = LoggerFactory.getLogger(RestSanctionNotifier.class);

    private final RestClient restClient;

    public RestSanctionNotifier(RestClient.Builder restClientBuilder, IntegrationServicesProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.notificaciones().baseUrl())
                .build();
    }

    @Override
    public void notifyPlayerSanctioned(PlayerSanctionedPayload payload) {
        try {
            restClient.post()
                    .uri("/api/notificaciones/sanciones")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("No fue posible notificar la sanción del jugador {}: {}", payload.playerId(), ex.getMessage());
        }
    }
}
