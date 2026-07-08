package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchObservationRepository extends JpaRepository<MatchObservation, UUID> {

    List<MatchObservation> findByMatchIdOrderByCreatedAtAsc(UUID matchId);
}
