package co.edu.escuelaing.techcup.match.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.Goal;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchResult;
import co.edu.escuelaing.techcup.match.integration.torneos.TournamentClient;
import co.edu.escuelaing.techcup.match.repository.CardRepository;
import co.edu.escuelaing.techcup.match.repository.GoalRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MatchFinishedStatPublisherTest {

    private final GoalRepository goalRepository = mock(GoalRepository.class);
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final MatchStatEventPublisher publisher = mock(MatchStatEventPublisher.class);
    private final TournamentClient tournamentClient = mock(TournamentClient.class);
    private final MatchFinishedStatPublisher statPublisher =
            new MatchFinishedStatPublisher(goalRepository, cardRepository, publisher, tournamentClient);

    private Match matchWonByHome() {
        UUID homeTeamId = UUID.randomUUID();
        UUID awayTeamId = UUID.randomUUID();
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setHomeScore(2);
        match.setAwayScore(1);
        return match;
    }

    @Test
    void publishStatsFor_playerWithGoalsAndCards_aggregatesAndPublishesOncePerPlayer() {
        Match match = matchWonByHome();
        UUID playerId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        when(tournamentClient.findTournamentIdForMatch(match.getId())).thenReturn(Optional.of(tournamentId));

        Goal goal1 = new Goal();
        goal1.setPlayerId(playerId);
        goal1.setTeamId(match.getHomeTeamId());
        Goal goal2 = new Goal();
        goal2.setPlayerId(playerId);
        goal2.setTeamId(match.getHomeTeamId());
        when(goalRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of(goal1, goal2));

        Card yellow = new Card();
        yellow.setPlayerId(playerId);
        yellow.setTeamId(match.getHomeTeamId());
        yellow.setCardType(CardType.YELLOW);
        when(cardRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of(yellow));

        statPublisher.publishStatsFor(match);

        ArgumentCaptor<MatchStatEvent> captor = ArgumentCaptor.forClass(MatchStatEvent.class);
        verify(publisher, times(1)).publish(captor.capture());
        MatchStatEvent event = captor.getValue();
        assertThat(event.playerId()).isEqualTo(playerId);
        assertThat(event.teamId()).isEqualTo(match.getHomeTeamId());
        assertThat(event.goals()).isEqualTo(2);
        assertThat(event.yellowCards()).isEqualTo(1);
        assertThat(event.redCards()).isZero();
        assertThat(event.result()).isEqualTo(MatchResult.WON);
        assertThat(event.tournamentId()).isEqualTo(tournamentId);
        assertThat(event.foulsCommitted()).isZero();
        assertThat(event.minutesPlayed()).isZero();
        assertThat(event.assists()).isZero();
        assertThat(event.goalkeeper()).isFalse();
    }

    @Test
    void publishStatsFor_whenTournamentLookupFails_tournamentIdIsNullButEventStillPublishes() {
        Match match = matchWonByHome();
        UUID playerId = UUID.randomUUID();
        when(tournamentClient.findTournamentIdForMatch(match.getId())).thenReturn(Optional.empty());

        Goal goal = new Goal();
        goal.setPlayerId(playerId);
        goal.setTeamId(match.getHomeTeamId());
        when(goalRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of(goal));
        when(cardRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of());

        statPublisher.publishStatsFor(match);

        ArgumentCaptor<MatchStatEvent> captor = ArgumentCaptor.forClass(MatchStatEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().tournamentId()).isNull();
    }

    @Test
    void publishStatsFor_awayPlayerWhenHomeWon_resultIsLost() {
        Match match = matchWonByHome();
        UUID playerId = UUID.randomUUID();
        Card redCard = new Card();
        redCard.setPlayerId(playerId);
        redCard.setTeamId(match.getAwayTeamId());
        redCard.setCardType(CardType.RED);
        when(cardRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of(redCard));
        when(goalRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of());

        statPublisher.publishStatsFor(match);

        ArgumentCaptor<MatchStatEvent> captor = ArgumentCaptor.forClass(MatchStatEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().result()).isEqualTo(MatchResult.LOST);
        assertThat(captor.getValue().redCards()).isEqualTo(1);
    }

    @Test
    void publishStatsFor_drawnMatch_resultIsDrawn() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setHomeTeamId(UUID.randomUUID());
        match.setAwayTeamId(UUID.randomUUID());
        match.setHomeScore(1);
        match.setAwayScore(1);
        UUID playerId = UUID.randomUUID();
        Goal goal = new Goal();
        goal.setPlayerId(playerId);
        goal.setTeamId(match.getHomeTeamId());
        when(goalRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of(goal));
        when(cardRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of());

        statPublisher.publishStatsFor(match);

        ArgumentCaptor<MatchStatEvent> captor = ArgumentCaptor.forClass(MatchStatEvent.class);
        verify(publisher).publish(captor.capture());
        assertThat(captor.getValue().result()).isEqualTo(MatchResult.DRAWN);
    }

    @Test
    void publishStatsFor_noGoalsOrCards_publishesNothing() {
        Match match = matchWonByHome();
        when(goalRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of());
        when(cardRepository.findByMatchIdOrderByMinuteAsc(match.getId())).thenReturn(List.of());

        statPublisher.publishStatsFor(match);

        verify(publisher, times(0)).publish(any());
        verify(tournamentClient, never()).findTournamentIdForMatch(any());
    }
}
