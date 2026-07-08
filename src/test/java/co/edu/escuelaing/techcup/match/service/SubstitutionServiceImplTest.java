package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.request.RegisterSubstitutionRequest;
import co.edu.escuelaing.techcup.match.dto.response.SubstitutionResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.Substitution;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidTeamException;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.estadisticas.MatchEventPublisher;
import co.edu.escuelaing.techcup.match.repository.SubstitutionRepository;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubstitutionServiceImplTest {

    @Mock
    private SubstitutionRepository substitutionRepository;
    @Mock
    private MatchAccessService matchAccessService;
    @Mock
    private MatchEventPublisher eventPublisher;
    @Mock
    private AuditReporter auditReporter;

    private SubstitutionServiceImpl substitutionService;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private Match match;

    @BeforeEach
    void setUp() {
        substitutionService = new SubstitutionServiceImpl(
                substitutionRepository, matchAccessService, eventPublisher, auditReporter);

        match = new Match();
        match.setId(matchId);
        match.setHomeTeamId(teamId);
        match.setAwayTeamId(UUID.randomUUID());
        match.setCurrentPeriod(MatchPeriod.SECOND_HALF);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now());
    }

    @Test
    void validSubstitution_isPersistedAndReturned() {
        when(matchAccessService.requireActiveMatch(matchId, refereeId)).thenReturn(match);
        when(substitutionRepository.save(any(Substitution.class))).thenAnswer(invocation -> {
            Substitution substitution = invocation.getArgument(0);
            substitution.setId(UUID.randomUUID());
            substitution.setCreatedAt(Instant.now());
            return substitution;
        });

        UUID playerOut = UUID.randomUUID();
        UUID playerIn = UUID.randomUUID();
        SubstitutionResponse response = substitutionService.registerSubstitution(
                matchId, refereeId, new RegisterSubstitutionRequest(teamId, playerOut, playerIn, 70));

        assertThat(response.playerOutId()).isEqualTo(playerOut);
        assertThat(response.playerInId()).isEqualTo(playerIn);
        assertThat(response.minute()).isEqualTo(70);
    }

    @Test
    void samePlayerInAndOut_throwsValidationException() {
        when(matchAccessService.requireActiveMatch(matchId, refereeId)).thenReturn(match);
        UUID samePlayer = UUID.randomUUID();

        assertThrows(ValidationException.class, () -> substitutionService.registerSubstitution(
                matchId, refereeId, new RegisterSubstitutionRequest(teamId, samePlayer, samePlayer, 70)));
    }

    @Test
    void teamNotInMatch_throwsInvalidTeamException() {
        when(matchAccessService.requireActiveMatch(matchId, refereeId)).thenReturn(match);
        UUID otherTeam = UUID.randomUUID();
        doThrow(new InvalidTeamException(otherTeam, matchId))
                .when(matchAccessService).validateTeamBelongsToMatch(match, otherTeam);

        assertThrows(InvalidTeamException.class, () -> substitutionService.registerSubstitution(
                matchId, refereeId, new RegisterSubstitutionRequest(otherTeam, UUID.randomUUID(), UUID.randomUUID(), 70)));
    }
}
