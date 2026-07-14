package co.edu.escuelaing.techcup.match.entity;

import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "match")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    private UUID id = UUID.randomUUID();

    @Indexed(unique = true)
    private UUID competenciaMatchId;

    private UUID homeTeamId;

    private UUID awayTeamId;

    private String homeTeamName;

    private String awayTeamName;

    private UUID refereeId;

    private MatchStatus status = MatchStatus.SCHEDULED;

    private MatchPeriod currentPeriod;

    private int homeScore;

    private int awayScore;

    private int addedMinutesFirstHalf;

    private int addedMinutesSecondHalf;

    private Instant periodStartedAt;

    private long accumulatedSeconds;

    private Instant startedAt;

    private Instant endedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
