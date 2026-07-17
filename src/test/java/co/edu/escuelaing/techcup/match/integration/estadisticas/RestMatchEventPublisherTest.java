package co.edu.escuelaing.techcup.match.integration.estadisticas;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RestMatchEventPublisherTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec bodyUriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    private RestMatchEventPublisher publisher;

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
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8082"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8083"));

        publisher = new RestMatchEventPublisher(builder, properties);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
    }

    @Test
    void publishGoal_success_postsToEndpoint() {
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        publisher.publishGoal(new GoalEventPayload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10, 1, 0));

        verify(bodyUriSpec).uri("/api/estadisticas/eventos/gol");
    }

    @Test
    void publishGoal_restClientException_isSwallowed() {
        when(bodySpec.retrieve()).thenThrow(new RestClientException("down"));

        assertThatCode(() -> publisher.publishGoal(
                new GoalEventPayload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 10, 1, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void publishCard_success_postsToEndpoint() {
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        publisher.publishCard(new CardEventPayload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CardType.YELLOW, 20));

        verify(bodyUriSpec).uri("/api/estadisticas/eventos/tarjeta");
    }

    @Test
    void publishCard_restClientException_isSwallowed() {
        when(bodySpec.retrieve()).thenThrow(new RestClientException("down"));

        assertThatCode(() -> publisher.publishCard(
                new CardEventPayload(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CardType.RED, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void publishSubstitution_success_postsToEndpoint() {
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        publisher.publishSubstitution(new SubstitutionEventPayload(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 60));

        verify(bodyUriSpec).uri("/api/estadisticas/eventos/sustitucion");
    }

    @Test
    void publishSubstitution_restClientException_isSwallowed() {
        when(bodySpec.retrieve()).thenThrow(new RestClientException("down"));

        assertThatCode(() -> publisher.publishSubstitution(new SubstitutionEventPayload(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 60)))
                .doesNotThrowAnyException();
    }
}
