package co.edu.escuelaing.techcup.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import co.edu.escuelaing.techcup.match.config.SecurityConfig;
import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.security.InternalApiKeyFilter;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.security.ServiceGuard;
import co.edu.escuelaing.techcup.match.service.MatchService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MatchDefinitionController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, InternalApiKeyFilter.class,
        ServiceGuard.class})
@EnableConfigurationProperties({RefereeSecurityProperties.class, InternalApiKeyProperties.class})
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO",
        "techcup.security.internal.api-key=test-internal-key"
})
class MatchDefinitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    private String definitionJson() {
        return "{"
                + "\"matchId\":\"" + UUID.randomUUID() + "\","
                + "\"tournamentId\":\"" + UUID.randomUUID() + "\","
                + "\"fase\":\"GRUPOS\","
                + "\"equipoAId\":\"" + UUID.randomUUID() + "\","
                + "\"equipoBId\":\"" + UUID.randomUUID() + "\","
                + "\"equipoANombre\":\"Local\","
                + "\"equipoBNombre\":\"Visitante\","
                + "\"fecha\":\"2026-08-01\","
                + "\"hora\":\"15:00:00\","
                + "\"arbitroId\":\"" + UUID.randomUUID() + "\","
                + "\"canchaId\":\"" + UUID.randomUUID() + "\""
                + "}";
    }

    private MatchResponse sampleResponse() {
        return new MatchResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), MatchPhase.GRUPOS,
                UUID.randomUUID(), UUID.randomUUID(), "Local", "Visitante", UUID.randomUUID(),
                MatchStatus.SCHEDULED, MatchPeriod.FIRST_HALF, 0, 0, 0, 0, 0, Instant.now(), null, null);
    }

    @Test
    void receiveDefinition_withValidInternalApiKey_returns201() throws Exception {
        when(matchService.receiveMatchDefinition(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/partidos")
                        .header(InternalApiKeyFilter.HEADER_NAME, "test-internal-key")
                        .contentType("application/json")
                        .content(definitionJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void receiveDefinition_withoutInternalApiKey_returns403() throws Exception {
        mockMvc.perform(post("/api/partidos")
                        .contentType("application/json")
                        .content(definitionJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiveDefinition_withRefereeJwtInsteadOfInternalKey_returns403() throws Exception {
        String userId = UUID.randomUUID().toString();
        String payloadJson = "{\"sub\":\"" + userId + "\",\"roles\":[\"ARBITRO\"]}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String token = header + "." + payload + ".signature";

        mockMvc.perform(post("/api/partidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(definitionJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void receiveDefinition_withWrongInternalApiKey_returns403() throws Exception {
        mockMvc.perform(post("/api/partidos")
                        .header(InternalApiKeyFilter.HEADER_NAME, "clave-incorrecta")
                        .contentType("application/json")
                        .content(definitionJson()))
                .andExpect(status().isForbidden());
    }
}
