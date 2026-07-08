package co.edu.escuelaing.techcup.match.entity;

import co.edu.escuelaing.techcup.match.entity.enums.MatchPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "substitution")
@Getter
@Setter
@NoArgsConstructor
public class Substitution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "player_out_id", nullable = false)
    private UUID playerOutId;

    @Column(name = "player_in_id", nullable = false)
    private UUID playerInId;

    @Column(nullable = false)
    private int minute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchPeriod period;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
