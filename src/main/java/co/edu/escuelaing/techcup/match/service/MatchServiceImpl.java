package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidMatchStateException;
import co.edu.escuelaing.techcup.match.exception.MatchAccessDeniedException;
import co.edu.escuelaing.techcup.match.exception.MatchNotReadyException;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.auditoria.MatchAuditEvent;
import co.edu.escuelaing.techcup.match.integration.competencia.CompetenciaClient;
import co.edu.escuelaing.techcup.match.integration.competencia.ScheduledMatchInfo;
import co.edu.escuelaing.techcup.match.mapper.MatchMapper;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final MatchAccessService matchAccessService;
    private final CompetenciaClient competenciaClient;
    private final AuditReporter auditReporter;

    public MatchServiceImpl(MatchRepository matchRepository,
                             MatchAccessService matchAccessService,
                             CompetenciaClient competenciaClient,
                             AuditReporter auditReporter) {
        this.matchRepository = matchRepository;
        this.matchAccessService = matchAccessService;
        this.competenciaClient = competenciaClient;
        this.auditReporter = auditReporter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> listAssignedMatches(UUID refereeId) {
        List<ScheduledMatchInfo> assigned = competenciaClient.getAssignedMatches(refereeId);
        return assigned.stream()
                .map(scheduled -> matchRepository.findByCompetenciaMatchId(scheduled.competenciaMatchId())
                        .map(MatchMapper::toSummary)
                        .orElseGet(() -> MatchMapper.toUnstartedSummary(scheduled, isReadyToStart(scheduled))))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
    }

    @Override
    @Transactional
    public MatchResponse startMatch(UUID competenciaMatchId, UUID refereeId) {
        ScheduledMatchInfo scheduled = competenciaClient.getScheduledMatch(competenciaMatchId);

        if (!scheduled.lineupConfirmed()) {
            throw new MatchNotReadyException("La alineación del partido aún no ha sido confirmada por Competencia");
        }
        if (Instant.now().isBefore(scheduled.scheduledKickoff())) {
            throw new MatchNotReadyException("El partido aún no ha llegado a su hora programada de inicio");
        }

        Match match = matchRepository.findByCompetenciaMatchId(competenciaMatchId).orElseGet(Match::new);

        if (match.getId() != null) {
            if (!match.getRefereeId().equals(refereeId)) {
                throw new MatchAccessDeniedException(competenciaMatchId);
            }
            if (match.getStatus() != MatchStatus.SCHEDULED) {
                throw new InvalidMatchStateException("El partido ya fue iniciado o finalizado previamente");
            }
        } else {
            match.setCompetenciaMatchId(scheduled.competenciaMatchId());
            match.setHomeTeamId(scheduled.homeTeamId());
            match.setAwayTeamId(scheduled.awayTeamId());
            match.setHomeTeamName(scheduled.homeTeamName());
            match.setAwayTeamName(scheduled.awayTeamName());
            match.setRefereeId(refereeId);
        }

        Instant now = Instant.now();
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        match.setStartedAt(now);
        match.setPeriodStartedAt(now);
        match.setAccumulatedSeconds(0);

        match = matchRepository.save(match);

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.MATCH_STARTED, refereeId, now, Map.of()));

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_STARTED);
    }

    @Override
    @Transactional
    public MatchResponse pauseMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new InvalidMatchStateException("Solo se puede pausar un partido en curso");
        }

        Instant now = Instant.now();
        match.setAccumulatedSeconds(match.getAccumulatedSeconds()
                + Duration.between(match.getPeriodStartedAt(), now).getSeconds());
        match.setStatus(MatchStatus.PAUSED);
        match.setPeriodStartedAt(null);
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_PAUSED);
    }

    @Override
    @Transactional
    public MatchResponse resumeMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        if (match.getStatus() != MatchStatus.PAUSED) {
            throw new InvalidMatchStateException("Solo se puede reanudar un partido pausado");
        }

        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now());
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_RESUMED);
    }

    @Override
    @Transactional
    public MatchResponse startNextPeriod(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        if (match.getStatus() != MatchStatus.PAUSED) {
            throw new InvalidMatchStateException("El partido debe estar pausado (descanso) para iniciar el siguiente tiempo");
        }
        if (match.getCurrentPeriod() != MatchPeriod.FIRST_HALF) {
            throw new InvalidMatchStateException("El partido ya se encuentra en el segundo tiempo");
        }

        match.setCurrentPeriod(MatchPeriod.SECOND_HALF);
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now());
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_RESUMED);
    }

    @Override
    @Transactional
    public MatchResponse addInjuryTime(UUID matchId, UUID refereeId, int minutes) {
        Match match = matchAccessService.requireActiveMatch(matchId, refereeId);

        if (match.getCurrentPeriod() == MatchPeriod.SECOND_HALF) {
            match.setAddedMinutesSecondHalf(match.getAddedMinutesSecondHalf() + minutes);
        } else {
            match.setAddedMinutesFirstHalf(match.getAddedMinutesFirstHalf() + minutes);
        }
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
    }

    @Override
    @Transactional
    public MatchResponse finishMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new InvalidMatchStateException("El partido ya fue finalizado");
        }
        if (match.getStatus() == MatchStatus.SCHEDULED) {
            throw new InvalidMatchStateException("No se puede finalizar un partido que no ha iniciado");
        }

        Instant now = Instant.now();
        if (match.getStatus() == MatchStatus.IN_PROGRESS && match.getPeriodStartedAt() != null) {
            match.setAccumulatedSeconds(match.getAccumulatedSeconds()
                    + Duration.between(match.getPeriodStartedAt(), now).getSeconds());
        }
        match.setStatus(MatchStatus.FINISHED);
        match.setEndedAt(now);
        match.setPeriodStartedAt(null);
        match = matchRepository.save(match);

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.MATCH_FINISHED, refereeId, now, Map.of()));

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_FINISHED);
    }

    private boolean isReadyToStart(ScheduledMatchInfo scheduled) {
        return scheduled.lineupConfirmed() && !Instant.now().isBefore(scheduled.scheduledKickoff());
    }
}
