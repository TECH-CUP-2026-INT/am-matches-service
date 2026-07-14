package co.edu.escuelaing.techcup.match.repository;

import co.edu.escuelaing.techcup.match.entity.Card;
import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CardRepository extends MongoRepository<Card, UUID> {

    List<Card> findByMatchIdOrderByMinuteAsc(UUID matchId);

    long countByMatchIdAndPlayerIdAndCardType(UUID matchId, UUID playerId, CardType cardType);
}
