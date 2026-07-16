package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.config.SanctionProperties;
import co.edu.escuelaing.techcup.match.dto.request.RegisterCardRequest;
import co.edu.escuelaing.techcup.match.dto.response.CardResponse;
import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidTeamException;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.estadisticas.MatchEventPublisher;
import co.edu.escuelaing.techcup.match.integration.notificaciones.SanctionNotifier;
import co.edu.escuelaing.techcup.match.repository.CardRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Cubre la regla de negocio confirmada: 2 amarillas de un jugador en el mismo partido
 * sancionan, y una roja directa sanciona de inmediato (techcup.sanciones.umbral-amarillas-partido).
 */
@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private MatchAccessService matchAccessService;
    @Mock
    private MatchEventPublisher eventPublisher;
    @Mock
    private SanctionNotifier sanctionNotifier;
    @Mock
    private AuditReporter auditReporter;

    private CardServiceImpl cardService;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private Match match;

    @BeforeEach
    void setUp() {
        cardService = new CardServiceImpl(
                cardRepository, matchAccessService, eventPublisher, sanctionNotifier, auditReporter,
                new SanctionProperties(2));

        match = new Match();
        match.setId(matchId);
        match.setHomeTeamId(teamId);
        match.setAwayTeamId(UUID.randomUUID());
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now().minusSeconds(600));

        lenient().when(matchAccessService.requireActiveMatch(matchId, refereeId)).thenReturn(match);
        lenient().when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0);
            card.setId(UUID.randomUUID());
            card.setCreatedAt(Instant.now());
            return card;
        });
    }

    @Test
    void firstYellowCard_doesNotTriggerSanction() {
        when(cardRepository.countByMatchIdAndPlayerIdAndCardType(matchId, playerId, CardType.YELLOW)).thenReturn(1L);

        CardResponse response = cardService.registerCard(
                matchId, refereeId, new RegisterCardRequest(teamId, playerId, CardType.YELLOW, 10));

        assertThat(response.playerSanctioned()).isFalse();
        verify(sanctionNotifier, never()).notifyPlayerSanctioned(any());
    }

    @Test
    void secondYellowCard_triggersSanction() {
        when(cardRepository.countByMatchIdAndPlayerIdAndCardType(matchId, playerId, CardType.YELLOW)).thenReturn(2L);

        CardResponse response = cardService.registerCard(
                matchId, refereeId, new RegisterCardRequest(teamId, playerId, CardType.YELLOW, 75));

        assertThat(response.playerSanctioned()).isTrue();
        verify(sanctionNotifier).notifyPlayerSanctioned(any());
    }

    @Test
    void directRedCard_triggersSanctionImmediately() {
        CardResponse response = cardService.registerCard(
                matchId, refereeId, new RegisterCardRequest(teamId, playerId, CardType.RED, 30));

        assertThat(response.playerSanctioned()).isTrue();
        assertThat(response.cardType()).isEqualTo(CardType.RED);
        verify(sanctionNotifier).notifyPlayerSanctioned(any());
        verify(cardRepository, never()).countByMatchIdAndPlayerIdAndCardType(any(), any(), any());
    }

    @Test
    void teamNotInMatch_throwsInvalidTeamException() {
        UUID otherTeamId = UUID.randomUUID();
        doThrow(new InvalidTeamException(otherTeamId, matchId))
                .when(matchAccessService).validateTeamBelongsToMatch(match, otherTeamId);

        RegisterCardRequest request = new RegisterCardRequest(otherTeamId, playerId, CardType.YELLOW, 10);
        assertThrows(InvalidTeamException.class, () -> cardService.registerCard(matchId, refereeId, request));
    }
}
