package co.edu.escuelaing.techcup.match.entity;

import co.edu.escuelaing.techcup.match.entity.enums.CardType;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "card")
@Getter
@Setter
@NoArgsConstructor
public class Card {

    @Id
    private UUID id = UUID.randomUUID();

    @Indexed
    private UUID matchId;

    private UUID teamId;

    private UUID playerId;

    private CardType cardType;

    private int minute;

    private MatchPeriod period;

    private Instant createdAt;
}
