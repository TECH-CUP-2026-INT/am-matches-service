package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.Substitution;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubstitutionRepository extends JpaRepository<Substitution, UUID> {

    List<Substitution> findByMatchIdOrderByMinuteAsc(UUID matchId);
}
