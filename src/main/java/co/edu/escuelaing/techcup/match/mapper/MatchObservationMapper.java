package co.edu.escuelaing.techcup.match.mapper;

import co.edu.escuelaing.techcup.match.dto.response.MatchObservationResponse;
import co.edu.escuelaing.techcup.match.entity.MatchObservation;
import co.edu.escuelaing.techcup.match.entity.enums.EventType;

public final class MatchObservationMapper {

    private MatchObservationMapper() {
    }

    public static MatchObservationResponse toResponse(MatchObservation observation) {
        return new MatchObservationResponse(
                observation.getId(),
                observation.getMatchId(),
                observation.getRefereeId(),
                observation.getText(),
                observation.getMinute(),
                EventType.OBSERVATION,
                observation.getCreatedAt()
        );
    }
}
