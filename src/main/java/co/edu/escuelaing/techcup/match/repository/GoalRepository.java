package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.Goal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GoalRepository extends MongoRepository<Goal, UUID> {

    List<Goal> findByMatchIdOrderByMinuteAsc(UUID matchId);
}
