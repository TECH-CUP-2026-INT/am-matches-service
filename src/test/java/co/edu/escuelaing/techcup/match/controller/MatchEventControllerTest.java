package co.edu.escuelaing.techcup.match.controller;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import co.edu.escuelaing.techcup.match.config.SecurityConfig;
import co.edu.escuelaing.techcup.match.dto.response.MatchEventResponse;
import co.edu.escuelaing.techcup.match.entity.enums.MatchEventType;
import co.edu.escuelaing.techcup.match.security.AdminOrOrganizadorGuard;
import co.edu.escuelaing.techcup.match.security.InternalApiKeyFilter;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.service.MatchEventQueryService;
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

@WebMvcTest(controllers = MatchEventController.class)
@Import({SecurityConfig.class, AdminOrOrganizadorGuard.class, JwtClaimsFilter.class, InternalApiKeyFilter.class})
@EnableConfigurationProperties({RefereeSecurityProperties.class, InternalApiKeyProperties.class})
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class MatchEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchEventQueryService matchEventQueryService;

    private String buildToken(String rolesJson) {
        String userId = UUID.randomUUID().toString();
        String payloadJson = "{\"sub\":\"" + userId + "\",\"roles\":" + rolesJson + "}";
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".signature";
    }

    @Test
    void listEvents_refereeOnlyToken_returns403() throws Exception {
        String token = buildToken("[\"ARBITRO\"]");

        mockMvc.perform(get("/api/partidos/eventos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEvents_noToken_returns403() throws Exception {
        mockMvc.perform(get("/api/partidos/eventos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEvents_adminToken_returns200() throws Exception {
        String token = buildToken("[\"ADMIN\"]");
        when(matchEventQueryService.listEvents(isNull())).thenReturn(List.of(
                new MatchEventResponse(MatchEventType.GOL, UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), Instant.now(), "Gol al minuto 10")
        ));

        mockMvc.perform(get("/api/partidos/eventos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("GOL"));
    }

    @Test
    void listEvents_organizadorToken_returns200() throws Exception {
        String token = buildToken("[\"ORGANIZADOR\"]");
        when(matchEventQueryService.listEvents(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/partidos/eventos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void listEvents_withMatchIdFilter_passesItToService() throws Exception {
        UUID matchId = UUID.randomUUID();
        String token = buildToken("[\"ADMIN\"]");
        when(matchEventQueryService.listEvents(matchId)).thenReturn(List.of());

        mockMvc.perform(get("/api/partidos/eventos")
                        .header("Authorization", "Bearer " + token)
                        .param("matchId", matchId.toString()))
                .andExpect(status().isOk());
    }
}
