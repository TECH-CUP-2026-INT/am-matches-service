package co.edu.escuelaing.techcup.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import co.edu.escuelaing.techcup.match.config.SecurityConfig;
import co.edu.escuelaing.techcup.match.dto.response.GoalResponse;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.service.GoalService;
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

@WebMvcTest(controllers = GoalController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, CurrentRefereeProvider.class})
@EnableConfigurationProperties(RefereeSecurityProperties.class)
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

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
    void registerGoal_returns201WithCreatedGoal() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(goalService.registerGoal(eq(matchId), any(), any())).thenReturn(new GoalResponse(
                UUID.randomUUID(), matchId, teamId, playerId, 20, MatchPeriod.FIRST_HALF, 1, 0,
                EventType.GOAL, Instant.now()));

        mockMvc.perform(post("/api/partidos/{matchId}/goles", matchId)
                        .header("Authorization", "Bearer " + refereeToken())
                        .contentType("application/json")
                        .content("{\"teamId\":\"" + teamId + "\",\"playerId\":\"" + playerId + "\",\"minute\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeScore").value(1));
    }

    @Test
    void listGoals_returnsGoalsForMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(goalService.listGoals(eq(matchId), any())).thenReturn(List.of(new GoalResponse(
                UUID.randomUUID(), matchId, UUID.randomUUID(), UUID.randomUUID(), 55, MatchPeriod.SECOND_HALF, 1, 1,
                EventType.GOAL, Instant.now())));

        mockMvc.perform(get("/api/partidos/{matchId}/goles", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].awayScore").value(1));
    }

    @Test
    void listGoals_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/partidos/{matchId}/goles", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }
}
