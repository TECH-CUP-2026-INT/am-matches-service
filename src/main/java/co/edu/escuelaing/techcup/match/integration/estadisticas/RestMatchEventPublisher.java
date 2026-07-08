package co.edu.escuelaing.techcup.match.integration.estadisticas;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementación síncrona vía REST, best-effort: un fallo en Estadísticas nunca debe
 * bloquear ni revertir el registro del evento en el Servicio de Partidos.
 */
@Component
public class RestMatchEventPublisher implements MatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestMatchEventPublisher.class);

    private final RestClient restClient;

    public RestMatchEventPublisher(RestClient.Builder restClientBuilder, IntegrationServicesProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.estadisticas().baseUrl())
                .build();
    }

    @Override
    public void publishGoal(GoalEventPayload payload) {
        post("/api/estadisticas/eventos/gol", payload);
    }

    @Override
    public void publishCard(CardEventPayload payload) {
        post("/api/estadisticas/eventos/tarjeta", payload);
    }

    @Override
    public void publishSubstitution(SubstitutionEventPayload payload) {
        post("/api/estadisticas/eventos/sustitucion", payload);
    }

    private void post(String uri, Object payload) {
        try {
            restClient.post().uri(uri).body(payload).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("No fue posible notificar al Servicio de Estadísticas en {}: {}", uri, ex.getMessage());
        }
    }
}
