package co.edu.escuelaing.techcup.match.exception;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.escuelaing.techcup.match.integration.competencia.CompetenciaIntegrationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExceptionMessagesTest {

    @Test
    void matchNotFoundException_containsMatchId() {
        UUID matchId = UUID.randomUUID();
        MatchNotFoundException ex = new MatchNotFoundException(matchId);
        assertThat(ex.getMessage()).contains(matchId.toString());
    }

    @Test
    void matchAccessDeniedException_containsMatchId() {
        UUID matchId = UUID.randomUUID();
        MatchAccessDeniedException ex = new MatchAccessDeniedException(matchId);
        assertThat(ex.getMessage()).contains(matchId.toString());
    }

    @Test
    void invalidMatchStateException_keepsMessage() {
        InvalidMatchStateException ex = new InvalidMatchStateException("estado inválido");
        assertThat(ex.getMessage()).isEqualTo("estado inválido");
    }

    @Test
    void invalidTeamException_containsTeamAndMatchIds() {
        UUID teamId = UUID.randomUUID();
        UUID matchId = UUID.randomUUID();
        InvalidTeamException ex = new InvalidTeamException(teamId, matchId);
        assertThat(ex.getMessage()).contains(teamId.toString()).contains(matchId.toString());
    }

    @Test
    void matchNotReadyException_keepsMessage() {
        MatchNotReadyException ex = new MatchNotReadyException("no está listo");
        assertThat(ex.getMessage()).isEqualTo("no está listo");
    }

    @Test
    void matchSheetAlreadyExistsException_containsMatchId() {
        UUID matchId = UUID.randomUUID();
        MatchSheetAlreadyExistsException ex = new MatchSheetAlreadyExistsException(matchId);
        assertThat(ex.getMessage()).contains(matchId.toString());
    }

    @Test
    void competenciaIntegrationException_keepsMessageAndCause() {
        Throwable cause = new RuntimeException("boom");
        CompetenciaIntegrationException ex = new CompetenciaIntegrationException("fallo integración", cause);
        assertThat(ex.getMessage()).isEqualTo("fallo integración");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
