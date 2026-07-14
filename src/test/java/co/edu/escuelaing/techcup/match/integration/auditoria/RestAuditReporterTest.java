package co.edu.escuelaing.techcup.match.integration.auditoria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RestAuditReporterTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec bodyUriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    private RestAuditReporter reporter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(any(String.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        IntegrationServicesProperties properties = new IntegrationServicesProperties(
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8081"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8082"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8083"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8084"));

        reporter = new RestAuditReporter(builder, properties);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
    }

    @Test
    void report_success_postsToEndpoint() {
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        reporter.report(new MatchAuditEvent(UUID.randomUUID(), EventType.GOAL, UUID.randomUUID(), Instant.now(), Map.of()));

        verify(bodyUriSpec).uri(eq("/api/auditoria/eventos"));
    }

    @Test
    void report_restClientException_isSwallowed() {
        when(bodySpec.retrieve()).thenThrow(new RestClientException("down"));

        reporter.report(new MatchAuditEvent(UUID.randomUUID(), EventType.MATCH_STARTED, UUID.randomUUID(), Instant.now(), Map.of()));
        // no exception propagated
    }
}
