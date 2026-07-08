package co.edu.escuelaing.techcup.match.service;

import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.time.Duration;
import java.time.Instant;

/**
 * Calcula el minuto actual del partido sin necesidad de un job en segundo plano:
 * accumulatedSeconds guarda el tiempo ya transcurrido antes de la pausa/lectura actual,
 * y solo se suma tiempo en vivo si el partido está IN_PROGRESS.
 */
public final class MatchClock {

    private MatchClock() {
    }

    public static int currentMinute(Match match) {
        long liveSeconds = match.getAccumulatedSeconds();
        if (match.getStatus() == MatchStatus.IN_PROGRESS && match.getPeriodStartedAt() != null) {
            liveSeconds += Duration.between(match.getPeriodStartedAt(), Instant.now()).getSeconds();
        }
        return (int) (liveSeconds / 60) + 1;
    }
}
