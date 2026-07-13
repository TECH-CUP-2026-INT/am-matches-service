package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.response.MatchEventResponse;
import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import co.edu.escuelaing.techcup.match.entity.Substitution;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchEventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.repository.CardRepository;
import co.edu.escuelaing.techcup.match.repository.GoalRepository;
import co.edu.escuelaing.techcup.match.repository.MatchObservationRepository;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import co.edu.escuelaing.techcup.match.repository.SubstitutionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchEventQueryServiceImplTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private GoalRepository goalRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private SubstitutionRepository substitutionRepository;
    @Mock
    private MatchObservationRepository matchObservationRepository;

    private MatchEventQueryServiceImpl service;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private final UUID playerId = UUID.randomUUID();
    private Match match;
    private Instant now;

    @BeforeEach
    void setUp() {
        service = new MatchEventQueryServiceImpl(
                matchRepository, goalRepository, cardRepository, substitutionRepository, matchObservationRepository);

        now = Instant.now();
        match = new Match();
        match.setId(matchId);
        match.setRefereeId(refereeId);
        match.setHomeTeamName("Local");
        match.setAwayTeamName("Visitante");
        match.setHomeScore(2);
        match.setAwayScore(1);
        match.setStatus(MatchStatus.FINISHED);
        match.setStartedAt(now.minus(90, ChronoUnit.MINUTES));
        match.setEndedAt(now);
    }

    @Test
    void listEvents_withMatchIdFilter_mergesAndSortsDescendingByTimestamp() {
        Goal goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setMatch(match);
        goal.setPlayerId(playerId);
        goal.setMinute(10);
        goal.setPeriod(MatchPeriod.FIRST_HALF);
        goal.setCreatedAt(now.minus(80, ChronoUnit.MINUTES));

        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setMatch(match);
        card.setPlayerId(playerId);
        card.setCardType(CardType.YELLOW);
        card.setMinute(20);
        card.setPeriod(MatchPeriod.FIRST_HALF);
        card.setCreatedAt(now.minus(70, ChronoUnit.MINUTES));

        Substitution substitution = new Substitution();
        substitution.setId(UUID.randomUUID());
        substitution.setMatch(match);
        substitution.setPlayerInId(UUID.randomUUID());
        substitution.setPlayerOutId(UUID.randomUUID());
        substitution.setMinute(60);
        substitution.setPeriod(MatchPeriod.SECOND_HALF);
        substitution.setCreatedAt(now.minus(30, ChronoUnit.MINUTES));

        MatchObservation observation = new MatchObservation();
        observation.setId(UUID.randomUUID());
        observation.setMatch(match);
        observation.setRefereeId(refereeId);
        observation.setText("Todo en orden");
        observation.setCreatedAt(now.minus(20, ChronoUnit.MINUTES));

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(goalRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of(goal));
        when(cardRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of(card));
        when(substitutionRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of(substitution));
        when(matchObservationRepository.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of(observation));

        List<MatchEventResponse> events = service.listEvents(matchId);

        // 2 lifecycle events (started/ended) + goal + card + substitution + observation = 6
        assertThat(events).hasSize(6);
        assertThat(events).isSortedAccordingTo((a, b) -> b.timestamp().compareTo(a.timestamp()));
        assertThat(events.get(0).tipo()).isEqualTo(MatchEventType.PARTIDO_FINALIZADO);
        assertThat(events).extracting(MatchEventResponse::tipo)
                .contains(MatchEventType.PARTIDO_INICIADO, MatchEventType.PARTIDO_FINALIZADO,
                        MatchEventType.GOL, MatchEventType.TARJETA,
                        MatchEventType.SUSTITUCION, MatchEventType.OBSERVACION);
        assertThat(events).allSatisfy(event -> assertThat(event.matchId()).isEqualTo(matchId));
    }

    @Test
    void listEvents_noMatchId_queriesAcrossAllMatches() {
        when(matchRepository.findAll()).thenReturn(List.of(match));
        when(goalRepository.findAll()).thenReturn(List.of());
        when(cardRepository.findAll()).thenReturn(List.of());
        when(substitutionRepository.findAll()).thenReturn(List.of());
        when(matchObservationRepository.findAll()).thenReturn(List.of());

        List<MatchEventResponse> events = service.listEvents(null);

        assertThat(events).hasSize(2);
        assertThat(events).extracting(MatchEventResponse::tipo)
                .containsExactlyInAnyOrder(MatchEventType.PARTIDO_INICIADO, MatchEventType.PARTIDO_FINALIZADO);
    }

    @Test
    void listEvents_matchNotFound_returnsEmptyList() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());
        when(goalRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of());
        when(cardRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of());
        when(substitutionRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of());
        when(matchObservationRepository.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of());

        List<MatchEventResponse> events = service.listEvents(matchId);

        assertThat(events).isEmpty();
    }

    @Test
    void listEvents_matchWithoutLifecycleTimestamps_producesNoLifecycleEvents() {
        Match scheduled = new Match();
        scheduled.setId(matchId);
        scheduled.setRefereeId(refereeId);
        scheduled.setStatus(MatchStatus.SCHEDULED);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(scheduled));
        when(goalRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of());
        when(cardRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of());
        when(substitutionRepository.findByMatchIdOrderByMinuteAsc(matchId)).thenReturn(List.of());
        when(matchObservationRepository.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of());

        List<MatchEventResponse> events = service.listEvents(matchId);

        assertThat(events).isEmpty();
    }
}
