package co.edu.escuelaing.techcup.match.integration.auditoria;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestAuditReporter implements AuditReporter {

    private static final Logger log = LoggerFactory.getLogger(RestAuditReporter.class);

    private final RestClient restClient;

    public RestAuditReporter(RestClient.Builder restClientBuilder, IntegrationServicesProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.auditoria().baseUrl())
                .build();
    }

    @Override
    public void report(MatchAuditEvent event) {
        try {
            restClient.post()
                    .uri("/api/auditoria/eventos")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("No fue posible reportar a Auditoría el evento {} del partido {}: {}",
                    event.eventType(), event.matchId(), ex.getMessage());
        }
    }
}
