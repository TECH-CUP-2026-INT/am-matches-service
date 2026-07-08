package co.edu.escuelaing.techcup.match.integration.auditoria;

import co.edu.escuelaing.techcup.match.entity.enums.EventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MatchAuditEvent(
        UUID matchId,
        EventType eventType,
        UUID refereeId,
        Instant occurredAt,
        Map<String, Object> details
) {
}
