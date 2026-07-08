package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByMatchIdOrderByMinuteAsc(UUID matchId);

    long countByMatchIdAndPlayerIdAndCardType(UUID matchId, UUID playerId, CardType cardType);
}
