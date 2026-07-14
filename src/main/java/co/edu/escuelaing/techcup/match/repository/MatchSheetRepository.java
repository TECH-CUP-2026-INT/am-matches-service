package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.MatchSheet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchSheetRepository extends MongoRepository<MatchSheet, UUID> {

    Optional<MatchSheet> findByMatchId(UUID matchId);
}
