package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchObservationRepository extends MongoRepository<MatchObservation, UUID> {

    List<MatchObservation> findByMatchIdOrderByCreatedAtAsc(UUID matchId);
}
