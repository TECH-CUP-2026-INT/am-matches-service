package co.edu.escuelaing.techcup.match.integration.competencia;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import java.util.List;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementación síncrona vía REST. Al estar detrás del puerto {@link CompetenciaClient},
 * puede reemplazarse por un cliente basado en eventos sin tocar la capa de servicio.
 */
@Component
public class RestCompetenciaClient implements CompetenciaClient {

    private final RestClient restClient;

    public RestCompetenciaClient(RestClient.Builder restClientBuilder, IntegrationServicesProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.competencia().baseUrl())
                .build();
    }

    @Override
    public ScheduledMatchInfo getScheduledMatch(UUID competenciaMatchId) {
        try {
            return restClient.get()
                    .uri("/api/partidos/{id}", competenciaMatchId)
                    .retrieve()
                    .body(ScheduledMatchInfo.class);
        } catch (RestClientException ex) {
            throw new CompetenciaIntegrationException(
                    "No fue posible obtener el partido programado " + competenciaMatchId
                            + " desde el Servicio de Competencia", ex);
        }
    }

    @Override
    public List<ScheduledMatchInfo> getAssignedMatches(UUID refereeId) {
        try {
            List<ScheduledMatchInfo> matches = restClient.get()
                    .uri("/api/partidos?arbitroId={refereeId}", refereeId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ScheduledMatchInfo>>() {
                    });
            return matches != null ? matches : List.of();
        } catch (RestClientException ex) {
            throw new CompetenciaIntegrationException(
                    "No fue posible obtener los partidos asignados al árbitro " + refereeId
                            + " desde el Servicio de Competencia", ex);
        }
    }
}
