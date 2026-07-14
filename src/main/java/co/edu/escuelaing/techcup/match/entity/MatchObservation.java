package co.edu.escuelaing.techcup.match.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "match_observation")
@Getter
@Setter
@NoArgsConstructor
public class MatchObservation {

    @Id
    private UUID id = UUID.randomUUID();

    @Indexed
    private UUID matchId;

    private UUID refereeId;

    private String text;

    private Integer minute;

    private Instant createdAt;
}
