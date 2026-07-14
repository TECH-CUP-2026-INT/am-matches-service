package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.Match;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchRepository extends MongoRepository<Match, UUID> {

    Optional<Match> findByCompetenciaMatchId(UUID competenciaMatchId);

    List<Match> findByRefereeId(UUID refereeId);
}
