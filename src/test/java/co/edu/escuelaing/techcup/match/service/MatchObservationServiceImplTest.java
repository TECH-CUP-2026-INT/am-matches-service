package co.edu.escuelaing.techcup.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.request.RegisterObservationRequest;
import co.edu.escuelaing.techcup.match.dto.response.MatchObservationResponse;
import co.edu.escuelaing.techcup.match.entity.Match;
import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import co.edu.escuelaing.techcup.match.repository.MatchObservationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchObservationServiceImplTest {

    @Mock
    private MatchObservationRepository observationRepository;
    @Mock
    private MatchAccessService matchAccessService;

    private MatchObservationServiceImpl observationService;

    private final UUID matchId = UUID.randomUUID();
    private final UUID refereeId = UUID.randomUUID();
    private Match match;

    @BeforeEach
    void setUp() {
        observationService = new MatchObservationServiceImpl(observationRepository, matchAccessService);

        match = new Match();
        match.setId(matchId);
    }

    @Test
    void registerObservation_persistsWithRefereeAndText() {
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        when(observationRepository.save(any(MatchObservation.class))).thenAnswer(invocation -> {
            MatchObservation observation = invocation.getArgument(0);
            observation.setId(UUID.randomUUID());
            observation.setCreatedAt(Instant.now());
            return observation;
        });

        MatchObservationResponse response = observationService.registerObservation(
                matchId, refereeId, new RegisterObservationRequest("Cancha en buen estado", 12));

        assertThat(response.text()).isEqualTo("Cancha en buen estado");
        assertThat(response.refereeId()).isEqualTo(refereeId);
        assertThat(response.minute()).isEqualTo(12);
    }

    @Test
    void listObservations_returnsRepositoryResults() {
        when(matchAccessService.requireOwnedMatch(matchId, refereeId)).thenReturn(match);
        MatchObservation observation = new MatchObservation();
        observation.setId(UUID.randomUUID());
        observation.setMatchId(match.getId());
        observation.setRefereeId(refereeId);
        observation.setText("Lluvia leve en el segundo tiempo");
        observation.setCreatedAt(Instant.now());
        when(observationRepository.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of(observation));

        List<MatchObservationResponse> responses = observationService.listObservations(matchId, refereeId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).text()).isEqualTo("Lluvia leve en el segundo tiempo");
    }
}
