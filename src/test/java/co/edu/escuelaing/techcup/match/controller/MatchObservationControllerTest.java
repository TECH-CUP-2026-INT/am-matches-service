package co.edu.escuelaing.techcup.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import co.edu.escuelaing.techcup.match.config.SecurityConfig;
import co.edu.escuelaing.techcup.match.dto.response.MatchObservationResponse;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.security.InternalApiKeyFilter;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.service.MatchObservationService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MatchObservationController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, InternalApiKeyFilter.class, CurrentRefereeProvider.class})
@EnableConfigurationProperties({RefereeSecurityProperties.class, InternalApiKeyProperties.class})
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class MatchObservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchObservationService observationService;

    private String refereeToken() {
        String userId = UUID.randomUUID().toString();
        String payloadJson = "{\"sub\":\"" + userId + "\",\"roles\":[\"ARBITRO\"]}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    @Test
    void registerObservation_returns201WithCreatedObservation() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(observationService.registerObservation(eq(matchId), any(), any())).thenReturn(
                new MatchObservationResponse(UUID.randomUUID(), matchId, UUID.randomUUID(), "Todo en orden", 15,
                        EventType.OBSERVATION, Instant.now()));

        mockMvc.perform(post("/api/partidos/{matchId}/observaciones", matchId)
                        .header("Authorization", "Bearer " + refereeToken())
                        .contentType("application/json")
                        .content("{\"text\":\"Todo en orden\",\"minute\":15}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Todo en orden"));
    }

    @Test
    void listObservations_returnsObservationsForMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(observationService.listObservations(eq(matchId), any())).thenReturn(List.of(
                new MatchObservationResponse(UUID.randomUUID(), matchId, UUID.randomUUID(), "Lluvia fuerte", 30,
                        EventType.OBSERVATION, Instant.now())));

        mockMvc.perform(get("/api/partidos/{matchId}/observaciones", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Lluvia fuerte"));
    }

    @Test
    void listObservations_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/partidos/{matchId}/observaciones", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
