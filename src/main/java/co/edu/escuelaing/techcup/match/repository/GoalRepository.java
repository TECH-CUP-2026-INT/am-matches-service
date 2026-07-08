package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.Goal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByMatchIdOrderByMinuteAsc(UUID matchId);
}
