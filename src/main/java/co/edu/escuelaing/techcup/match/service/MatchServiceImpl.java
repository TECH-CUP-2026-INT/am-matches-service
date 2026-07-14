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
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

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
    public List<MatchSummaryResponse> listAssignedMatches(UUID refereeId) {
        List<ScheduledMatchInfo> assigned = competenciaClient.getAssignedMatches(refereeId);
        return assigned.stream()
                .map(scheduled -> matchRepository.findByCompetenciaMatchId(scheduled.competenciaMatchId())
                        .map(MatchMapper::toSummary)
                        .orElseGet(() -> MatchMapper.toUnstartedSummary(scheduled, isReadyToStart(scheduled))))
                .toList();
    }

    @Override
    public MatchResponse getMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
    }

    @Override
    public MatchResponse startMatch(UUID competenciaMatchId, UUID refereeId) {
        ScheduledMatchInfo scheduled = competenciaClient.getScheduledMatch(competenciaMatchId);

        if (!scheduled.lineupConfirmed()) {
            throw new MatchNotReadyException("La alineación del partido aún no ha sido confirmada por Competencia");
        }
        if (Instant.now().isBefore(scheduled.scheduledKickoff())) {
            throw new MatchNotReadyException("El partido aún no ha llegado a su hora programada de inicio");
        }

        // Nota: con Mongo el id de Match se genera en el constructor (inicializador de
        // campo), por lo que ya no sirve "match.getId() != null" para distinguir un
        // partido existente de uno nuevo (siempre tendría id). Se usa el Optional
        // devuelto por el repositorio para esa decisión.
        Optional<Match> existingMatch = matchRepository.findByCompetenciaMatchId(competenciaMatchId);
        Match match;
        Instant now = Instant.now();

        if (existingMatch.isPresent()) {
            match = existingMatch.get();
            if (!match.getRefereeId().equals(refereeId)) {
                throw new MatchAccessDeniedException(competenciaMatchId);
            }
            if (match.getStatus() != MatchStatus.SCHEDULED) {
                throw new InvalidMatchStateException("El partido ya fue iniciado o finalizado previamente");
            }
        } else {
            match = new Match();
            match.setCompetenciaMatchId(scheduled.competenciaMatchId());
            match.setHomeTeamId(scheduled.homeTeamId());
            match.setAwayTeamId(scheduled.awayTeamId());
            match.setHomeTeamName(scheduled.homeTeamName());
            match.setAwayTeamName(scheduled.awayTeamName());
            match.setRefereeId(refereeId);
            match.setCreatedAt(now);
        }

        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setCurrentPeriod(MatchPeriod.FIRST_HALF);
        match.setStartedAt(now);
        match.setPeriodStartedAt(now);
        match.setAccumulatedSeconds(0);
        match.setUpdatedAt(now);

        match = matchRepository.save(match);

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.MATCH_STARTED, refereeId, now, Map.of()));

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_STARTED);
    }

    @Override
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
        match.setUpdatedAt(now);
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_PAUSED);
    }

    @Override
    public MatchResponse resumeMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        if (match.getStatus() != MatchStatus.PAUSED) {
            throw new InvalidMatchStateException("Solo se puede reanudar un partido pausado");
        }

        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setPeriodStartedAt(Instant.now());
        match.setUpdatedAt(Instant.now());
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_RESUMED);
    }

    @Override
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
        match.setUpdatedAt(Instant.now());
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_RESUMED);
    }

    @Override
    public MatchResponse addInjuryTime(UUID matchId, UUID refereeId, int minutes) {
        Match match = matchAccessService.requireActiveMatch(matchId, refereeId);

        if (match.getCurrentPeriod() == MatchPeriod.SECOND_HALF) {
            match.setAddedMinutesSecondHalf(match.getAddedMinutesSecondHalf() + minutes);
        } else {
            match.setAddedMinutesFirstHalf(match.getAddedMinutesFirstHalf() + minutes);
        }
        match.setUpdatedAt(Instant.now());
        match = matchRepository.save(match);

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
    }

    @Override
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
        match.setUpdatedAt(now);
        match = matchRepository.save(match);

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.MATCH_FINISHED, refereeId, now, Map.of()));

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_FINISHED);
    }

    private boolean isReadyToStart(ScheduledMatchInfo scheduled) {
        return scheduled.lineupConfirmed() && !Instant.now().isBefore(scheduled.scheduledKickoff());
    }
}
