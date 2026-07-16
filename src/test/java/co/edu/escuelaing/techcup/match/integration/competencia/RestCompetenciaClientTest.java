package co.edu.escuelaing.techcup.match.integration.competencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RestCompetenciaClientTest {

    private RestClient.Builder builder;
    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec uriSpec;
    private RestClient.RequestHeadersSpec headersSpec;
    private RestClient.ResponseSpec responseSpec;

    private RestCompetenciaClient client;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(any(String.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        IntegrationServicesProperties properties = new IntegrationServicesProperties(
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8081"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8082"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8083"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8090"));

        client = new RestCompetenciaClient(builder, properties);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getScheduledMatch_success_returnsBody() {
        UUID competenciaMatchId = UUID.randomUUID();
        ScheduledMatchInfo info = new ScheduledMatchInfo(
                competenciaMatchId, UUID.randomUUID(), UUID.randomUUID(), "Home", "Away",
                java.time.Instant.now(), true);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/partidos/{id}", competenciaMatchId)).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(ScheduledMatchInfo.class)).thenReturn(info);

        ScheduledMatchInfo result = client.getScheduledMatch(competenciaMatchId);

        assertThat(result).isEqualTo(info);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getScheduledMatch_restClientException_wrapsInCompetenciaIntegrationException() {
        UUID competenciaMatchId = UUID.randomUUID();

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/partidos/{id}", competenciaMatchId)).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RestClientException("connection refused"));

        assertThrows(CompetenciaIntegrationException.class, () -> client.getScheduledMatch(competenciaMatchId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAssignedMatches_success_returnsList() {
        UUID refereeId = UUID.randomUUID();
        ScheduledMatchInfo info = new ScheduledMatchInfo(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Home", "Away",
                java.time.Instant.now(), true);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/partidos?arbitroId={refereeId}", refereeId)).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(List.of(info));

        List<ScheduledMatchInfo> result = client.getAssignedMatches(refereeId);

        assertThat(result).containsExactly(info);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAssignedMatches_nullBody_returnsEmptyList() {
        UUID refereeId = UUID.randomUUID();

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/partidos?arbitroId={refereeId}", refereeId)).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(null);

        List<ScheduledMatchInfo> result = client.getAssignedMatches(refereeId);

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAssignedMatches_restClientException_wrapsInCompetenciaIntegrationException() {
        UUID refereeId = UUID.randomUUID();

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/partidos?arbitroId={refereeId}", refereeId)).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RestClientException("timeout"));

        assertThrows(CompetenciaIntegrationException.class, () -> client.getAssignedMatches(refereeId));
    }
}
