package co.edu.escuelaing.techcup.match.integration.notificaciones;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RestSanctionNotifierTest {

    private RestClient restClient;
    private RestClient.RequestBodyUriSpec bodyUriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private RestClient.Builder builder;

    private RestSanctionNotifier notifier;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(any(String.class))).thenReturn(builder);
        when(builder.defaultHeader(any(String.class), any(String.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        IntegrationServicesProperties properties = new IntegrationServicesProperties(
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8081"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8082"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8083"),
                new IntegrationServicesProperties.ServiceEndpoint("http://localhost:8090"));

        notifier = new RestSanctionNotifier(builder, properties, new InternalApiKeyProperties("secret-key"));

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(any(String.class))).thenReturn(bodySpec);
        when(bodySpec.body(any(Object.class))).thenReturn(bodySpec);
    }

    @Test
    void constructor_setsInternalApiKeyDefaultHeader() {
        verify(builder).defaultHeader("X-Internal-Api-Key", "secret-key");
    }

    @Test
    void notifyPlayerSanctioned_success_postsToEndpoint() {
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        notifier.notifyPlayerSanctioned(new PlayerSanctionedPayload(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CardType.YELLOW, 2, Instant.now()));

        verify(bodyUriSpec).uri("/api/notificaciones/sanciones");
    }

    @Test
    void notifyPlayerSanctioned_restClientException_isSwallowed() {
        when(bodySpec.retrieve()).thenThrow(new RestClientException("down"));

        assertThatCode(() -> notifier.notifyPlayerSanctioned(new PlayerSanctionedPayload(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CardType.RED, 1, Instant.now())))
                .doesNotThrowAnyException();
    }
}
