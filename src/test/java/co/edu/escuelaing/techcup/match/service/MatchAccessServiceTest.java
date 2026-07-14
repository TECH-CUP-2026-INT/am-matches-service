package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.enums.MatchStatus;
import co.edu.escuelaing.techcup.match.exception.InvalidMatchStateException;
import co.edu.escuelaing.techcup.match.exception.InvalidTeamException;
import co.edu.escuelaing.techcup.match.exception.MatchAccessDeniedException;
import co.edu.escuelaing.techcup.match.exception.MatchNotFoundException;
import co.edu.escuelaing.techcup.match.repository.MatchRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchAccessServiceTest {

    @Mock
    private MatchRepository matchRepository;

    private MatchAccessService service;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private final UUID homeTeamId = UUID.randomUUID();
    private final UUID awayTeamId = UUID.randomUUID();
    private Match match;

    @BeforeEach
    void setUp() {
        service = new MatchAccessService(matchRepository);
        match = new Match();
        match.setId(matchId);
        match.setRefereeId(refereeId);
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setStatus(MatchStatus.IN_PROGRESS);
    }

    @Test
    void requireOwnedMatch_matchNotFound_throws() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());
        assertThrows(MatchNotFoundException.class, () -> service.requireOwnedMatch(matchId, refereeId));
    }

    @Test
    void requireOwnedMatch_otherReferee_throwsAccessDenied() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        UUID otherReferee = UUID.randomUUID();
        assertThrows(MatchAccessDeniedException.class, () -> service.requireOwnedMatch(matchId, otherReferee));
    }

    @Test
    void requireOwnedMatch_ownedByReferee_returnsMatch() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        Match result = service.requireOwnedMatch(matchId, refereeId);
        assertThat(result).isSameAs(match);
    }

    @Test
    void requireActiveMatch_scheduledStatus_throwsInvalidMatchState() {
        match.setStatus(MatchStatus.SCHEDULED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        assertThrows(InvalidMatchStateException.class, () -> service.requireActiveMatch(matchId, refereeId));
    }

    @Test
    void requireActiveMatch_finishedStatus_throwsInvalidMatchState() {
        match.setStatus(MatchStatus.FINISHED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        assertThrows(InvalidMatchStateException.class, () -> service.requireActiveMatch(matchId, refereeId));
    }

    @Test
    void requireActiveMatch_pausedStatus_isAllowed() {
        match.setStatus(MatchStatus.PAUSED);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        Match result = service.requireActiveMatch(matchId, refereeId);
        assertThat(result).isSameAs(match);
    }

    @Test
    void requireActiveMatch_inProgress_isAllowed() {
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(match));
        Match result = service.requireActiveMatch(matchId, refereeId);
        assertThat(result).isSameAs(match);
    }

    @Test
    void validateTeamBelongsToMatch_homeTeam_ok() {
        service.validateTeamBelongsToMatch(match, homeTeamId);
    }

    @Test
    void validateTeamBelongsToMatch_awayTeam_ok() {
        service.validateTeamBelongsToMatch(match, awayTeamId);
    }

    @Test
    void validateTeamBelongsToMatch_unknownTeam_throws() {
        UUID unknownTeam = UUID.randomUUID();
        assertThrows(InvalidTeamException.class, () -> service.validateTeamBelongsToMatch(match, unknownTeam));
    }
}
