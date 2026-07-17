package co.edu.escuelaing.techcup.match.entity;

import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchPhase;
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

    /** Id del partido tal como lo asigna Tournament (dueño de la definición del partido). */
    @Indexed(unique = true)
    private UUID competenciaMatchId;

    private UUID tournamentId;

    private MatchPhase phase;

    private UUID courtId;

    /** Fecha/hora programada de kickoff, recibida de Tournament; habilita "iniciar partido". */
    private Instant scheduledKickoff;

    private UUID homeTeamId;

    private UUID awayTeamId;

    private String homeTeamName;

    private String awayTeamName;

    private UUID refereeId;

    private MatchStatus status = MatchStatus.SCHEDULED;

    private MatchPeriod currentPeriod;

    private int homeScore;

    private int awayScore;

    /** Equipo que no se presentó; null salvo que el partido haya terminado por walkover. */
    private UUID absentTeamId;

    private int addedMinutesFirstHalf;

    private int addedMinutesSecondHalf;

    private Instant periodStartedAt;

    private long accumulatedSeconds;

    private Instant startedAt;

    private Instant endedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
