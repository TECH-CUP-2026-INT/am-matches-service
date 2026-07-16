package co.edu.escuelaing.techcup.match.integration.torneos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties;
import co.edu.escuelaing.techcup.match.config.IntegrationServicesProperties.ServiceEndpoint;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RestTournamentClientTest {

    private static final String BASE_URL = "http://torneos.test";

    private RestTournamentClient buildClient(MockRestServiceServer[] serverHolder) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        serverHolder[0] = server;
        IntegrationServicesProperties properties =
                new IntegrationServicesProperties(null, null, null, null, new ServiceEndpoint(BASE_URL));
        return new RestTournamentClient(builder, properties);
    }

    @Test
    void findTournamentIdForMatch_whenTournamentExists_returnsItsId() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestTournamentClient client = buildClient(holder);
        UUID matchId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();

        holder[0].expect(requestTo(BASE_URL + "/tournaments/matches/" + matchId + "/tournament"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"matchId\":\"" + matchId + "\",\"tournamentId\":\"" + tournamentId + "\"}",
                        MediaType.APPLICATION_JSON));

        Optional<UUID> result = client.findTournamentIdForMatch(matchId);

        assertThat(result).contains(tournamentId);
    }

    @Test
    void findTournamentIdForMatch_whenMatchIsNotRegisteredInTournaments_returnsEmpty() {
        MockRestServiceServer[] holder = new MockRestServiceServer[1];
        RestTournamentClient client = buildClient(holder);
        UUID matchId = UUID.randomUUID();

        holder[0].expect(requestTo(BASE_URL + "/tournaments/matches/" + matchId + "/tournament"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.findTournamentIdForMatch(matchId)).isEmpty();
    }
}
