package co.edu.escuelaing.techcup.match.integration.auditoria;

import co.edu.escuelaing.techcup.match.entity.AuditEvent;
import co.edu.escuelaing.techcup.match.repository.AuditEventRepository;
import org.springframework.stereotype.Component;

@Component
public class MongoAuditReporter implements AuditReporter {

    private final AuditEventRepository auditEventRepository;

    public MongoAuditReporter(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Override
    public void report(MatchAuditEvent event) {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setMatchId(event.matchId());
        auditEvent.setEventType(event.eventType());
        auditEvent.setRefereeId(event.refereeId());
        auditEvent.setOccurredAt(event.occurredAt());
        auditEvent.setDetails(event.details());
        auditEventRepository.save(auditEvent);
    }
}
