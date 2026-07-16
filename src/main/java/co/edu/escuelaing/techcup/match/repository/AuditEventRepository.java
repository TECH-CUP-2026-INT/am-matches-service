package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditEventRepository extends MongoRepository<AuditEvent, UUID> {

    List<AuditEvent> findByMatchIdOrderByOccurredAtAsc(UUID matchId);
}
