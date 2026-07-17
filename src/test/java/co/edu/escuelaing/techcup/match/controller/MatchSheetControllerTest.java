package co.edu.escuelaing.techcup.match.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.escuelaing.techcup.match.config.InternalApiKeyProperties;
import co.edu.escuelaing.techcup.match.config.RefereeSecurityProperties;
import co.edu.escuelaing.techcup.match.config.SecurityConfig;
import co.edu.escuelaing.techcup.match.dto.response.MatchSheetResponse;
import co.edu.escuelaing.techcup.match.security.CurrentRefereeProvider;
import co.edu.escuelaing.techcup.match.security.InternalApiKeyFilter;
import co.edu.escuelaing.techcup.match.security.JwtClaimsFilter;
import co.edu.escuelaing.techcup.match.security.RefereeGuard;
import co.edu.escuelaing.techcup.match.service.MatchSheetService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(controllers = MatchSheetController.class)
@Import({SecurityConfig.class, RefereeGuard.class, JwtClaimsFilter.class, InternalApiKeyFilter.class, CurrentRefereeProvider.class})
@EnableConfigurationProperties({RefereeSecurityProperties.class, InternalApiKeyProperties.class})
@TestPropertySource(properties = {
        "techcup.security.role-claim=roles",
        "techcup.security.referee-role=ARBITRO"
})
class MatchSheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchSheetService matchSheetService;

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
    void uploadSheet_returns201WithCreatedSheet() throws Exception {
        UUID matchId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", "contenido".getBytes());
        when(matchSheetService.uploadSheet(eq(matchId), any(), any())).thenReturn(
                new MatchSheetResponse(UUID.randomUUID(), matchId, "/planillas/acta.pdf", UUID.randomUUID(), Instant.now()));

        mockMvc.perform(multipart("/api/partidos/{matchId}/planilla", matchId)
                        .file(file)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileUrl").value("/planillas/acta.pdf"));
    }

    @Test
    void getSheet_whenUploaded_returns200() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchSheetService.getSheet(eq(matchId), any())).thenReturn(
                new MatchSheetResponse(UUID.randomUUID(), matchId, "/planillas/acta.pdf", UUID.randomUUID(), Instant.now()));

        mockMvc.perform(get("/api/partidos/{matchId}/planilla", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isOk());
    }

    @Test
    void getSheet_whenNotUploaded_returns404() throws Exception {
        UUID matchId = UUID.randomUUID();
        when(matchSheetService.getSheet(eq(matchId), any())).thenReturn(null);

        mockMvc.perform(get("/api/partidos/{matchId}/planilla", matchId)
                        .header("Authorization", "Bearer " + refereeToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSheet_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/partidos/{matchId}/planilla", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadSheet_whenFileCannotBeRead_wrapsIOExceptionAsUnchecked() throws IOException {
        // Prueba unitaria directa (sin MockMvc/multipart real): simula el IOException que
        // MultipartFile.getBytes() puede lanzar si el archivo subido no se puede leer.
        MatchSheetController controller = new MatchSheetController(matchSheetService, mock(CurrentRefereeProvider.class));
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.getOriginalFilename()).thenReturn("acta.pdf");
        when(brokenFile.getContentType()).thenReturn("application/pdf");
        when(brokenFile.getBytes()).thenThrow(new IOException("disco no disponible"));

        assertThrows(UncheckedIOException.class, () -> controller.uploadSheet(UUID.randomUUID(), brokenFile));
    }
}
