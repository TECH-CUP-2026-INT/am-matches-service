package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.FinishMatchRequest;
import co.edu.escuelaing.techcup.match.dto.request.MatchDefinitionRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchResponse;
import co.edu.escuelaing.techcup.match.dto.response.MatchSummaryResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidMatchStateException;
import co.edu.escuelaing.techcup.match.exception.MatchAccessDeniedException;
import co.edu.escuelaing.techcup.match.exception.MatchNotFoundException;
import co.edu.escuelaing.techcup.match.exception.MatchNotReadyException;
import co.edu.escuelaing.techcup.match.exception.PenaltyShootoutRequiredException;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.auditoria.MatchAuditEvent;
import co.edu.escuelaing.techcup.match.mapper.MatchMapper;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedEvent;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedEventPublisher;
import co.edu.escuelaing.techcup.match.messaging.MatchFinishedStatPublisher;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import jakarta.validation.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final MatchAccessService matchAccessService;
    private final AuditReporter auditReporter;
    private final MatchFinishedStatPublisher matchFinishedStatPublisher;
    private final MatchFinishedEventPublisher matchFinishedEventPublisher;

    public MatchServiceImpl(MatchRepository matchRepository,
                             MatchAccessService matchAccessService,
                             AuditReporter auditReporter,
                             MatchFinishedStatPublisher matchFinishedStatPublisher,
                             MatchFinishedEventPublisher matchFinishedEventPublisher) {
        this.matchRepository = matchRepository;
        this.matchAccessService = matchAccessService;
        this.auditReporter = auditReporter;
        this.matchFinishedStatPublisher = matchFinishedStatPublisher;
        this.matchFinishedEventPublisher = matchFinishedEventPublisher;
    }

    @Override
    public MatchResponse receiveMatchDefinition(MatchDefinitionRequest request) {
        Optional<Match> existing = matchRepository.findByCompetenciaMatchId(request.matchId());
        if (existing.isPresent()) {
            // Idempotencia: Tournament puede reintentar la entrega del mismo mensaje
            // (p. ej. redelivery); no se sobrescribe un partido ya recibido.
            Match match = existing.get();
            return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
        }

        Instant now = Instant.now();
        Match match = new Match();
        match.setCompetenciaMatchId(request.matchId());
        match.setTournamentId(request.tournamentId());
        match.setPhase(request.fase());
        match.setHomeTeamId(request.equipoAId());
        match.setAwayTeamId(request.equipoBId());
        match.setHomeTeamName(request.equipoANombre());
        match.setAwayTeamName(request.equipoBNombre());
        match.setRefereeId(request.arbitroId());
        match.setCourtId(request.canchaId());
        match.setScheduledKickoff(request.fecha().atTime(request.hora()).toInstant(ZoneOffset.UTC));
        match.setStatus(MatchStatus.SCHEDULED);
        match.setCreatedAt(now);
        match.setUpdatedAt(now);

        match = matchRepository.save(match);
        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
    }

    @Override
    public List<MatchSummaryResponse> listAssignedMatches(UUID refereeId) {
        return matchRepository.findByRefereeId(refereeId).stream()
                .map(MatchMapper::toSummary)
                .toList();
    }

    @Override
    public MatchResponse getMatch(UUID matchId, UUID refereeId) {
        Match match = matchAccessService.requireOwnedMatch(matchId, refereeId);
        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), null);
    }

    @Override
    public MatchResponse startMatch(UUID competenciaMatchId, UUID refereeId) {
        Match match = matchRepository.findByCompetenciaMatchId(competenciaMatchId)
                .orElseThrow(() -> new MatchNotFoundException(competenciaMatchId));

        if (!match.getRefereeId().equals(refereeId)) {
            throw new MatchAccessDeniedException(competenciaMatchId);
        }
        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new InvalidMatchStateException("El partido ya fue iniciado o finalizado previamente");
        }
        if (Instant.now().isBefore(match.getScheduledKickoff())) {
            throw new MatchNotReadyException("El partido aún no ha llegado a su hora programada de inicio");
        }

        Instant now = Instant.now();
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
    public MatchResponse finishMatch(UUID matchId, UUID refereeId, FinishMatchRequest request) {
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

        MatchOutcome outcome = resolveOutcome(match, request);

        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(outcome.golesA());
        match.setAwayScore(outcome.golesB());
        match.setEndedAt(now);
        match.setPeriodStartedAt(null);
        match.setUpdatedAt(now);
        match = matchRepository.save(match);

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.MATCH_FINISHED, refereeId, now, Map.of()));
        matchFinishedStatPublisher.publishStatsFor(match);
        matchFinishedEventPublisher.publish(new MatchFinishedEvent(
                match.getCompetenciaMatchId(), match.getTournamentId(), match.getPhase(),
                outcome.golesA(), outcome.golesB(), outcome.ganadorId(), outcome.eliminadoId(),
                outcome.ausenteId(), now));

        return MatchMapper.toResponse(match, MatchClock.currentMinute(match), EventType.MATCH_FINISHED);
    }

    /**
     * Fija golesA/golesB/ganadorId/eliminadoId según fase y walkover, consultando penales
     * solo cuando eliminatoria termina empatada. GRUPOS admite empate (ganadorId null,
     * nunca hay eliminadoId); ELIMINATORIA siempre exige un ganador.
     */
    private MatchOutcome resolveOutcome(Match match, FinishMatchRequest request) {
        boolean walkover = request != null && Boolean.TRUE.equals(request.walkover());

        if (walkover) {
            UUID absentTeamId = request.equipoAusenteId();
            if (absentTeamId == null) {
                throw new ValidationException("Debe indicar el equipo ausente (equipoAusenteId) para declarar walkover");
            }
            matchAccessService.validateTeamBelongsToMatch(match, absentTeamId);
            match.setAbsentTeamId(absentTeamId);

            UUID presentTeamId = absentTeamId.equals(match.getHomeTeamId())
                    ? match.getAwayTeamId() : match.getHomeTeamId();

            // Gana el equipo presente en ambas fases; solo cambia el eliminado, porque en
            // grupos no se elimina a nadie. El marcador queda 0-0: ausenteId es el único dato
            // que le permite a Tournament separarlo de un empate real (ver MatchFinishedEvent).
            if (match.getPhase() == MatchPhase.ELIMINATORIA) {
                return new MatchOutcome(0, 0, presentTeamId, absentTeamId, absentTeamId);
            }
            return new MatchOutcome(0, 0, presentTeamId, null, absentTeamId);
        }

        int golesA = match.getHomeScore();
        int golesB = match.getAwayScore();

        if (match.getPhase() == MatchPhase.GRUPOS) {
            if (golesA > golesB) {
                return new MatchOutcome(golesA, golesB, match.getHomeTeamId(), null, null);
            }
            if (golesB > golesA) {
                return new MatchOutcome(golesA, golesB, match.getAwayTeamId(), null, null);
            }
            return new MatchOutcome(golesA, golesB, null, null, null);
        }

        // ELIMINATORIA: no se permite empate.
        if (golesA > golesB) {
            return new MatchOutcome(golesA, golesB, match.getHomeTeamId(), match.getAwayTeamId(), null);
        }
        if (golesB > golesA) {
            return new MatchOutcome(golesA, golesB, match.getAwayTeamId(), match.getHomeTeamId(), null);
        }

        if (request == null || request.golesPenalesA() == null || request.golesPenalesB() == null) {
            throw new PenaltyShootoutRequiredException(
                    "Partido de eliminatoria empatado: se requiere el resultado de los penales para definir el ganador");
        }
        int penalesA = request.golesPenalesA();
        int penalesB = request.golesPenalesB();
        if (penalesA == penalesB) {
            throw new InvalidMatchStateException("Los goles de penales no pueden terminar en empate");
        }
        return penalesA > penalesB
                ? new MatchOutcome(golesA, golesB, match.getHomeTeamId(), match.getAwayTeamId(), null)
                : new MatchOutcome(golesA, golesB, match.getAwayTeamId(), match.getHomeTeamId(), null);
    }

    private record MatchOutcome(int golesA, int golesB, UUID ganadorId, UUID eliminadoId, UUID ausenteId) {
    }
}
