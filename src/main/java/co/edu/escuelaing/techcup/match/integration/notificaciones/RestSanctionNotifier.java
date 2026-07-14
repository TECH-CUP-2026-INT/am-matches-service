package co.edu.escuelaing.techcup.match.integration.notificaciones;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestSanctionNotifier implements SanctionNotifier {

    /** Debe coincidir con InternalApiKeyFilter.HEADER_NAME del servicio de notificaciones. */
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private static final Logger log = LoggerFactory.getLogger(RestSanctionNotifier.class);

    private final RestClient restClient;

    public RestSanctionNotifier(
            RestClient.Builder restClientBuilder,
            IntegrationServicesProperties properties,
            InternalApiKeyProperties internalApiKeyProperties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.notificaciones().baseUrl())
                .defaultHeader(INTERNAL_API_KEY_HEADER, internalApiKeyProperties.apiKey())
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
