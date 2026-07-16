package co.edu.escuelaing.techcup.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import co.edu.escuelaing.techcup.match.config.SecurityConfig;
import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.service.MatchService;
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

@WebMvcTest(controllers = MatchController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, CurrentRefereeProvider.class})
@EnableConfigurationProperties(RefereeSecurityProperties.class)
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    private String refereeToken() {
        String userId = UUID.randomUUID().toString();
        String payloadJson = "{\"sub\":\"" + userId + "\",\"roles\":[\"ARBITRO\"]}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    private MatchResponse sampleResponse(MatchStatus status, MatchPeriod period) {
        return new MatchResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Local", "Visitante", UUID.randomUUID(), status, period, 0, 0, 0, 0, 10,
                Instant.now(), null, EventType.MATCH_STARTED);
    }

    @Test
    void listAssignedMatches_returnsSummaries() throws Exception {
        when(matchService.listAssignedMatches(any())).thenReturn(List.of(
                new MatchSummaryResponse(UUID.randomUUID(), UUID.randomUUID(), "Local", "Visitante",
                        MatchStatus.SCHEDULED, false, 0, 0)));

        mockMvc.perform(get("/api/partidos").header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].homeTeamName").value("Local"));
    }

    @Test
    void getMatch_returnsMatchDetail() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.getMatch(eq(matchId), any())).thenReturn(sampleResponse(MatchStatus.IN_PROGRESS, MatchPeriod.FIRST_HALF));

        mockMvc.perform(get("/api/partidos/{matchId}", matchId).header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void startMatch_returns201() throws Exception {
        UUID competenciaMatchId = UUID.randomUUID();
        when(matchService.startMatch(eq(competenciaMatchId), any()))
                .thenReturn(sampleResponse(MatchStatus.IN_PROGRESS, MatchPeriod.FIRST_HALF));

        mockMvc.perform(post("/api/partidos/{competenciaMatchId}/iniciar", competenciaMatchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isCreated());
    }

    @Test
    void pauseMatch_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.pauseMatch(eq(matchId), any())).thenReturn(sampleResponse(MatchStatus.PAUSED, MatchPeriod.FIRST_HALF));

        mockMvc.perform(post("/api/partidos/{matchId}/pausar", matchId).header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void resumeMatch_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.resumeMatch(eq(matchId), any())).thenReturn(sampleResponse(MatchStatus.IN_PROGRESS, MatchPeriod.FIRST_HALF));

        mockMvc.perform(post("/api/partidos/{matchId}/reanudar", matchId).header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk());
    }

    @Test
    void startNextPeriod_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.startNextPeriod(eq(matchId), any()))
                .thenReturn(sampleResponse(MatchStatus.IN_PROGRESS, MatchPeriod.SECOND_HALF));

        mockMvc.perform(post("/api/partidos/{matchId}/siguiente-tiempo", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriod").value("SECOND_HALF"));
    }

    @Test
    void addInjuryTime_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.addInjuryTime(eq(matchId), any(), anyInt()))
                .thenReturn(sampleResponse(MatchStatus.IN_PROGRESS, MatchPeriod.SECOND_HALF));

        mockMvc.perform(post("/api/partidos/{matchId}/tiempo-adicional", matchId)
                        .header("Authorization", "Bearer " + refereeToken())
                        .contentType("application/json")
                        .content("{\"minutes\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    void finishMatch_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchService.finishMatch(eq(matchId), any())).thenReturn(sampleResponse(MatchStatus.FINISHED, MatchPeriod.SECOND_HALF));

        mockMvc.perform(post("/api/partidos/{matchId}/finalizar", matchId).header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void listAssignedMatches_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/partidos")).andExpect(status().isForbidden());
    }
}
