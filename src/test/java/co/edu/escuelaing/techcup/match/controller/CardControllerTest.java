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
import co.edu.escuelaing.techcup.match.dto.response.CardResponse;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.service.CardService;
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

@WebMvcTest(controllers = CardController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, CurrentRefereeProvider.class})
@EnableConfigurationProperties(RefereeSecurityProperties.class)
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

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
    void registerCard_returns201WithCreatedCard() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        when(cardService.registerCard(eq(matchId), any(), any())).thenReturn(new CardResponse(
                UUID.randomUUID(), matchId, teamId, playerId, CardType.YELLOW, "amarillo", 10,
                MatchPeriod.FIRST_HALF, EventType.YELLOW_CARD, false, Instant.now()));

        mockMvc.perform(post("/api/partidos/{matchId}/tarjetas", matchId)
                        .header("Authorization", "Bearer " + refereeToken())
                        .contentType("application/json")
                        .content("{\"teamId\":\"" + teamId + "\",\"playerId\":\"" + playerId
                                + "\",\"cardType\":\"YELLOW\",\"minute\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardType").value("YELLOW"));
    }

    @Test
    void listCards_returnsCardsForMatch() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(cardService.listCards(eq(matchId), any())).thenReturn(List.of(new CardResponse(
                        UUID.randomUUID(), matchId, UUID.randomUUID(), UUID.randomUUID(), CardType.RED, "rojo", 30,
                        MatchPeriod.SECOND_HALF, EventType.RED_CARD, true, Instant.now())));

        mockMvc.perform(get("/api/partidos/{matchId}/tarjetas", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardType").value("RED"));
    }

    @Test
    void registerCard_withoutToken_returns403() throws Exception {
        UUID matchId = UUID.randomUUID();

        mockMvc.perform(post("/api/partidos/{matchId}/tarjetas", matchId)
                        .contentType("application/json")
                        .content("{\"teamId\":\"" + UUID.randomUUID() + "\",\"playerId\":\"" + UUID.randomUUID()
                                + "\",\"cardType\":\"YELLOW\",\"minute\":10}"))
                .andExpect(status().isForbidden());
    }
}
