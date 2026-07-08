package co.edu.escuelaing.techcup.match.entity;

import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "match")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "competencia_match_id", nullable = false, unique = true)
    private UUID competenciaMatchId;

    @Column(name = "home_team_id", nullable = false)
    private UUID homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private UUID awayTeamId;

    @Column(name = "home_team_name", nullable = false, length = 150)
    private String homeTeamName;

    @Column(name = "away_team_name", nullable = false, length = 150)
    private String awayTeamName;

    @Column(name = "referee_id", nullable = false)
    private UUID refereeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_period", length = 20)
    private MatchPeriod currentPeriod;

    @Column(name = "home_score", nullable = false)
    private int homeScore;

    @Column(name = "away_score", nullable = false)
    private int awayScore;

    @Column(name = "added_minutes_first_half", nullable = false)
    private int addedMinutesFirstHalf;

    @Column(name = "added_minutes_second_half", nullable = false)
    private int addedMinutesSecondHalf;

    @Column(name = "period_started_at")
    private Instant periodStartedAt;

    @Column(name = "accumulated_seconds", nullable = false)
    private long accumulatedSeconds;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
