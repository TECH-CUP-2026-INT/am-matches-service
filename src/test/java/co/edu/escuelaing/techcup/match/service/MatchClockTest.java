package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MatchClockTest {

    @Test
    void inProgressMatch_addsElapsedLiveSecondsToAccumulated() {
        Match match = new Match();
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setAccumulatedSeconds(0);
        match.setPeriodStartedAt(Instant.now().minusSeconds(125));

        assertThat(MatchClock.currentMinute(match)).isEqualTo(3);
    }

    @Test
    void pausedMatch_usesOnlyAccumulatedSeconds() {
        Match match = new Match();
        match.setStatus(MatchStatus.PAUSED);
        match.setAccumulatedSeconds(600);
        match.setPeriodStartedAt(null);

        assertThat(MatchClock.currentMinute(match)).isEqualTo(11);
    }

    @Test
    void justStartedMatch_isMinuteOne() {
        Match match = new Match();
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setAccumulatedSeconds(0);
        match.setPeriodStartedAt(Instant.now());

        assertThat(MatchClock.currentMinute(match)).isEqualTo(1);
    }
}
