package co.edu.escuelaing.techcup.match.integration.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import co.edu.escuelaing.techcup.match.entity.AuditEvent;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import co.edu.escuelaing.techcup.match.repository.AuditEventRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MongoAuditReporterTest {

    private final AuditEventRepository auditEventRepository = mock(AuditEventRepository.class);
    private final MongoAuditReporter reporter = new MongoAuditReporter(auditEventRepository);

    @Test
    void report_persistsAuditEventWithSameFields() {
        UUID matchId = UUID.randomUUID();
        UUID refereeId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        Map<String, Object> details = Map.of("minute", 10);

        reporter.report(new MatchAuditEvent(matchId, EventType.GOAL, refereeId, occurredAt, details));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMatchId()).isEqualTo(matchId);
        assertThat(saved.getEventType()).isEqualTo(EventType.GOAL);
        assertThat(saved.getRefereeId()).isEqualTo(refereeId);
        assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(saved.getDetails()).isEqualTo(details);
    }
}
