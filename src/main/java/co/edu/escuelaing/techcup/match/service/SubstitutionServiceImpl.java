package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.dto.request.RegisterSubstitutionRequest;
import co.edu.escuelaing.techcup.match.dto.response.SubstitutionResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.Substitution;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.integration.auditoria.AuditReporter;
import co.edu.escuelaing.techcup.match.integration.auditoria.MatchAuditEvent;
import co.edu.escuelaing.techcup.match.integration.estadisticas.MatchEventPublisher;
import co.edu.escuelaing.techcup.match.integration.estadisticas.SubstitutionEventPayload;
import co.edu.escuelaing.techcup.match.mapper.SubstitutionMapper;
import co.edu.escuelaing.techcup.match.repository.SubstitutionRepository;
import jakarta.validation.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubstitutionServiceImpl implements SubstitutionService {

    private final SubstitutionRepository substitutionRepository;
    private final MatchAccessService matchAccessService;
    private final MatchEventPublisher eventPublisher;
    private final AuditReporter auditReporter;

    public SubstitutionServiceImpl(SubstitutionRepository substitutionRepository,
                                    MatchAccessService matchAccessService,
                                    MatchEventPublisher eventPublisher,
                                    AuditReporter auditReporter) {
        this.substitutionRepository = substitutionRepository;
        this.matchAccessService = matchAccessService;
        this.eventPublisher = eventPublisher;
        this.auditReporter = auditReporter;
    }

    @Override
    public SubstitutionResponse registerSubstitution(UUID matchId, UUID refereeId, RegisterSubstitutionRequest request) {
        Match match = matchAccessService.requireActiveMatch(matchId, refereeId);
        matchAccessService.validateTeamBelongsToMatch(match, request.teamId());

        if (request.playerOutId().equals(request.playerInId())) {
            throw new ValidationException("El jugador que entra no puede ser el mismo que sale");
        }

        int minute = request.minute() != null ? request.minute() : MatchClock.currentMinute(match);

        Substitution substitution = new Substitution();
        substitution.setMatchId(match.getId());
        substitution.setTeamId(request.teamId());
        substitution.setPlayerOutId(request.playerOutId());
        substitution.setPlayerInId(request.playerInId());
        substitution.setMinute(minute);
        substitution.setPeriod(match.getCurrentPeriod());
        substitution.setCreatedAt(Instant.now());
        substitution = substitutionRepository.save(substitution);

        eventPublisher.publishSubstitution(new SubstitutionEventPayload(
                match.getId(), substitution.getTeamId(), substitution.getPlayerOutId(),
                substitution.getPlayerInId(), minute));

        auditReporter.report(new MatchAuditEvent(match.getId(), EventType.SUBSTITUTION, refereeId, Instant.now(),
                Map.of("teamId", substitution.getTeamId(),
                        "playerOutId", substitution.getPlayerOutId(),
                        "playerInId", substitution.getPlayerInId(),
                        "minute", minute)));

        return SubstitutionMapper.toResponse(substitution);
    }

    @Override
    public List<SubstitutionResponse> listSubstitutions(UUID matchId, UUID refereeId) {
        matchAccessService.requireOwnedMatch(matchId, refereeId);
        return substitutionRepository.findByMatchIdOrderByMinuteAsc(matchId).stream()
                .map(SubstitutionMapper::toResponse)
                .toList();
    }
}
