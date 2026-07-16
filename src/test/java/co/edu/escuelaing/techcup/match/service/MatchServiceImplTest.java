package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidMatchStateException;
import co.edu.escuelaing.techcup.match.exception.MatchNotReadyException;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.competencia.CompetenciaClient;
import co.edu.escuelaing.techcup.match.integration.competencia.ScheduledMatchInfo;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedStatPublisher;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchServiceImplTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private MatchAccessService matchAccessService;
    @Mock
    private CompetenciaClient competenciaClient;
    @Mock
    private AuditReporter auditReporter;
    @Mock
    private MatchFinishedStatPublisher matchFinishedStatPublisher;

    private MatchServiceImpl matchService;

    private final UUID competenciaMatchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        matchService = new MatchServiceImpl(
                matchRepository, matchAccessService, competenciaClient, auditReporter, matchFinishedStatPublisher);
    }

    @Test
    void startMatch_lineupNotConfirmed_throwsMatchNotReadyException() {
        when(competenciaClient.getScheduledMatch(competenciaMatchId)).thenReturn(new ScheduledMatchInfo(
                competenciaMatchId, UUID.randomUUID(), UUID.randomUUID(), "Local", "Visitante",
                Instant.now().minusSeconds(60), false));

        assertThrows(MatchNotReadyException.class, () -> matchService.startMatch(competenciaMatchId, refereeId));
    }

    @Test
    void startMatch_beforeScheduledKickoff_throwsMatchNotReadyException() {
        when(competenciaClient.getScheduledMatch(competenciaMatchId)).thenReturn(new ScheduledMatchInfo(
                competenciaMatchId, UUID.randomUUID(), UUID.randomUUID(), "Local", "Visitante",
                Instant.now().plusSeconds(3600), true));

        assertThrows(MatchNotReadyException.class, () -> matchService.startMatch(competenciaMatchId, refereeId));
    }

    @Test
    void startMatch_readyWithNoExistingRecord_startsMatchInFirstHalf() {
        when(competenciaClient.getScheduledMatch(competenciaMatchId)).thenReturn(new ScheduledMatchInfo(
                competenciaMatchId, UUID.randomUUID(), UUID.randomUUID(), "Local", "Visitante",
                Instant.now().minusSeconds(60), true));
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> {
            Match match = invocation.getArgument(0);
            if (match.getId() == null) {
                match.setId(UUID.randomUUID());
            }
            return match;
        });

        MatchResponse response = matchService.startMatch(competenciaMatchId, refereeId);

        assertThat(response.status()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(response.currentPeriod()).isEqualTo(MatchPeriod.FIRST_HALF);
        verify(auditReporter).report(any());
    }

    @Test
    void startMatch_alreadyStarted_throwsInvalidMatchStateException() {
        Match existing = new Match();
        existing.setId(UUID.randomUUID());
        existing.setRefereeId(refereeId);
        existing.setStatus(MatchStatus.IN_PROGRESS);

        when(competenciaClient.getScheduledMatch(competenciaMatchId)).thenReturn(new ScheduledMatchInfo(
                competenciaMatchId, UUID.randomUUID(), UUID.randomUUID(), "Local", "Visitante",
                Instant.now().minusSeconds(60), true));
        when(matchRepository.findByCompetenciaMatchId(competenciaMatchId)).thenReturn(Optional.of(existing));

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
    void finishMatch_alreadyFinished_throwsInvalidMatchStateException() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.FINISHED);

        UUID matchId = match.getId();
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);

        assertThrows(InvalidMatchStateException.class, () -> matchService.finishMatch(matchId, refereeId));
    }

    @Test
    void finishMatch_fromInProgress_marksFinishedAndReportsAudit() {
        Match match = new Match();
        match.setId(UUID.randomUUID());
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now().minusSeconds(60));
        match.setAccumulatedSeconds(0);

        when(matchAccessService.requireOwnedMatch(match.getId(), refereeId)).thenReturn(match);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MatchResponse response = matchService.finishMatch(match.getId(), refereeId);

        assertThat(response.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(match.getEndedAt()).isNotNull();
        verify(auditReporter).report(any());
        verify(matchFinishedStatPublisher).publishStatsFor(match);
    }
}
