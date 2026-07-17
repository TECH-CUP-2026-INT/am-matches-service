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
import co.edu.escuelaing.techcup.match.dto.response.SubstitutionResponse;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.security.InternalApiKeyFilter;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.service.SubstitutionService;
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

@WebMvcTest(controllers = SubstitutionController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, InternalApiKeyFilter.class, CurrentRefereeProvider.class})
@EnableConfigurationProperties({RefereeSecurityProperties.class, InternalApiKeyProperties.class})
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class SubstitutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubstitutionService substitutionService;

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
    void registerSubstitution_returns201WithCreatedSubstitution() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID playerOutId = UUID.randomUUID();
        UUID playerInId = UUID.randomUUID();
        when(substitutionService.registerSubstitution(eq(matchId), any(), any())).thenReturn(new SubstitutionResponse(
                UUID.randomUUID(), matchId, teamId, playerOutId, playerInId, 65, MatchPeriod.SECOND_HALF,
                EventType.SUBSTITUTION, Instant.now()));

        mockMvc.perform(post("/api/partidos/{matchId}/sustituciones", matchId)
                        .header("Authorization", "Bearer " + refereeToken())
                        .contentType("application/json")
                        .content("{\"teamId\":\"" + teamId + "\",\"playerOutId\":\"" + playerOutId
                                + "\",\"playerInId\":\"" + playerInId + "\",\"minute\":65}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minute").value(65));
    }

    @Test
    void listSubstitutions_returnsSubstitutionsForMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(substitutionService.listSubstitutions(eq(matchId), any())).thenReturn(List.of(new SubstitutionResponse(
                UUID.randomUUID(), matchId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 70,
                MatchPeriod.SECOND_HALF, EventType.SUBSTITUTION, Instant.now())));

        mockMvc.perform(get("/api/partidos/{matchId}/sustituciones", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].minute").value(70));
    }

    @Test
    void listSubstitutions_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/partidos/{matchId}/sustituciones", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
