package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.request.FinishMatchRequest;
import co.edu.escuelaing.techcup.match.dto.request.MatchDefinitionRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidMatchStateException;
import co.edu.escuelaing.techcup.match.exception.InvalidTeamException;
import co.edu.escuelaing.techcup.match.exception.MatchAccessDeniedException;
import co.edu.escuelaing.techcup.match.exception.MatchNotFoundException;
import co.edu.escuelaing.techcup.match.exception.MatchNotReadyException;
import co.edu.escuelaing.techcup.match.exception.PenaltyShootoutRequiredException;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedEvent;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedEventPublisher;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedStatPublisher;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchAccessService matchAccessService;
    @Mock
    private AuditReporter auditReporter;
    @Mock
    private MatchFinishedStatPublisher matchFinishedStatPublisher;
    @Mock
    private MatchFinishedEventPublisher matchFinishedEventPublisher;

    private MatchServiceImpl matchService;

    private final UUID competenciaMatchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchService = new MatchServiceImpl(
                matchRepository, matchAccessService, auditReporter, matchFinishedStatPublisher, matchFinishedEventPublisher);
    }

    private MatchDefinitionRequest sampleDefinition(MatchPhase phase, Instant kickoff) {
        var utc = kickoff.atZone(java.time.ZoneOffset.UTC);
        return new MatchDefinitionRequest(
                competenciaMatchId, UUID.randomUUID(), phase, UUID.randomUUID(), UUID.randomUUID(),
                "Local", "Visitante", utc.toLocalDate(), utc.toLocalTime(), refereeId, UUID.randomUUID());
    }

    @Test
    void receiveMatchDefinition_newMatch_createsScheduledMatch() {
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.receiveMatchDefinition(
                sampleDefinition(MatchPhase.GRUPOS, Instant.now().plusSeconds(3600)));

        assertThat(response.status()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(response.competenciaMatchId()).isEqualTo(competenciaMatchId);
        assertThat(response.phase()).isEqualTo(MatchPhase.GRUPOS);
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void receiveMatchDefinition_alreadyExists_isIdempotentAndDoesNotSaveAgain() {
        Match existing = new Match();
        existing.setId(UUID.randomUUID());
        existing.setCompetenciaMatchId(competenciaMatchId);
        existing.setStatus(MatchStatus.SCHEDULED);
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.of(existing));

        MatchResponse response = matchService.receiveMatchDefinition(
                sampleDefinition(MatchPhase.GRUPOS, Instant.now()));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(matchRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void startMatch_matchNotReceivedYet_throwsMatchNotFoundException() {
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class, () -> matchService.startMatch(competenciaMatchId, refereeId));
    }

    @Test
    void startMatch_beforeScheduledKickoff_throwsMatchNotReadyException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setRefereeId(refereeId);
        match.setStatus(MatchStatus.SCHEDULED);
        match.setScheduledKickoff(Instant.now().plusSeconds(3600));
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.of(match));

        assertThrows(MatchNotReadyException.class, () -> matchService.startMatch(competenciaMatchId, refereeId));
    }

    @Test
    void startMatch_readyAndScheduled_startsMatchInFirstHalf() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setRefereeId(refereeId);
        match.setStatus(MatchStatus.SCHEDULED);
        match.setScheduledKickoff(Instant.now().minusSeconds(60));
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.startMatch(competenciaMatchId, refereeId);

        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(response.currentPeriod()).isEqualTo(MatchPeriod.FIRST_HALF);
        verify(auditReporter).report(any());
    }

    @Test
    void startMatch_wrongReferee_throwsMatchAccessDeniedException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setRefereeId(UUID.randomUUID());
        match.setStatus(MatchStatus.SCHEDULED);
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.of(match));

        assertThrows(MatchAccessDeniedException.class, () -> matchService.startMatch(competenciaMatchId, refereeId));
    }

    @Test
    void startMatch_alreadyStarted_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setRefereeId(refereeId);
        match.setStatus(MatchStatus.IN_PROGRESS);
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.of(match));

        assertThrows(InvalidMatchStateException.class, () -> matchService.startMatch(competenciaMatchId, refereeId));
    }

    @Test
    void pauseMatch_accumulatesElapsedSecondsAndClearsPeriodStart() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now().minusSeconds(120));
        match.setAccumulatedSeconds(0);

        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.pauseMatch(match.getId(), refereeId);

        assertThat(response.status()).isEqualTo(MatchStatus.PAUSED);
        assertThat(match.getAccumulatedSeconds()).isGreaterThanOrEqualTo(120L);
        assertThat(match.getPeriodStartedAt()).isNull();
    }

    @Test
    void pauseMatch_whenNotInProgress_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.PAUSED);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.pauseMatch(matchId, refereeId));
    }

    @Test
    void resumeMatch_setsInProgressAndRestartsPeriodClock() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.PAUSED);
        match.setAccumulatedSeconds(300);

        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.resumeMatch(match.getId(), refereeId);

        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(match.getPeriodStartedAt()).isNotNull();
    }

    @Test
    void resumeMatch_whenNotPaused_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.resumeMatch(matchId, refereeId));
    }

    @Test
    void startNextPeriod_fromPausedFirstHalf_startsSecondHalfInProgress() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.PAUSED);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);

        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.startNextPeriod(match.getId(), refereeId);

        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(response.currentPeriod()).isEqualTo(MatchPeriod.SECOND_HALF);
        assertThat(match.getPeriodStartedAt()).isNotNull();
    }

    @Test
    void startNextPeriod_whenNotPaused_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.startNextPeriod(matchId, refereeId));
    }

    @Test
    void startNextPeriod_whenAlreadySecondHalf_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.PAUSED);
        match.setCurrentPeriod(MatchPeriod.SECOND_HALF);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.startNextPeriod(matchId, refereeId));
    }

    @Test
    void addInjuryTime_duringSecondHalf_addsToSecondHalfMinutes() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setCurrentPeriod(MatchPeriod.SECOND_HALF);
        match.setAddedMinutesSecondHalf(1);

        when(matchAccessService.requireActiveMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.addInjuryTime(match.getId(), refereeId, 2);

        assertThat(response.addedMinutesSecondHalf()).isEqualTo(3);
        assertThat(response.addedMinutesFirstHalf()).isZero();
    }

    @Test
    void addInjuryTime_duringFirstHalf_addsToFirstHalfMinutes() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        match.setAddedMinutesFirstHalf(1);

        when(matchAccessService.requireActiveMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.addInjuryTime(match.getId(), refereeId, 2);

        assertThat(response.addedMinutesFirstHalf()).isEqualTo(3);
    }

    @Test
    void getMatch_returnsCurrentMatchState() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);

        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);

        MatchResponse response = matchService.getMatch(match.getId(), refereeId);

        assertThat(response.id()).isEqualTo(match.getId());
        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
    }

    @Test
    void listAssignedMatches_returnsSummariesFromLocalRepository() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setCompetenciaMatchId(competenciaMatchId);
        match.setStatus(MatchStatus.SCHEDULED);
        when(matchRepository.findByRefereeId(refereeId)).thenReturn(List.of(match));

        List<MatchSummaryResponse> summaries = matchService.listAssignedMatches(refereeId);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).status()).isEqualTo(MatchStatus.SCHEDULED);
    }

    private Match inProgressMatch(MatchPhase phase, int homeScore, int awayScore) {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setCompetenciaMatchId(competenciaMatchId);
        match.setTournamentId(UUID.randomUUID());
        match.setPhase(phase);
        match.setHomeTeamId(UUID.randomUUID());
        match.setAwayTeamId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now().minusSeconds(60));
        match.setAccumulatedSeconds(0);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        return match;
    }

    @Test
    void finishMatch_scheduledMatch_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.SCHEDULED);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.finishMatch(matchId, refereeId, null));
    }

    @Test
    void finishMatch_alreadyFinished_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.FINISHED);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.finishMatch(matchId, refereeId, null));

        verify(matchFinishedEventPublisher, org.mockito.Mockito.never()).publish(any());
    }

    @Test
    void finishMatch_gruposHigherHomeScore_ganadorIsHomeTeamAndNoEliminado() {
        Match match = inProgressMatch(MatchPhase.GRUPOS, 2, 1);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        matchService.finishMatch(match.getId(), refereeId, null);

        ArgumentCaptor<MatchFinishedEvent> captor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(matchFinishedEventPublisher).publish(captor.capture());
        MatchFinishedEvent event = captor.getValue();
        assertThat(event.matchId()).isEqualTo(competenciaMatchId);
        assertThat(event.ganadorId()).isEqualTo(match.getHomeTeamId());
        assertThat(event.eliminadoId()).isNull();
        assertThat(event.golesA()).isEqualTo(2);
        assertThat(event.golesB()).isEqualTo(1);
    }

    @Test
    void finishMatch_gruposDraw_ganadorAndEliminadoAreNull() {
        Match match = inProgressMatch(MatchPhase.GRUPOS, 1, 1);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        matchService.finishMatch(match.getId(), refereeId, null);

        ArgumentCaptor<MatchFinishedEvent> captor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(matchFinishedEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().ganadorId()).isNull();
        assertThat(captor.getValue().eliminadoId()).isNull();
    }

    @Test
    void finishMatch_eliminatoriaNoDraw_ganadorAndEliminadoAreSet() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 3, 1);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        matchService.finishMatch(match.getId(), refereeId, null);

        ArgumentCaptor<MatchFinishedEvent> captor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(matchFinishedEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().ganadorId()).isEqualTo(match.getHomeTeamId());
        assertThat(captor.getValue().eliminadoId()).isEqualTo(match.getAwayTeamId());
    }

    @Test
    void finishMatch_eliminatoriaDrawWithoutPenalties_throwsPenaltyShootoutRequiredException() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 2, 2);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);

        assertThrows(PenaltyShootoutRequiredException.class,
                () -> matchService.finishMatch(match.getId(), refereeId, null));

        verify(matchFinishedEventPublisher, org.mockito.Mockito.never()).publish(any());
    }

    @Test
    void finishMatch_eliminatoriaDrawWithPenalties_resolvesWinnerFromPenaltyGoals() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 1, 1);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinishMatchRequest request = new FinishMatchRequest(null, null, 5, 4);
        matchService.finishMatch(match.getId(), refereeId, request);

        ArgumentCaptor<MatchFinishedEvent> captor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(matchFinishedEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().ganadorId()).isEqualTo(match.getHomeTeamId());
        assertThat(captor.getValue().eliminadoId()).isEqualTo(match.getAwayTeamId());
        // El marcador reportado sigue siendo el del tiempo reglamentario, no el de penales.
        assertThat(captor.getValue().golesA()).isEqualTo(1);
        assertThat(captor.getValue().golesB()).isEqualTo(1);
    }

    @Test
    void finishMatch_eliminatoriaDrawWithTiedPenalties_throwsInvalidMatchStateException() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 1, 1);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);

        FinishMatchRequest request = new FinishMatchRequest(null, null, 4, 4);

        assertThrows(InvalidMatchStateException.class,
                () -> matchService.finishMatch(match.getId(), refereeId, request));
    }

    @Test
    void finishMatch_walkoverInGrupos_presentTeamWinsAndNoOneIsEliminated() {
        Match match = inProgressMatch(MatchPhase.GRUPOS, 0, 0);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinishMatchRequest request = new FinishMatchRequest(true, match.getAwayTeamId(), null, null);
        matchService.finishMatch(match.getId(), refereeId, request);

        ArgumentCaptor<MatchFinishedEvent> captor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(matchFinishedEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().ganadorId()).isEqualTo(match.getHomeTeamId());
        assertThat(captor.getValue().eliminadoId()).isNull();
        assertThat(captor.getValue().ausenteId()).isEqualTo(match.getAwayTeamId());
        assertThat(captor.getValue().golesA()).isZero();
        assertThat(captor.getValue().golesB()).isZero();
        assertThat(match.getAbsentTeamId()).isEqualTo(match.getAwayTeamId());
    }

    @Test
    void finishMatch_walkoverInEliminatoria_presentTeamWinsAbsentTeamEliminated() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 0, 0);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinishMatchRequest request = new FinishMatchRequest(true, match.getHomeTeamId(), null, null);
        matchService.finishMatch(match.getId(), refereeId, request);

        ArgumentCaptor<MatchFinishedEvent> captor = ArgumentCaptor.forClass(MatchFinishedEvent.class);
        verify(matchFinishedEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().ganadorId()).isEqualTo(match.getAwayTeamId());
        assertThat(captor.getValue().eliminadoId()).isEqualTo(match.getHomeTeamId());
        assertThat(captor.getValue().ausenteId()).isEqualTo(match.getHomeTeamId());
    }

    @Test
    void finishMatch_walkoverWithoutAbsentTeam_throwsValidationException() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 0, 0);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);

        FinishMatchRequest request = new FinishMatchRequest(true, null, null, null);

        assertThrows(ValidationException.class, () -> matchService.finishMatch(match.getId(), refereeId, request));
    }

    @Test
    void finishMatch_walkoverWithTeamNotInMatch_propagatesInvalidTeamException() {
        Match match = inProgressMatch(MatchPhase.ELIMINATORIA, 0, 0);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        UUID unrelatedTeamId = UUID.randomUUID();
        doThrow(new InvalidTeamException(unrelatedTeamId, match.getId()))
                .when(matchAccessService).validateTeamBelongsToMatch(match, unrelatedTeamId);

        FinishMatchRequest request = new FinishMatchRequest(true, unrelatedTeamId, null, null);

        assertThrows(InvalidTeamException.class, () -> matchService.finishMatch(match.getId(), refereeId, request));
    }

    @Test
    void finishMatch_fromInProgress_marksFinishedAndReportsAudit() {
        Match match = inProgressMatch(MatchPhase.GRUPOS, 0, 0);
        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.finishMatch(match.getId(), refereeId, null);

        assertThat(response.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getEndedAt()).isNotNull();
        verify(auditReporter).report(any());
        verify(matchFinishedStatPublisher).publishStatsFor(match);
    }
}
